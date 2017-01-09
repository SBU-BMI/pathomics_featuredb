#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

if [ -z $OPENCV_JAVA_DIR ]; then
	echo "OPENCV_JAVA_DIR is not set."
	exit 1;
fi

if [ "$#" -lt 4 ]; then
	echo "Usage: run_featuredb_quip_zip.sh <dbHost> <dbPort> <dbName> <quipZipFile>"
	exit 1;
fi

dbHost=$1
shift
dbPort=$1
shift
dbName=$1
shift
quipZip=$1
shift

tmpFolder=/tmp/output$RANDOM
mkdir $tmpFolder
unzip $quipZip -d $tmpFolder 

$FEATUREDB_DIR/src/build/install/featuredb-loader/bin/featuredb-loader --dbhost $dbHost --dbport $dbPort --dbname $dbName --fromdb --quip $tmpFolder --inptype csv 

\rm -rf $tmpFolder
