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

def set_document_metadata(gj_poly,rand,x,y):
    gj_poly["parent_id"] = "self"
    gj_poly["normalized"] = "true"
    gj_poly["x"] = x 
    gj_poly["y"] = y 
    gj_poly["object_type"] = "nucleus"
    gj_poly["randval"] = rand 

def get_geojson_doc(row,headers,h_len):
    p_poly = wkb.loads(zlib.decompress(row[h_len-1]))
    polyarray = list(p_poly.exterior.coords);
    polyjson  = geojson.Polygon([polyarray]);
    scfeatures = {}
    scfeatures["scalar_features"] = set_scalar_features(row,headers)
    gj_poly = geojson.Feature(geometry=polyjson,properties=scfeatures)
    gj_poly["footprint"] = float(row[7]); 
    set_document_metadata(gj_poly,row[6],row[0],row[1]);
    return gj_poly

def get_provenance_metadata(case_id,subject_id,analysis_id):
    image = {}
    image["case_id"] = case_id 
    image["subject_id"] = subject_id 
    analysis = {}
    analysis["execution_id"] = analysis_id 
    analysis["study_id"] = "default"
    analysis["source"] = "computer"
    analysis["computation"] = "segmentation"
    provenance = {}
    provenance["image"] = image
    provenance["analysis"] = analysis
    return provenance

def compose_result_set(c,qtype,caseid,subjectid,analysisid):
    headers = [description[0] for description in c.description]
    h_len = len(c.description);
    results = [];
    if qtype=="select":
       rcnt = 0;
       sql_result = {};
       sql_result["provenance"] = get_provenance_metadata(caseid,subjectid,analysisid);
       for row in c:
           j_val = get_geojson_doc(row,headers,h_len);
           results.append(j_val);
           rcnt = rcnt + 1;
       sql_result["result_cnt"] = int(rcnt);
       sql_result["result_set"] = results;
       print("Number of results: ",rcnt);
    elif qtype=="count":
       sql_result = {};
       sql_result["provenance"] = get_provenance_metadata(caseid,subjectid,analysisid);
       for row in c:
           sql_result["result_cnt"] = int(row[0]);
       sql_result["result_set"] = [];
    return sql_result; 

# set up the application
app = Flask(__name__)

@app.route('/sql/<subjectid>/<caseid>/<analysisid>/<qtype>',methods=['GET'])
def get_sql_list(subjectid,caseid,analysisid,qtype):
    a_flt = request.args.get('filter');
    r_val = request.args.get('rand');
    l_val = request.args.get('limit');
    s_val = request.args.get('skip');

    print(a_flt)

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
   
    sql = sql + "(" + a_flt;
    if r_val!=None and qtype=="select":
       sql = sql + ") order by random() ";
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
    result_set = compose_result_set(c,qtype,caseid,subjectid,analysisid);
    c.close();
    conn.close();

    return jsonify(result_set)

@app.route('/query/<subjectid>/<caseid>/<analysisid>/<qtype>',methods=['GET'])
def get_query_list(subjectid,caseid,analysisid,qtype):
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
    result_set = compose_result_set(c,qtype,caseid,subjectid,analysisid);
    c.close();
    conn.close();

    return jsonify(result_set)

