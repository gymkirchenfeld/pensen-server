#!/bin/sh

# abort on error
set -e

# verify pwd
BASE_DIR=$(cd $(dirname "$0"); pwd)

PARENT_DIR=$(dirname "${BASE_DIR}")

cd ${PARENT_DIR}/ch.kinet.datalib
mvn clean install

cd ${PARENT_DIR}/ch.kinet.pdflib
mvn clean install

cd ${BASE_DIR}
mvn clean compile install
