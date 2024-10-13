#!/bin/sh

# verify pwd
BASE_DIR=$(cd $(dirname "$0"); pwd)

TEMP_DIR="pensen-server"
HOST="pensen.gymhofwil.ch"

STAGE2_FILE="install-stage2.sh"

ORIG_JAR_LOCAL_FILE="${BASE_DIR}/target/pensen-server-jar-with-dependencies.jar"
JAR_LOCAL_FILE="${BASE_DIR}/target/pensen-server.jar"
SERVICE_LOCAL_FILE="${BASE_DIR}/hofwil/pensen.service"
STAGE2_LOCAL_FILE="${BASE_DIR}/hofwil/${STAGE2_FILE}"

FILES="${JAR_LOCAL_FILE} ${SERVICE_LOCAL_FILE} ${STAGE2_LOCAL_FILE}"

# copy jar file
cp ${ORIG_JAR_LOCAL_FILE} ${JAR_LOCAL_FILE}
# remove old directory
ssh ${HOST} "rm -rf ${TEMP_DIR}"
ssh ${HOST} "mkdir -p ${TEMP_DIR}"

# upload files
for FILE in ${FILES}; do
  scp ${FILE} ${HOST}:${TEMP_DIR}/
done

# ensure execution permission for scripts
ssh ${HOST} "chmod u+x ${TEMP_DIR}/${STAGE2_FILE}"

# execute the stage2 script
ssh -t ${HOST} "${TEMP_DIR}/${STAGE2_FILE}"
