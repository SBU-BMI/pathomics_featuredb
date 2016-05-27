#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

dbHost="localhost"
dbPort="27017"
dbName=$1
	
mongo $dbHost:$dbPort/$dbName $FEATUREDB_DIR/script/indexes/db_index.js
