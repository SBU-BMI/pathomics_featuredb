#!/bin/bash

if [ -z $FEATUREDB_DIR ]; then
	echo "FEATUREDB_DIR is not set."
	exit 1;
fi

mongod --logpath=/data/db/db.log --storageEngine=wiredTiger --fork --directoryperdb "$@"
node $FEATUREDB_DIR/query_server/server.js &
/bin/bash

