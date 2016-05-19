#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

dbName=$1
tempDir=$2
inpFile=$3
inpType=$4

cd $tempDir
unzip $inpFile
run_featuredb.sh load --dbname $dbName --inptype $inpType --inplist file_list.csv "$@" 
rm -rf $tempDir
