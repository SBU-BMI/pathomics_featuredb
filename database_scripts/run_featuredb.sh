#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

if [ -z $OPENCV_JAVA_DIR ]; then
	echo "OPENCV_JAVA_DIR is not set."
	exit 1;
fi

dbCmd=""
dbHost="localhost"
dbPort="27017"
dbDockerName=""
imgMetaFile=""
dbName=""

if [ "$#" -lt 1 ]; then
	echo "Usage: run_featuredb.sh <command> [options]"
	echo "Commands: "
	echo "    create   - create featuredb"
	echo "				parameters: --dbname <database name> [--dbhost <mongodb host> --dbport <mongodb port>]"
	echo "    imgmeta  - load image metadata"
	echo "				parameters: --dbname <database name> --imgmeta <image metadata file> [ --dbhost <mongodb host> --dbport <mongodb port>]"
	echo "	  load     - load analysis results"
	echo "				parameters: --dbname <database name> [--dbhost <mongodb host> --dbport <mongodb host>] <additional parameters>"
	exit 1;
fi

dbCmd=$1
shift

if [ "$dbCmd" = "create" ]; then
	while [[ $# > 1 ]]
	do
		key="$1"

		case $key in
			--dbhost)
				dbHost="$2"
				shift # past argument
			;;
			--dbport)
				dbPort="$2"
				shift # past argument
			;;
			--dbname)
				dbName="$2"
				shift # past argument
			;;
			--default)
				DEFAULT=YES
			;;
			*)
				echo "Unknown option";
				exit 1;	# unknown option
			;;
		esac
		shift # past argument or value
	done
	if [ "$dbName" = "" ]; then
		echo "Missing database name";
    	exit 1;
	fi
	mongo $dbHost:$dbPort/$dbName $FEATUREDB_DIR/database_scripts/indexes/db_index.js
	exit 0;
fi

if [ "$dbCmd" = "imgmeta" ]; then
	while [[ $# > 1 ]]
	do
		key="$1"

		case $key in
			--dbhost)
				dbHost="$2"
				shift # past argument
			;;
			--dbport)
				dbPort="$2"
				shift # past argument
			;;
			--dbname)
				dbName="$2"
				shift # past argument
			;;
			--imgmeta)
				imgMetaFile="$2"
				shift # past argument
			;;
			--default)
				DEFAULT=YES
			;;
			*)
				echo "Unknown option";
				exit 1;	# unknown option
			;;
		esac
		shift # past argument or value
	done
	if [ "$dbName" = "" ]; then
		echo "Missing database name parameter.";
    	exit 1;
	fi
	if [ "$imgMetaFile" = "" ]; then
		echo "Missing image metadata file parameter.";
		exit 1;
	fi
	mongoimport --host $dbHost --port $dbPort -d $dbName -c images --type=csv --headerline $imgMetaFile
	exit 0;
fi

if [ "$dbCmd" = "load" ]; then
	java -Djava.library.path=$OPENCV_JAVA_DIR -jar $FEATUREDB_DIR/src/build/libs/mongodb-loader-1.3.jar "$@"
	exit 0;
fi
