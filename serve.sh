#!/bin/sh

JAR=target/pensen-server-jar-with-dependencies.jar
MAIN_CLASS=ch.kinet.pensen.server.Server
CONFIG_FILE=application.cfg

java -cp ${JAR} -Dconfig.file=${CONFIG_FILE} ${MAIN_CLASS}
