## Introduce
A tool to compare result between spark and [gluten](https://github.com/oap-project/gluten) with clickhouse backend.

## How to use
**1**. Get the dependency package. Depend on spark version 3.2.2 and kyuubi version 1.6.1-incubating.

**2**. Deploy 2 environments for spark, one for gluten and another not.
  - For deploy spark for gluten: 
    1) compile gluten code, get the clickhouse backend file: libch.so, put it into $SPARK_HOME directory, and config LD_PRELOAD in spark-env.sh, as `export LD_PRELOAD=$SPARK_HOME/libch.so`
    2) compile gluten code, get the gluten jar `gluten-0.5.0-SNAPSHOT-spark-3.2-jar-with-dependencies.jar`, put it into $SPARK_HOME/jars
  - For deploy spark not for gluten: decompress the spark package;
  
**3**. Deploy 2 environments for kyuubi, one for gluten and another not.
 - For deploy kyuubi for gluten: set config for `SPARK_HOME` in kyuubi-env.sh, point to the spark home directory path for gluten; set the config in kyuubi-defaults.conf to open kyuubi rest api
      ```
        kyuubi.frontend.rest.bind.host     127.0.0.1
        kyuubi.frontend.rest.bind.port     10098
        kyuubi.frontend.protocols=THRIFT_BINARY,THRIFT_HTTP,REST
      ```
 - For deploy kyuubi not for gluten: set config for `SPARK_HOME` in kyuubi-env.sh, point to the spark home directory path not for gluten; open the config for rest in kyuubi-defaults.conf as above

**4**. Depoly the `gluten-compare-tool`
 - Compile the code by use `mvn clean package`, get the install package under assembly/target, which is a compressed package named `*.tar.gz`;
 - Decompress the package, config the tool in `application.properties` and `log4j.properties`, set the gluten or spark kyuubi http request url in `application.properties`, and some other configs;
 - set `JAVA_HOME` and `TOOL_HOME` in bin/start.sh, and start the service by use start.sh or stop the service by stop.sh
 
**5**. Compare the sql, submit sql by single or batch, use the rest api provided by the tool, and which will save the result diff under directory `$TOOL_HOME/results`.
- For submit single sql, submit by rest api and put the sql in the parameters;
- For submit batch sql, you should put the sql script files in a directory, and to run them by use the batch submit rest api and put the sql files directory path in the parameters;  
 
## The rest api
The tool has 2 api for submit single sql or batch sqls to compare result between spark and gluten

**Submit single sql**
- request path: /gluten/compare/single
- request method: POST
- request body: 
    ```
    {
        "user":"abc",
        "sql": "select count(*) from test_tbl3"
    }
    ```
- Example
    ```
     curl -X POST -H "Content-Type:application/json" -d '{"user":"abc", "sql":"select count(*) from test_tbl3"}' 'http://${host}:${port}/gluten/compare/single'
    ```

**Submit batch sql**
- request path: /gluten/compare/batch
- request method: POST
- request body: 
   ```
   {
      "user":"abc",
      "sql_path":"/data/sqls/"
   }
   ```
  the sql path parameter is the path which store the sql script files
