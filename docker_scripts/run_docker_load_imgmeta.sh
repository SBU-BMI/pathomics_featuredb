#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

dbName=$1
tempDir=$2
imgFile=$3
cancerType=$4
subjectId=$5
caseId=$6
imgMetaFile=$imgFile.csv

cd $tempDir
quip_image_metadata $imgFile $cancerType $subjectId $caseId $imgMetaFile
mongoimport -d $dbName -c images --type=csv --headerline $imgMetaFile
rm -rf $tempDir
