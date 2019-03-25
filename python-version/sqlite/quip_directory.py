import sys
import glob
import sqlite3
import argparse

parser = argparse.ArgumentParser(description="QuIP sqlite3 database directory.")
parser.add_argument("--dbname",required=True,type=str,metavar="<database name>",help="Name of the sqlite3 directory.")
parser.add_argument("--dbroot",required=True,type=str,metavar="<database name>",help="Root folder.")

def get_file_list(folder):
    metafiles = []
    fnames = folder + "/**/*.qdb"
    i = 1
    for name in glob.glob(fnames,recursive=True):
        metafiles.append(name)
        i = i + 1
    return metafiles

def create_directory(dbfiles,dbout):
    conn = sqlite3.connect(dbout);
    c = conn.cursor();
    sql = "CREATE TABLE IF NOT EXISTS quip_directory ";
    sql = sql + "(case_id text, subject_id text, analysis_id text, op_type text, result_type text, ";
    sql = sql + "analysis_table text, width int, height int, mpp float, db_file text)"
    c.execute(sql);
    conn.commit();
    c.close();

    sql_i = "INSERT INTO quip_directory(case_id,subject_id,analysis_id,op_type,result_type, " 
    sql_i = sql_i + "analysis_table,width,height,mpp,db_file) VALUES(?,?,?,?,?,?,?,?,?,?)";
    for dbfile in dbfiles:
       conn_i = sqlite3.connect(dbfile);
       c_i = conn_i.cursor();
       c_i.execute("SELECT * from metadata;");
       for row in c_i: 
           tdata = ();
           for col in row:
               tdata = tdata + (col,);
           tdata = tdata + (dbfile,);
       c = conn.cursor();
       c.execute(sql_i,tdata);
       c.close();
       conn.commit();
       conn_i.close();

    create_index("quip_directory",conn);
    conn.close();

def create_index(table_name,conn):
    c = conn.cursor();
    sql = "CREATE INDEX sca_"+str(table_name)+" ON "+str(table_name)+" (subject_id,case_id,analysis_id)";
    c.execute(sql);
    conn.commit();
    c.close();

if __name__ == "__main__":
   quip_args = {};
   quip_args = vars(parser.parse_args());
   dbroot = quip_args["dbroot"];
   dbname = quip_args["dbname"];

   dbout = dbroot + "/" + dbname;
   dbfiles = get_file_list(dbroot);
   create_directory(dbfiles,dbout);

   sys.exit(0);

