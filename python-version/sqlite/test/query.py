import sqlite3
import zlib
import json
import sys
import geojson
from shapely import geometry, wkt, wkb

def set_scalar_features(row,headers):
    scalar_features = []
    nvarray = []
    scalar_values = {}
    for i in range(len(row)-1):
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


conn = sqlite3.connect("example.db");

cnt = 0;
c = conn.cursor();

c.execute("select analysis_table from metadata");
analysis_table = "";
for row in c:
    analysis_table = row[0];

sql = "select * from " + analysis_table + " where rand > 0.6 limit 5000";

c.execute(sql);

headers = [description[0] for description in c.description]
h_len = len(c.description);

for row in c:
    cnt = cnt + 1
    p_poly = wkb.loads(zlib.decompress(row[h_len-1]))
    polyarray = list(p_poly.exterior.coords);
    polyjson  = geojson.Polygon([polyarray]);
    scfeatures = {}
    scfeatures["scalar_features"] = set_scalar_features(row,headers)
    gj_poly = geojson.Feature(geometry=polyjson,properties=scfeatures)
    gj_poly["footprint"] = float(row[0]); 
    set_document_metadata(gj_poly,row[1],row[2],row[3]);

print(cnt)
