#!/bin/sh

TEMP_DIR="pensen-server"
APP_DIR="/srv/pensen-server"
JAR_FILE="pensen-server.jar"

SYSTEMD_DIR="/etc/systemd/system/"
SERVICE_FILE="pensen-api.service"

# stop the old server
sudo systemctl stop ${SERVICE_FILE}

# install new service file
sudo mv ${TEMP_DIR}/${SERVICE_FILE} ${SYSTEMD_DIR}
sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_FILE}

# install new application version
sudo mkdir -p ${APP_DIR}
sudo mv ${TEMP_DIR}/${JAR_FILE} ${APP_DIR}/${JAR_FILE}

# starting new server
sudo systemctl start ${SERVICE_FILE}
