import csv
import json
import geojson
import random
import sys
from   multiprocessing import Pool, Lock
import os
import glob
import quipargs
import sqlite3
import zlib
import uuid
import time
from shapely import geometry, wkb, wkt

conn = None;
lock = Lock();

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

def get_polygon(polydata,imw,imh):
    p_len   = len(polydata);
    p_first = polydata[0].split("[")[1];
    p_last  = polydata[p_len-1].split("]")[0];
    polydata[0] = p_first;
    polydata[p_len-1] = p_last;

    s_poly = geometry.Polygon([[float(polydata[i]),float(polydata[i+1])] for i in range(0,p_len,2)]);
    s_poly = s_poly.simplify(1.0,preserve_topology=True);
    s_area = s_poly.area;

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

def set_provenance_metadata(mdata,batch_id,tag_id):
    image = {}
    image["case_id"] = mdata["case_id"]
    image["subject_id"] = mdata["subject_id"]
    analysis = {}
    analysis["execution_id"] = mdata["analysis_id"]
    analysis["study_id"] = ""
    analysis["source"] = "computer"
    analysis["computation"] = "segmentation"
    provenance = {}
    provenance["image"] = image
    provenance["analysis"] = analysis
    provenance["data_loader"] = "1.4"
    provenance["batch_id"] = batch_id 
    provenance["tag_id"]   = tag_id 
    return provenance

def set_document_metadata(gj_poly,bbox,mdata,batch_id,tag_id):
    gj_poly["parent_id"] = "self"
    gj_poly["normalized"] = "true"
    gj_poly["bbox"] = bbox
    gj_poly["x"] = (float(bbox[0])+float(bbox[2]))/2
    gj_poly["y"] = (float(bbox[1])+float(bbox[3]))/2
    gj_poly["object_type"] = "nucleus"
    gj_poly["randval"] = random.random()
    gj_poly["provenance"] = set_provenance_metadata(mdata,batch_id,tag_id)

def process_quip(mfile):
    mdata = read_metadata(mfile[1])
    process_file(mdata,mfile[0],mfile[2],mfile[3])

def getting_spec_for_insertion(headers,polycol,analysis_table):
    sql1 = "INSERT INTO "+analysis_table+" (x,y,minx,miny,maxx,maxy,rand,area"; 
    sql2 = " VALUES(?,?,?,?,?,?,?,?"; 
    farray = [];
    for i in range(1,polycol-1):
        if headers[i] not in farray:
           sql1 = sql1 + "," + headers[i];
           sql2 = sql2 + ",?";
           farray.append(headers[i]);
    if headers[polycol-1] not in farray:
       sql1 = sql1 + "," + headers[polycol-1];
       sql2 = sql2 + ",?";
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

    sql = getting_spec_for_insertion(headers,polycol,analysis_table);

    cnt = 0
    tdata_array = [];
    for row in csvreader:
       polyarray,p_area,bbox = get_polygon(row[polycol].split(":"),image_width,image_height)
       x = (float(bbox[0])+float(bbox[2]))/2
       y = (float(bbox[1])+float(bbox[3]))/2
       varray = set_scalar_features(row,headers,polycol)
       cnt = cnt + 1
       gj_poly_zip = zlib.compress(polyarray,9); 
       # gj_poly_zip = zlib.compress(str(polyarray).encode("utf-8"),9); 
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

def store_metadata(mfiles,analysis_table):
    mdata = read_metadata(mfiles[0][1]);
    c = conn.cursor();
    sql = "INSERT INTO metadata(case_id,subject_id,analysis_id,analysis_table,width,height) VALUES(?,?,?,?,?,?)";
    tdata = (mdata["case_id"],mdata["subject_id"],mdata["analysis_id"],analysis_table,mdata["image_width"],mdata["image_height"]);
    c.execute(sql,tdata);
    conn.commit();
    c.close();

def get_table_spec(mfiles,conn):
    mdata = read_metadata(mfiles[0][1]);

    # check if table exists
    sql = "SELECT analysis_table from metadata where ";
    sql = sql + "case_id = '" + str(mdata["case_id"]) + "'";
    sql = sql + " and ";
    sql = sql + "subject_id = '" + str(mdata["subject_id"]) + "'";
    sql = sql + " and ";
    sql = sql + "analysis_id = '" + str(mdata["analysis_id"]) + "'";
    c = conn.cursor();
    c.execute(sql);
    analysis_table = ""
    for row in c:
        analysis_table = row[0];
    if analysis_table!="":
        return analysis_table,True,"";

    analysis_table = "objects_"+str(uuid.uuid1().hex);

    fname = mfiles[0][0]+"/"+mdata["out_file_prefix"]+"-features.csv";
    csvfile   = open(fname);
    csvreader = csv.reader(csvfile);
    headers   = next(csvreader);
    polycol   = headers.index("Polygon");
    sql = "CREATE TABLE " + analysis_table + " (x float, y float, minx float, miny float, maxx float, maxy float, rand float, area int"; 
    farray = [];
    for i in range(1,polycol-1):
        if headers[i] not in farray:
           sql = sql + "," + headers[i] + " float";
           farray.append(headers[i]);
    if headers[polycol-1] not in farray:
       sql = sql + "," + headers[polycol-1] + " float";
    sql = sql + ", json_doc blob)";
    csvfile.close();
    print(sql);
    return analysis_table,False,sql;

def create_tables(mfiles,conn):
    c = conn.cursor();
    c.execute("CREATE TABLE IF NOT EXISTS metadata (case_id text, subject_id text, analysis_id text, analysis_table text, width int, height int)");
    conn.commit();
    table_name,table_exists,sql = get_table_spec(mfiles,conn);
    if table_exists==False:
       c.execute(sql);
       # sql = "CREATE INDEX xyar_"+str(table_name)+" ON "+str(table_name)+" (area,x,y,rand)";
       # c.execute(sql);
       conn.commit();
    conn.commit();
    c.close();
    return table_name,table_exists;

def create_index(table_name,conn):
    c = conn.cursor();
    sql = "CREATE INDEX xyar_"+str(table_name)+" ON "+str(table_name)+" (area,x,y,rand)";
    c.execute(sql);
    conn.commit();
 
if __name__ == "__main__":
   quipargs.args = vars(quipargs.parser.parse_args())
   random.seed(a=None)
   csv.field_size_limit(sys.maxsize)

   qfolder = quipargs.args["quip"];
   dbname  = quipargs.args["dbname"];
   nprocs  = int(quipargs.args["nprocs"]);

   mfiles = get_file_list(qfolder) 

   conn = sqlite3.connect(dbname);
   analysis_table,table_exists = create_tables(mfiles,conn);
   if table_exists==False:
      store_metadata(mfiles,analysis_table);
   meta_files = [];
   for i in range(len(mfiles)):
       meta_files.append((mfiles[i][0],mfiles[i][1],mfiles[i][2],analysis_table));
   p = Pool(processes=nprocs)
   p.map(process_quip,meta_files,1)
   create_index(analysis_table,conn);

