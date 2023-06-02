package io.glutenproject.sql.compare;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SessionConfig {

    @Value("${kyuubi.client.address}")
    private String clientAddress;
    @Value("${hive.server2.thrift.resultset.default.fetch.size}")
    private String resultFetchSize;
    @Value("${hive.server2.proxy.user}")
    private String proxyUser;
    @Autowired
    private GlutenConfig glutenConfig;
    @Autowired
    private YarnConfig yarnConfig;

    @Data
    @Configuration
    public static class GlutenConfig {
        @Value("${spark.plugins}")
        private String plugins;
        @Value("${spark.serializer}")
        private String serializer;
        @Value("${spark.sql.sources.ignoreDataLocality}")
        private String sourceIgnoreDataLocality;
        @Value("${spark.sql.adaptive.enabled}")
        private Boolean adaptiveEnabled;
        @Value("${spark.sql.columnVector.offheap.enabled}")
        private Boolean columnVectorOffHeapEnabled;
        @Value("${spark.memory.offHeap.enabled}")
        private Boolean memoryOffHeapEnabled;
        @Value("${spark.memory.offHeap.size}")
        private Long memoryOffHeapSize;
        @Value("${spark.gluten.sql.columnar.columnartorow}")
        private Boolean columnarToArrowEnabled;
        @Value("${spark.gluten.sql.columnar.libpath}")
        private String columnarLibPath;
        @Value("${spark.gluten.sql.columnar.iterator}")
        private Boolean columnarIteratorEnabled;
        @Value("${spark.gluten.sql.columnar.loadarrow}")
        private Boolean columnarLoadArrowEnabled;
        @Value("${spark.gluten.sql.columnar.backend.lib}")
        private String columnarBackendLib;
        @Value("${spark.gluten.sql.columnar.hashagg.enablefinal}")
        private Boolean hashAggEnableFinal;
        @Value("${spark.gluten.sql.enable.native.validation}")
        private Boolean nativeValidationEnabled;
        @Value("${spark.io.compression.codec}")
        private String compressionCodec;
        @Value("${spark.gluten.sql.columnar.backend.ch.use.v2}")
        private Boolean columnarBackendChUseV2;
        @Value("${spark.gluten.sql.columnar.forceshuffledhashjoin}")
        private Boolean columnarForceShuffleHashJoin;
        @Value("${spark.sql.catalog.spark_catalog}")
        private String sparkCatalog;
        @Value("${spark.databricks.delta.snapshotPartitions}")
        private Integer deltaSnapshotPartitions;
        @Value("${spark.databricks.delta.properties.defaults.checkpointInterval}")
        private Integer deltaCheckpointInterval;
        @Value("${spark.gluten.sql.columnar.backend.ch.runtime_config.hdfs.libhdfs3_conf}")
        private String libhdfs3Conf;
        @Value("${spark.executorEnv.LD_PRELOAD}")
        private String executorLdPreLoad;
        @Value("${spark.files}")
        private String sparkFiles;
    }

    @Data
    @Configuration
    public static class YarnConfig {
        @Value("${spark.yarn.queue}")
        private String queue;
    }
}
