#!/bin/sh

cd /app

java -server -jar app.jar "${port}" "${xtdburl}"

