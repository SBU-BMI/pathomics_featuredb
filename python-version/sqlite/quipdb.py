from pymongo import MongoClient
from pymongo.errors import ConnectionFailure
from pymongo.errors import ConfigurationError
import random
import datetime

def connect(dbhost,dbport):
    dburi = "mongodb://"+dbhost+":"+str(dbport)+"/"
    client = MongoClient(dburi)
    try:
       res = client.admin.command('ismaster')
    except ConnectionFailure:
       print("Server is not available.")
       return None
    return client

def getdb(client,dbname):
    db = client[dbname]
    return db

def check_metadata(db,mdata):
    case_id = mdata["case_id"]
    subject_id = mdata["subject_id"]
    exec_id = mdata["analysis_id"]
    query = {}
    query["image.case_id"] = case_id 
    query["image.subject_id"] = subject_id 
    query["provenance.analysis_execution_id"] =  exec_id 
    res = db.metadata.find_one(query)
    return res

def submit_metadata(db,mdata):
    mdoc = {"color" : "yellow"}
    mdoc["title"] = mdata["analysis_desc"]
    imgdoc = {"case_id" : mdata["case_id"], "subject_id" : mdata["subject_id"] }
    mdoc["image"] = imgdoc
    provdoc = {"study_id" : ""}
    provdoc["analysis_execution_id"] = mdata["analysis_id"]
    provdoc["type"] = "computer"
    provdoc["algorithm_params"] = mdata
    provdoc["randval"] = random.random()
    provdoc["submit_date"] = datetime.datetime.utcnow()
    mdoc["provenance"] = provdoc
    res = db.metadata.insert_one(mdoc)

def submit_results(db,results):
    res = db.objects.insert_many(results)

