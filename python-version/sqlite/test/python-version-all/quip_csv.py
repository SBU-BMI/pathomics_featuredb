import csv
import json
import random
import sys
from   multiprocessing import Pool, Lock
import glob
import sqlite3
import zlib
import uuid
from shapely import geometry, wkb, wkt
import argparse

# Command line arguments
parser = argparse.ArgumentParser(description="QuIP results loader.")
parser.add_argument("--nprocs",required=True,type=int,metavar="<num of processes>",help="Number of concurrent processes to create db.")
parser.add_argument("--dbname",required=True,type=str,metavar="<database name>",help="The name of the sqlite3 database.")
parser.add_argument("--quip",required=True,type=str,metavar="<folder>",help="QuIP results folder name.")

# Input files
def get_file_list(folder):
    metafiles = []
    fnames = folder + "/*-algmeta.json"
    i = 1
    for name in glob.glob(fnames):
        metafiles.append((folder,name,i,""))
        i = i + 1
    return metafiles

def read_metadata(meta_file):
    mf = open(meta_file)
    data = json.load(mf)
    return data

# Database setup
conn = None;

def store_metadata(mfile,mdata,analysis_table):
    c = conn.cursor();
    sql = "INSERT INTO metadata(case_id,subject_id,analysis_id,analysis_table,width,height) VALUES(?,?,?,?,?,?)";
    tdata = (mdata["case_id"],mdata["subject_id"],mdata["analysis_id"],analysis_table,mdata["image_width"],mdata["image_height"]);
    c.execute(sql,tdata);
    conn.commit();
    c.close();

def create_analysis_table(mfile,mdata,conn):
    # check if analysis table exists
    sql = "SELECT analysis_table from metadata where ";
    sql = sql + "case_id = '" + str(mdata["case_id"]) + "'";
    sql = sql + " and ";
    sql = sql + "subject_id = '" + str(mdata["subject_id"]) + "'";
    sql = sql + " and ";
    sql = sql + "analysis_id = '" + str(mdata["analysis_id"]) + "'";
    c = conn.cursor();
    c.execute(sql);
    row = c.fetchone();
    if row!=None:
       return row[0],True;

    analysis_table = "objects_"+str(uuid.uuid1().hex);

    fname = mfile[0]+"/"+mdata["out_file_prefix"]+"-features.csv";
    csvfile   = open(fname);
    csvreader = csv.reader(csvfile);
    headers   = next(csvreader);
    polycol   = headers.index("Polygon");
    sql = "CREATE TABLE " + analysis_table;
    sql = sql + " (x float, y float, minx float, miny float, maxx float, maxy float, rand float, area int"; 
    farray = [];
    for i in range(1,polycol):
        if headers[i] not in farray:
           sql = sql + "," + headers[i] + " float";
           farray.append(headers[i]);
    sql = sql + ", json_doc blob)";
    csvfile.close();
    c.execute(sql);
    conn.commit();
    c.close();
    return analysis_table,False;

def create_metadata_table(mfile,mdata,conn):
    c = conn.cursor();
    sql = "CREATE TABLE IF NOT EXISTS metadata ";
    sql = sql + "(case_id text, subject_id text, analysis_id text, ";
    sql = sql + "analysis_table text, width int, height int)"
    c.execute(sql);
    conn.commit();
    c.close();
 
def create_tables(mfile,mdata,conn):
    create_metadata_table(mfile,mdata,conn);
    table_name,table_exists = create_analysis_table(mfile,mdata,conn);
    return table_name,table_exists;

def create_index(table_name,conn):
    c = conn.cursor();
    sql = "CREATE INDEX axy_"+str(table_name)+" ON "+str(table_name)+" (area,x,y)";
    c.execute(sql);
    conn.commit();
    c.close();

# Process analysis results files
lock = Lock();

