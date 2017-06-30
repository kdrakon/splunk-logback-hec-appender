#!/bin/bash

docker run --name splunk --hostname splunk -p 8000:8000 -p 8088:8088 -d -e "SPLUNK_START_ARGS=--accept-license" splunk/splunk:6.6.1