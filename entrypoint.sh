#!/bin/sh

cd /app

java -server -Xms128m -Xmx128m -jar app.jar "${port}" "${xtdburl}"

