#!/bin/bash 

dbCmd=""
dbDockerName=""
dbName=""
dbLocalPort=27017
dbQryPort=3000
dbDockerImage="sbubmi/pathomics_featuredb:1.0"
imgMetaFile=""
dbInpFile=""
dbZipFile=""
dbInpType=""

print_usage()
{
	echo "Usage: run_docker_featuredb.sh -h|<command> [options]"
	echo "Commands: "
	echo "	start - start Docker instance"
	echo "		arguments: <docker name> [--dbpath <host database folder> --qryport <host port for query server> --dbport <host port for mongodb> --image <docker image> --root <yes>]"
	echo "		default values: "
	echo "			host port for query server: " $dbQryPort
	echo "			host port for mongodb: " $dbLocalPort
	echo "			docker image: " $dbDockerImage
	echo "			user: run as root in docker. Default is local user id: " $UID "(" $USER")"
	echo " "
	echo "	remove - kill and remove Docker instance" 
	echo "		arguments: <docker name>"
	echo " "
	echo "	create - create featuredb"
	echo "		arguments: <docker name> <database name>"
	echo " "
	echo "	imgmeta - load image metadata"
	echo "		arguments: <docker name> <database name> <file> <--image <cancer_type> <subject id> <case id> | --metadata>"
	echo "			metadata - file is a QUIP image metadata CSV file containing metadata about multiple images."
	echo "			image - file is a whole tissue image. Need to provide cancer type, subject id, and case id. "
	echo " "
	echo "	loadfile - load a single file containing analysis results"
	echo "		arguments: <docker name> <database name> <input file> <csv|mask> <additional arguments>"
	echo "			required additional arguments: "
	echo "				--cid <case_id>      Case ID (Image ID)"
	echo "				--eid <exec_id>      Analysis execution id."
	echo "				--sid <subject_id>   Subject ID"
	echo "			optional additional arguments: "
	echo "				--etitle <title>               Analysis title for FeatureDB storage and visualization."
	echo "				--etype <type>                 Analysis type: human|computer."
	echo "				--fromdb                       Get image metadata from FeatureDB and normalize coordinates."
	echo "				--namespace <namespace>        Namespace for feature attribute names."
	echo "				--norm <w,h>                   Normalize polygon coordinates in mask image using"
	echo "				                               using width (w) and height (h)."
	echo "				--shift <x,y>                  Shift in X and Y dimensions for a mask image."
	echo "				--simplify                     Simplify polygons in CSV files using the JTS library."
	echo "				--sizefilter <minSize,maxSize> Filter polygons in CSV files based on polygon area."
	echo " "
	echo "	loadzip  - load multiple files stored in a zip file" 
	echo "		arguments: <docker name> <database name> <input zip file> <csv|mask> <additional arguments>"
	echo "			required additional arguments: "
	echo "				--eid <exec_id>      Analysis execution id."
	echo "			optional additional arguments: "
	echo "				--etitle <title>               Analysis title for FeatureDB storage and visualization."
	echo "				--etype <type>                 Analysis type: human|computer."
	echo "				--fromdb                       Get image metadata from FeatureDB and normalize coordinates."
	echo "				--namespace <namespace>        Namespace for feature attribute names."
	echo "				--simplify                     Simplify polygons in CSV files using the JTS library."
	echo "				--sizefilter <minSize,maxSize> Filter polygons in CSV files based on polygon area."
	echo "	loadquip  - load quip analysis file collection in a zip file" 
	echo "		arguments: <docker name> <database name> <input zip file> <csv|mask> <additional arguments>"
	echo "			optional additional arguments: "
	echo "				--namespace <namespace>        Namespace for feature attribute names."
	echo "				--simplify                     Simplify polygons in CSV files using the JTS library."
	echo "				--sizefilter <minSize,maxSize> Filter polygons in CSV files based on polygon area."
}

if [[ "$#" -lt 1 ]] || [[ "$1" = "-h" ]]; then
	print_usage;
	exit 1;
fi

dbCmd=$1
shift

# start command 
if [[ "$dbCmd" = "start" ]]; then
	dbDockerUser=$UID
	dbDockerName=$1
	shift
	if [[ "$dbDockerName" = "" ]]; then
		echo "A docker instance name is required."
		exit 1;
	fi
	while [[ $# > 0 ]]
	do
		key="$1"

		echo $key

		case $key in
			--dbpath)
				dbLocalFolder="$2"
				shift # past argument
			;;
			--qryport)
				dbQryPort="$2"
				shift # past argument
			;;
			--dbport)
				dbLocalPort="$2"
				shift # past argument
			;;
			--image)
				dbDockerImage="$2"
				shift # past argument
			;;
			--root)
				dbDockerUser="root"
				shift # past argument
			;;
			*)
				echo "Unknown option";
				exit 1;	# unknown option
			;;
		esac
		shift
	done
	echo  $dbDockerUser
	echo "$dbDockerImage"
	echo "$dbLocalFolder"
	if [[ "$dbLocalFolder" = "" ]]; then
		docker run --name $dbDockerName --user $dbDockerUser:$dbDockerUser -it -p $dbLocalPort:27017 -p $dbQryPort:3000 -d "$dbDockerImage" run_docker_mongodb_query.sh 
	else
		docker run --name $dbDockerName --user $dbDockerUser:$dbDockerUser -it -v $dbLocalFolder:/data/db -p $dbLocalPort:27017 -p $dbQryPort:3000 -d "$dbDockerImage" run_docker_mongodb_query.sh 
	fi
	if [[ $? == 0 ]]; then 
		echo "Use the following docker instance name in future interactions with featuredb docker: " $dbDockerName
		exit 0;
	else 
		exit 1;
	fi
fi

