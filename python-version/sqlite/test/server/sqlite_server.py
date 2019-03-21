import json
import os
import sys
import subprocess  
import shutil
from uuid   import uuid4
from flask  import Flask, request, jsonify, send_file
import sqlite3
import zlib
import geojson
from shapely import geometry, wkt, wkb

def set_scalar_features(row,headers):
    scalar_features = []
    nvarray = []
    scalar_values = {}
    for i in range(7,len(row)-1):
       nv = { "name" : headers[i], "value" : float(row[i]) }
       nvarray.append(nv)
    scalar_values["ns"] = "http://u24.bmi.stonybrook.edu/v1"
    scalar_values["nv"] = nvarray
    scalar_features.append(scalar_values)

    return scalar_features

def set_provenance_metadata():
    image = {}
    image["case_id"] = "case_id"
    image["subject_id"] = "subject_id"
    analysis = {}
    analysis["execution_id"] = "analysis_id"
    analysis["study_id"] = ""
    analysis["source"] = "computer"
    analysis["computation"] = "segmentation"
    provenance = {}
    provenance["image"] = image
    provenance["analysis"] = analysis
    provenance["data_loader"] = "1.4"
    provenance["batch_id"] = 0 
    provenance["tag_id"]   = 1 
    return provenance

def set_document_metadata(gj_poly,rand,x,y):
    gj_poly["parent_id"] = "self"
    gj_poly["normalized"] = "true"
    gj_poly["x"] = x 
    gj_poly["y"] = y 
    gj_poly["object_type"] = "nucleus"
    gj_poly["randval"] = rand 
    gj_poly["provenance"] = set_provenance_metadata()

def set_geojson_doc(row,headers,h_len):
    p_poly = wkb.loads(zlib.decompress(row[h_len-1]))
    # p_poly = wkt.loads(zlib.decompress(row[h_len-1]).decode("utf-8"))
    polyarray = list(p_poly.exterior.coords);
    polyjson  = geojson.Polygon([polyarray]);
    scfeatures = {}
    scfeatures["scalar_features"] = set_scalar_features(row,headers)
    gj_poly = geojson.Feature(geometry=polyjson,properties=scfeatures)
    gj_poly["footprint"] = float(row[7]); 
    set_document_metadata(gj_poly,row[6],row[0],row[1]);
    return gj_poly

# set up the application
app = Flask(__name__)

@app.route('/query/<subjectid>/<caseid>/<analysisid>/<qtype>',methods=['GET'])
def get_list(subjectid,caseid,analysisid,qtype):
    a_val = request.args.get('area');
    x_val = request.args.get('x');
    y_val = request.args.get('y');
    r_val = request.args.get('rand');
    l_val = request.args.get('limit');
    s_val = request.args.get('skip');

    conn = sqlite3.connect("example.db");
    c = conn.cursor();
    sql  = "select analysis_table from metadata where ";
    sql  = sql + " case_id = '" + caseid + "'";
    sql  = sql + " and subject_id = '" + subjectid + "'";
    sql  = sql + " and analysis_id = '" + analysisid + "'";

    c.execute(sql);
    analysis_table = "";
    for row in c:
        analysis_table = row[0];

    sql = "";
    if qtype=="select": 
       sql = "select * from " + analysis_table + " where ";
    elif qtype=="count":
       sql = "select count(*) from " + analysis_table + " where ";
   
    sql = sql + "(" 
    if a_val!=None:
       v_val = a_val.split(',');
       sql = sql + "area > " + str(v_val[0]);
       if (len(v_val)>1):
          sql = sql + " and area < " + str(v_val[1]);
    else:
       sql = sql + "area > 0";

    if x_val!=None:
       xx_val = x_val.split(',');
       sql = sql + " and x > " + str(xx_val[0]);
       if (len(xx_val)>1):
          sql = sql + " and x < " + str(xx_val[1]);
    else:
       sql = sql + " and x > " + str(0.0);

    if y_val!=None:
       yy_val = y_val.split(',');
       sql = sql + " and y > " + str(yy_val[0]);
       if (len(yy_val)>1):
          sql = sql + " and y < " + str(yy_val[1]);
    else:
       sql = sql + " and y > " + str(0.0);

    if r_val!=None:
       sql = sql + ") order by random() ";
       # rr_val = r_val.split(',');
       # sql = sql + " and rand > " + str(rr_val[0]);
       # if (len(rr_val)>1):
       #   sql = sql + " and rand < " + str(rr_val[1]);
    else:
       sql = sql + ")";
 
    if qtype=="select":
       if s_val!=None:
          sql = sql + " limit " + str(l_val) + " offset " + str(s_val);
       else:
          sql = sql + " limit " + str(l_val);
    print(sql);
    c = conn.cursor();
    c.execute(sql);
    headers = [description[0] for description in c.description]
    h_len = len(c.description);
    results = [];
    if qtype=="select":
       rcnt = 0;
       for row in c:
           j_val = set_geojson_doc(row,headers,h_len);
           results.append(j_val);
           rcnt = rcnt + 1;
       print("Number of results: ",rcnt);
    elif qtype=="count":
       for row in c:
           j_val = {"count": row[0] };
           results.append(j_val);
    c.close();
    conn.close();

    return jsonify(results)

