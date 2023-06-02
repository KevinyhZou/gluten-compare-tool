#!/bin/bash

export JAVA_HOME=
export TOOL_HOME=

nohup $JAVA_HOME/bin/java -Xmx4G -Xms4G -Dspring.config.location=file:${TOOL_HOME}/conf/application.properties -cp $TOOL_HOME/jars/gluten-compare-tool-core-1.0-SNAPSHOT.jar:${TOOL_HOME}/jars/* io.glutenproject.sql.compare.Application >/dev/null 2>&1 &

