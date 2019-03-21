import argparse

parser = argparse.ArgumentParser(description="QuIP results loader.")
parser.add_argument("--nprocs",required=True,type=int,metavar="<num of processes>",help="Number of concurrent processes to create db.")
parser.add_argument("--dbname",required=True,type=str,metavar="<database name>",help="The name of the sqlite3 database.")
parser.add_argument("--quip",required=True,type=str,metavar="<folder>",help="QuIP results folder name.")

args = {}
