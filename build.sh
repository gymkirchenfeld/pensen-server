#!/bin/sh

# verify pwd
BASE_DIR=$(cd $(dirname "$0"); pwd)
mvn clean compile install
