#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

dbName=$1
tempDir=$2
imgMetaFile=$3

cd $tempDir
mongoimport -d $dbName -c images --type=csv --headerline $imgMetaFile
rm -rf $tempDir
