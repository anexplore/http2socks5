#!/bin/bash
JVM_ARGS=""
LOG_CONFIG=" -Dlogback.configurationFile=logback.xml "

java ${JVM_ARGS} ${LOG_CONFIG} -jar http2socks5-1.0-SNAPSHOT-jar-with-dependencies.jar