# remove command
if [[ "$dbCmd" = "remove" ]]; then
	dbDockerName=$1
	if [[ "$dbDockerName" = "" ]]; then
		echo "A docker instance name is required."
		exit 1;
	fi
	docker kill $dbDockerName
	docker rm   $dbDockerName
	exit 0;
fi

# create command
if [[ "$dbCmd" = "create" ]]; then
	dbDockerName=$1
	dbName=$2
	if [[ "$dbDockerName" = "" ]] || [[ "$dbName" = "" ]]; then
		echo "Docker instance and database names are required."
		exit 1;
	fi
	docker exec $dbDockerName run_docker_create_featuredb.sh $dbName
	exit 0;
fi

# image metadata load
if [[ "$dbCmd" = "imgmeta" ]]; then
	dbDockerName=$1
	dbName=$2
	inpFile=$3
	fileType=$4
	if [[ "$dbDockerName" = "" ]] || [[ "$dbName" = "" ]] || [[ "$inpFile" = "" ]] || [[ "$fileType" = "" ]]; then
		echo "Docker instance and database names and image metadata file are required."
		exit 1;
	fi
	if [[ "$fileType" != "--metadata" ]] && [[ "$fileType" != "--image" ]]; then
		echo "ERROR: Wrong input file type. Should be metadata or image."
		exit 1;
	fi
	
	if [[ "$fileType" = "--metadata" ]]; then
		tempDir="staging"$$"-"$RANDOM
		docker exec $dbDockerName mkdir /tmp/$tempDir
		baseName=$(basename $inpFile);
		docker cp $inpFile $dbDockerName:/tmp/$tempDir/$baseName
		docker exec $dbDockerName run_docker_load_metafile.sh $dbName /tmp/$tempDir $baseName
		exit 0;
	else
		cancerType=$5
		subjectId=$6
		caseId=$7
		if [[ "$cancerType" = "" ]] || [[ "$subjectId" = "" ]] || [[ "$caseId" = "" ]]; then
			echo "ERROR: cancer type, subject id, case id are missing."
			exit 1;
		fi
		tempDir="staging"$$"-"$RANDOM
		docker exec $dbDockerName mkdir /tmp/$tempDir
		baseName=$(basename $inpFile);
		docker cp $inpFile $dbDockerName:/tmp/$tempDir/$baseName
		docker exec $dbDockerName run_docker_load_imgmeta.sh $dbName /tmp/$tempDir $baseName $cancerType $subjectId $caseId
		exit 0;
	fi
fi

# load file
if [[ "$dbCmd" = "loadfile" ]]; then
	dbDockerName=$1
	shift
	dbName=$1
	shift
	dbInpFile=$1
	shift
	dbInpType=$1
	shift
	if [[ "$dbDockerName" = "" ]] || [[ "$dbName" = "" ]] || [[ "$dbInpFile" = "" ]] || [[ "$dbInpType" = "" ]]; then
		echo "Input parameters are missing."
		exit 1;
	fi
	if [[ "$dbInpType" != "csv" ]] && [[ "$dbInpType" != "mask" ]]; then 
		echo "Input type must be csv or mask."
		exit 1;
	fi
	tempDir="staging"$$"-"$RANDOM
	docker exec $dbDockerName mkdir /tmp/$tempDir
	baseName=$(basename $dbInpFile);
	docker cp $dbInpFile $dbDockerName:/tmp/$tempDir/$baseName
	docker exec $dbDockerName run_docker_load_file.sh $dbName /tmp/$tempDir $baseName $dbInpType "$@" 
	exit 0;
fi

# load zip file
if [[ "$dbCmd" = "loadzip" ]]; then
	dbDockerName=$1
	shift
	dbName=$1
	shift
	dbZipFile=$1
	shift
	dbInpType=$1
	shift
	if [[ "$dbDockerName" = "" ]] || [[ "$dbName" = "" ]] || [[ "$dbZipFile" = "" ]] || [[ "$dbInpType" = "" ]]; then
		echo "Input parameters are missing."
		exit 1;
	fi
	if [[ "$dbInpType" != "csv" ]] && [[ "$dbInpType" != "mask" ]]; then 
		echo "Input type must be csv or mask."
		exit 1;
	fi
	tempDir="staging"$$"-"$RANDOM
	docker exec $dbDockerName mkdir /tmp/$tempDir
	baseName=$(basename $dbZipFile);
	docker cp $dbZipFile $dbDockerName:/tmp/$tempDir/$baseName
	docker exec $dbDockerName run_docker_load_zip.sh $dbName /tmp/$tempDir $baseName $dbInpType "$@" 
	exit 0;
fi

# load zip file
if [[ "$dbCmd" = "loadquip" ]]; then
	dbDockerName=$1
	shift
	dbName=$1
	shift
	dbZipFile=$1
	shift
	dbInpType=$1
	shift
	if [[ "$dbDockerName" = "" ]] || [[ "$dbName" = "" ]] || [[ "$dbZipFile" = "" ]] || [[ "$dbInpType" = "" ]]; then
		echo "Input parameters are missing."
		exit 1;
	fi
	if [[ "$dbInpType" != "csv" ]] && [[ "$dbInpType" != "mask" ]]; then 
		echo "Input type must be csv or mask."
		exit 1;
	fi
	tempDir="staging"$$"-"$RANDOM
	docker exec $dbDockerName mkdir /tmp/$tempDir
	baseName=$(basename $dbZipFile);
	docker cp $dbZipFile $dbDockerName:/tmp/$tempDir/$baseName
	docker exec $dbDockerName run_docker_quip_zip.sh $dbName /tmp/$tempDir $baseName $dbInpType "$@" 
	exit 0;
fi

echo "Unknown command."
exit 1;