def get_polygon(polydata,imw,imh):
    p_len   = len(polydata);
    p_first = polydata[0].split("[")[1];
    p_last  = polydata[p_len-1].split("]")[0];
    polydata[0] = p_first;
    polydata[p_len-1] = p_last;

    s_poly = geometry.Polygon([[float(polydata[i]),float(polydata[i+1])] for i in range(0,p_len,2)]);
    s_poly = s_poly.simplify(1.0,preserve_topology=True);
    s_area = int(s_poly.area);

    p_poly = [];
    for p in list(s_poly.exterior.coords):
        x = p[0]/float(imw);
        y = p[1]/float(imh);
        p_poly.append((x,y));
    s_poly = geometry.Polygon(p_poly);
    return wkb.dumps(s_poly),s_area,s_poly.bounds;

def set_scalar_features(row,headers,polycol):
    farray = [];
    varray = [];
    for i in range(1,polycol):
        if headers[i] not in farray:
           varray.append(float(row[i]));
           farray.append(headers[i]);
    return varray

def get_insertion_sql(headers,polycol,analysis_table):
    sql1 = "INSERT INTO "+analysis_table+" (x,y,minx,miny,maxx,maxy,rand,area"; 
    sql2 = " VALUES(?,?,?,?,?,?,?,?"; 
    farray = [];
    for i in range(1,polycol):
        if headers[i] not in farray:
           sql1 = sql1 + "," + headers[i];
           sql2 = sql2 + ",?";
           farray.append(headers[i]);
    sql1 = sql1 + ",json_doc)";
    sql2 = sql2 + ",?)";
    return sql1 + sql2;

def process_file(mdata,fname,idx,analysis_table):
    image_width  = mdata["image_width"]
    image_height = mdata["image_height"]

    fname = fname+"/"+mdata["out_file_prefix"]+"-features.csv"

    csvfile   = open(fname)
    csvreader = csv.reader(csvfile)
    headers   = next(csvreader) 
    polycol   = headers.index("Polygon") 

    sql = get_insertion_sql(headers,polycol,analysis_table);

    cnt = 0
    tdata_array = [];
    for row in csvreader:
       polyarray,p_area,bbox = get_polygon(row[polycol].split(":"),image_width,image_height)
       x = (float(bbox[0])+float(bbox[2]))/2
       y = (float(bbox[1])+float(bbox[3]))/2
       varray = set_scalar_features(row,headers,polycol)
       cnt = cnt + 1
       gj_poly_zip = zlib.compress(polyarray,9); 
       tdata = (x,y,float(bbox[0]),float(bbox[1]),float(bbox[2]),float(bbox[3]),random.random(),p_area);
       for fv in varray:
           tdata = tdata + (fv,);
       tdata = tdata + (gj_poly_zip,);
       tdata_array.append(tdata);
       
    if (cnt>0):
       lock.acquire();
       print("IDX: ", idx, " File: ",fname,"  Count: ",cnt)
       c = conn.cursor();
       c.executemany(sql,tdata_array);
       conn.commit();
       c.close();
       lock.release();

def process_quip(mfile):
    mdata = read_metadata(mfile[1])
    process_file(mdata,mfile[0],mfile[2],mfile[3])
 
if __name__ == "__main__":
   random.seed(a=None);
   csv.field_size_limit(sys.maxsize);

   quip_args = {};
   quip_args = vars(parser.parse_args());
   qfolder = quip_args["quip"];
   dbname  = quip_args["dbname"];
   nprocs  = int(quip_args["nprocs"]);

   mfiles = get_file_list(qfolder);
   mdata  = read_metadata(mfiles[0][1]);

   conn = sqlite3.connect(dbname);
   analysis_table,table_exists = create_tables(mfiles[0],mdata,conn);
   if table_exists==False:
      store_metadata(mfiles[0],mdata,analysis_table);
   meta_files = [];
   for i in range(len(mfiles)):
       meta_files.append((mfiles[i][0],mfiles[i][1],mfiles[i][2],analysis_table));
   p = Pool(processes=nprocs)
   p.map(process_quip,meta_files,1)
   create_index(analysis_table,conn);

