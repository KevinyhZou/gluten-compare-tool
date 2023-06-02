#!/bin/bash
pid=$(ps -ef | grep gluten-compare-tool-core | grep io.glutenproject.sql.compare.Application | awk '{print $2}')
kill $pid
