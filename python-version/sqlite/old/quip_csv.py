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

conn = None;
lock = Lock();

def get_file_list(folder):
    metafiles = []
    fnames = folder + "/*-algmeta.json"
    i = 1
    for name in glob.glob(fnames):
        metafiles.append((folder,name,i))
        i = i + 1
    return metafiles

def read_metadata(meta_file):
    mf = open(meta_file)
    data = json.load(mf)
    return data

def poly_geojson(polydata,imw,imh):
    polyarray = []
    x1 = float(polydata[0].split("[")[1])/float(imw)
    y1 = float(polydata[1])/float(imh)
    minx = x1; miny = y1
    maxx = x1; maxy = y1
    polyarray.append((x1,y1))
    i = 2
    while i < len(polydata)-2:
        x = float(polydata[i])/float(imw)
        y = float(polydata[i+1])/float(imh)
        if minx>x: minx = x
        if miny>y: miny = y
        if maxx<x: maxx = x
        if maxy<y: maxy = y
        polyarray.append((x,y))
        i = i + 2
    x = float(polydata[i])/float(imw)
    y = float(polydata[i+1].split("]")[0])/float(imh)
    if minx>x: minx = x
    if miny>y: miny = y
    if maxx<x: maxx = x
    if maxy<y: maxy = y
    polyarray.append((x,y))
    polyarray.append((x1,y1))
    bbox = [minx,miny,maxx,maxy]
    return geojson.Polygon([polyarray]),bbox

def set_scalar_features(row,headers,polycol):
    scalar_features = []
    nvarray = []
    scalar_values = {}
    for i in range(polycol):
       nv = { "name" : headers[i], "value" : float(row[i]) }
       nvarray.append(nv)
    scalar_values["ns"] = "http://u24.bmi.stonybrook.edu/v1"
    scalar_values["nv"] = nvarray
    scalar_features.append(scalar_values)
    return scalar_features

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
    process_file(mdata,mfile[0],mfile[2])

def process_file(mdata,fname,idx):
    image_width  = mdata["image_width"]
    image_height = mdata["image_height"]

    fname = fname+"/"+mdata["out_file_prefix"]+"-features.csv"

    csvfile   = open(fname)
    csvreader = csv.reader(csvfile)
    headers   = next(csvreader) 
    polycol   = headers.index("Polygon") 

    cnt = 0
    multi_documents = []
    tdata_array = [];
    for row in csvreader:
       polydata = row[polycol]
       polyjson,bbox = poly_geojson(polydata.split(":"),image_width,image_height)
       scfeatures = {}
       scfeatures["scalar_features"] = set_scalar_features(row,headers,polycol)
       gj_poly = geojson.Feature(geometry=polyjson,properties=scfeatures)
       gj_poly["footprint"] = float(row[headers.index("AreaInPixels")])
       set_document_metadata(gj_poly,bbox,mdata,"b0","t0")
       multi_documents.append(gj_poly)
       cnt = cnt + 1
       gj_poly_zip = zlib.compress(json.dumps(gj_poly).encode("utf-8"),5); 
       # gj_poly_zip = zlib.compress(str(gj_poly).encode("utf-8"),5); 
       tdata = (float(gj_poly["footprint"]),gj_poly["randval"],float(gj_poly["x"]),float(gj_poly["y"]),gj_poly_zip);
       tdata_array.append(tdata);
       
    if (cnt>0):
       print("IDX: ", idx, " File: ",fname,"  Count: ",cnt)
       lock.acquire();
       c = conn.cursor();
       sql = "INSERT INTO objects(area,rand,x,y,json_doc) VALUES(?,?,?,?,?)";
       c.executemany(sql,tdata_array);
       conn.commit();
       c.close();
       lock.release();

def store_metadata(mfiles):
    mdata = read_metadata(mfiles[0][1]);
    c = conn.cursor();
    sql = "INSERT INTO metadata(case_id,subject_id,analysis_id,width,height) VALUES(?,?,?,?,?)";
    tdata = (mdata["case_id"],mdata["subject_id"],mdata["analysis_id"],mdata["image_width"],mdata["image_height"]);
    c.execute(sql,tdata);
    conn.commit();
    c.close();

def create_tables(mfiles,conn):
    c = conn.cursor();
    c.execute("CREATE TABLE objects (area int, rand float, x float, y float, json_doc blob)");
    c.execute("CREATE TABLE metadata (case_id text, subject_id text, analysis_id text, width int, height int)");
    c.execute("CREATE INDEX area_x_y_rand ON objects (area,x,y,rand)");
    conn.commit();
    c.close();
 
if __name__ == "__main__":
   quipargs.args = vars(quipargs.parser.parse_args())
   random.seed(a=None)
   csv.field_size_limit(sys.maxsize)

   qfolder = quipargs.args["quip"];
   dbname  = quipargs.args["dbname"];
   nprocs  = int(quipargs.args["nprocs"]);

   mfiles = get_file_list(qfolder) 

   conn = sqlite3.connect(dbname);
   create_tables(mfiles,conn);
   store_metadata(mfiles);

   p = Pool(processes=nprocs)
   p.map(process_quip,mfiles,1)

