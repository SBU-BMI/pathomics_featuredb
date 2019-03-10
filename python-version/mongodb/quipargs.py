import argparse

parser = argparse.ArgumentParser(description="QuIP results loader.")
parser.add_argument("--dbhost",default="localhost",metavar="<hostname>",type=str,help="FeatureDB host name. Default: localhost")
parser.add_argument("--dbport",default=27017,type=int,metavar="<port>",help="FeatureDB host port. Default: 27017")
parser.add_argument("--dbname",required=True,metavar="<name>",type=str,help="FeatureDB database name.")
parser.add_argument("--quip",required=True,type=str,metavar="<folder>",help="QuIP results folder name.")

args = {}
