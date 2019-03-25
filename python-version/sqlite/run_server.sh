#!/bin/bash

gunicorn -w 8 -b 127.0.0.1:5000 sqlite_server:app 
