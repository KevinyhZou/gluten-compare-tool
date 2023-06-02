package io.glutenproject.sql.compare;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.glutenproject.sql.compare.kyuubi.KyuubiClient;
import io.glutenproject.sql.compare.kyuubi.request.*;
import io.glutenproject.sql.compare.kyuubi.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import scala.Tuple2;
import scala.Tuple3;

import java.io.*;
import java.util.*;

@Slf4j
@Service
@PropertySource(value={"classpath:application.properties"})
public class CompareService {

    private static final String PATH_OPEN_SESSION = "/api/v1/sessions";
    private static final String PATH_CLOSE_SESSION = "/api/v1/sessions/%s";
    private static final String PATH_SQL_QUERY = "/api/v1/sessions/%s/operations/statement";
    private static final String PATH_SQL_QUERY_STATUS = "/api/v1/operations/%s/event";
    private static final String PATH_FETCH_RESULT = "/api/v1/operations/%s/rowset";
    private static final Integer DEFAULT_PROTOCOL_VERSION = 0;
    private static final String GLUTEN_CONFIG_KEY_PREFIX = "set:hivevar:";
    private static final String HIVE_CONFIG_KEY_PREFIX = "set:hiveconf:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Value("${kyuubi.http.request.user}")
    private String user;
    @Value("${kyuubi.http.request.password}")
    private String password;
    @Value("${kyuubi.http.request.timeout}")
    private Integer requestTimeout;
    @Value("${spark.kyuubi.http.url}")
    private String sparkKyuubiUrl;
    @Value("${gluten.kyuubi.http.url}")
    private String glutenKyuubiUrl;
    @Value("${gluten.sql.compare.result.path}")
    private String resultDiffDetailWritePath;
    @Autowired
    private SessionCache sessionCache;
    @Autowired
    private SessionConfig sessionConfig;
    private KyuubiClient sparkKyuubiClient;
    private KyuubiClient glutenKyuubiClient;

    public CompareService() {
        sparkKyuubiClient = new KyuubiClient();
        glutenKyuubiClient = new KyuubiClient();
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeSessions));
    }

    public String compareSql(String handleId, String user, String sql) {
        Tuple2<ResultRow[], String> glutenQueryResult = submitSqlAndGetResult(user, sql, true);
        Tuple2<ResultRow[], String> sparkQueryResult = submitSqlAndGetResult(user, sql, false);
        ResultRow[] glutenQueryResultRows = glutenQueryResult._1;
        ResultRow[] sparkQueryResultRows = sparkQueryResult._1;
        String glutenQueryErrMsg = glutenQueryResult._2;
        String sparkQueryErrMsg = sparkQueryResult._2;
        List<Tuple3<Boolean, ResultRow, ResultRow>> diffResult = compareAndGetDiffResult(glutenQueryResultRows, sparkQueryResultRows);
        if (diffResult == null || diffResult.isEmpty()) {
            log.info("The sql compare result is equal, the sql:" + sql);
            return "OK";
        } else {
            if (handleId == null) {
                handleId = UUID.randomUUID().toString();
            }
            String msg = String.format("the compare result is not equals, and the diff details is write to %s", resultDiffDetailWritePath + "/" + handleId);
            writeResultDiffDetail(handleId + ".result", sql, glutenQueryErrMsg, sparkQueryErrMsg, diffResult);
            return msg;
        }
    }

    public String compareBatch(String user, String sqlPath) {
        File f = new File(sqlPath);
        if (!f.exists()) {
            String errMsg = String.format("the sql path %s is not exist", sqlPath);
            throw new GlutenSqlCompareException(errMsg);
        }
        File[] sqlFiles = f.listFiles();
        if (sqlFiles == null) {
            String msg = "no sqls need compare";
            log.warn(msg);
            return msg;
        }
        Map<String, String> sqls = new TreeMap<>();
        for (File sqlFile : sqlFiles) {
            if (sqlFile.getName().endsWith(".sql")) {
                String sql = readSqlFromFile(sqlFile);
                sqls.put(sqlFile.getName(), sql);
            }
        }
        for (String key : sqls.keySet()) {
            compareSql(key, user, sqls.get(key).trim());
        }
        return String.format("the compare result is not equals, and the diff details is write to %s", resultDiffDetailWritePath);
    }

    private String readSqlFromFile(File sqlFile) {
        if (!sqlFile.exists()) {
            throw new GlutenSqlCompareException("sql file not exist:" + sqlFile.getAbsolutePath());
        }
        try (FileReader fr = new FileReader(sqlFile); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sqlBuilder = new StringBuilder();
            String s;
            while((s = br.readLine()) != null) {
                sqlBuilder.append(s).append("\n");
            }
            return sqlBuilder.toString();
        } catch (IOException e) {
            log.error("Failed to read sql from file", e);
            throw new GlutenSqlCompareException(e);
        }
    }

    private String openOrGetSession(String user, Boolean glutenSession) {
        if (glutenSession && sessionCache.getGlutenSessionId(user) == null) {
            glutenKyuubiClient.setKyuubiRequestUrl(glutenKyuubiUrl);
            glutenKyuubiClient.setUser(this.user);
            glutenKyuubiClient.setPassword(this.password);
            glutenKyuubiClient.setRequestTimeout(this.requestTimeout);
            String sessionId = openSession(true);
            log.info("Gluten Session opened, sessionId:{}, gluten:{}", sessionId, true);
            sessionCache.putGlutenSessionId(user, sessionId);
        }
        if (!glutenSession && sessionCache.getSparkSessionId(user) == null) {
            sparkKyuubiClient.setKyuubiRequestUrl(sparkKyuubiUrl);
            sparkKyuubiClient.setUser(this.user);
            sparkKyuubiClient.setPassword(this.password);
            sparkKyuubiClient.setRequestTimeout(this.requestTimeout);
            String sessionId = openSession(false);
            log.info("Spark Session opened, sessionId:{}, gluten:{}", sessionId, false);
            sessionCache.putSparkSessionId(user, sessionId);
        }
        return glutenSession ? sessionCache.getGlutenSessionId(user) : sessionCache.getSparkSessionId(user);
    }

    private Tuple2<ResultRow[], String> submitSqlAndGetResult(String user, String sql, Boolean glutenSession) {
        String sessionId = openOrGetSession(user, glutenSession);
        String statementId = submitSql(sessionId, sql, glutenSession);
        Long totalWaitTime = 0L;
        Long maxWaitTime = 60 * 60 * 1000L;
        Tuple2<String, String> jobStateInfo = querySqlJobStatus(sessionId, statementId, glutenSession);
        while(!"FINISHED_STATE".equals(jobStateInfo._1) && !"ERROR_STATE".equals(jobStateInfo._1)
            && totalWaitTime < maxWaitTime) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            totalWaitTime += 3000;
            jobStateInfo = querySqlJobStatus(sessionId, statementId, glutenSession);
        }
        if (totalWaitTime >= maxWaitTime) {
            String errMsg = String.format("The sql job run time long is more than max wait time %s", maxWaitTime);
            log.error(errMsg);
            // cancel the sql query
            return Tuple2.apply(null, errMsg);
        }
        if ("ERROR_STATE".equals(jobStateInfo._1)) {
            String errMsg = "The sql job run failed, please check the sql job. ";
            log.error(errMsg + " With sql: " + sql);
            return Tuple2.apply(null, errMsg + " With exception: " + jobStateInfo._2);
        }
        ResultRow[] resultRows = fetchSqlJobResult(sessionId, statementId, glutenSession);
        return Tuple2.apply(resultRows, null);
    }

    private void writeResultDiffDetail(String resultFileName, String sql, String glutenQueryErrMsg, String sparkQueryErrMsg, List<Tuple3<Boolean, ResultRow, ResultRow>> diffResult) {
        if (resultDiffDetailWritePath == null) {
            throw new GlutenSqlCompareException("Failed to write result diff to file, as the result path is null");
        }
        File resultDiffDir = new File(System.getProperty("user.dir") + "/" + resultDiffDetailWritePath);
        if (!resultDiffDir.exists()) {
            boolean mkdirSucc = resultDiffDir.mkdir();
            if (!mkdirSucc) {
                throw new GlutenSqlCompareException("Failed to create result diff dir, the path:" + resultDiffDetailWritePath);
            }
        }
        String resultDiffFilePath = resultDiffDetailWritePath + "/" + resultFileName;
        File resultDiffFile = new File(resultDiffFilePath);
        if (!resultDiffFile.exists()) {
            try {
                boolean createFileSucc = resultDiffFile.createNewFile();
                if (!createFileSucc) {
                    throw new GlutenSqlCompareException("Failed to create result diff file, the path:" + resultDiffFilePath);
                }
            } catch (Exception e) {
                log.error("Failed to create result diff file, the path:" + resultDiffFilePath, e);
                throw new GlutenSqlCompareException(e.getMessage());
            }
        }
        try (FileWriter fw = new FileWriter(resultDiffFile)) {
            String sqlToWrite = sql + "\n\n";
            fw.write("sql:" + sqlToWrite);
            if (glutenQueryErrMsg != null) {
                fw.write("Gluten query error message:" + glutenQueryErrMsg + "\n\n");
            }
            if (sparkQueryErrMsg != null) {
                fw.write("Spark query error message:" + sparkQueryErrMsg + "\n\n");
            }
            fw.write("the compare result show as below \n");
            fw.write("======Gluten Result================Spark Result========\n");
            for (Tuple3<Boolean, ResultRow, ResultRow> t : diffResult) {
                String s = t._1() +
                        "\t\t" +
                        (t._2() == null ? "null" : t._2().toString()) +
                        "\t\t" +
                        (t._3() == null ? "null" : t._3().toString()) +
                        "\n";
                fw.write(s);
            }
        } catch (Exception e) {
            log.error("Failed to write to result diff file, the file path:" + resultDiffFilePath, e);
            throw new GlutenSqlCompareException(e);
        }
    }

    private List<Tuple3<Boolean, ResultRow, ResultRow>> compareAndGetDiffResult(ResultRow[] first, ResultRow[] second) {
        List<Tuple3<Boolean, ResultRow, ResultRow>> diffResult = new ArrayList<>();
        boolean compareEqual = true;
        if (first == null && second == null) {
            return diffResult;
        } else if (first == null || second == null) {
            compareEqual = false;
            int len = first != null ? first.length : second.length;
            for (int i = 0; i < len; i ++) {
                Tuple3<Boolean, ResultRow, ResultRow> t;
                if (first != null) {
                    t = Tuple3.apply(false, first[i], null);
                } else {
                    t = Tuple3.apply(false, null, second[i]);
                }
                diffResult.add(t);
            }
        } else {
            int len = Math.min(first.length, second.length);
            for (int i = 0; i < len; i ++) {
                Tuple3<Boolean, ResultRow, ResultRow> t;
                if (!first[i].equals(second[i])) {
                    compareEqual = false;
                    t = Tuple3.apply(false, first[i], second[i]);
                } else {
                    t = Tuple3.apply(true, first[i], second[i]);
                }
                diffResult.add(t);
            }
            if (first.length != second.length) {
                compareEqual = false;
                int maxLen = Math.max(first.length, second.length);
                for (int j = len; j < maxLen; j ++) {
                    Tuple3<Boolean, ResultRow, ResultRow> t3;
                    if (first.length < second.length) {
                        t3 = Tuple3.apply(false, null, second[j]);
                    } else {
                        t3 = Tuple3.apply(false, first[j], null);
                    }
                    diffResult.add(t3);
                }
            }
        }
        return compareEqual ? null : diffResult;
    }

    private String openSession(boolean glutenSession) {
        try {
            OpenSessionRequest request = new OpenSessionRequest();
            request.setUser(user);
            request.setPassword(password);
            request.setProtocolVersion(DEFAULT_PROTOCOL_VERSION);
            request.setIpAddr("0.0.0.0");
            Map<String, Object> configs = ConfigUtils.convertConfigToMap(sessionConfig, HIVE_CONFIG_KEY_PREFIX, GLUTEN_CONFIG_KEY_PREFIX, glutenSession);
            request.setConfigs(configs);
            KyuubiClient kyuubiClient = getKyuubiClient(glutenSession);
            OpenSessionResponse response = kyuubiClient.requestToKyuubi(request, PATH_OPEN_SESSION, RequestMethod.POST, OpenSessionResponse.class);
            return response.getIdentifier();
        } catch (Exception e) {
            log.error("Failed to open session", e);
            throw new GlutenSqlCompareException(e);
        }
    }

    public void closeSession(String sessionId, Boolean glutenSession) {
        try {
            CloseSessionRequest request = new CloseSessionRequest();
            String requestPath = String.format(PATH_CLOSE_SESSION, sessionId);
            KyuubiClient kyuubiClient = getKyuubiClient(glutenSession);
            kyuubiClient.requestToKyuubi(request, requestPath, RequestMethod.DELETE, CloseSessionResponse.class);
        } catch (Exception e) {
            log.error("Failed to close session", e);
            throw new GlutenSqlCompareException(e);
        }
    }

    private void closeSessions() {
        log.info("Will close all the cached sessions");
        Map<String, String> gutenSessionIds = sessionCache.getGlutenSessionIds();
        Map<String, String> sparkSessionIds = sessionCache.getSparkSessionIds();
        for (String sessionId : gutenSessionIds.values()) {
            try {
                closeSession(sessionId, true);
            } catch (Exception e) {
                log.error("Failed to close session, sessionId:" + sessionId, e);
            }
        }
        for (String sessionId : sparkSessionIds.values()) {
            try {
                closeSession(sessionId, false);
            } catch (Exception e) {
                log.error("Failed to close session, sessionId:" + sessionId, e);
            }
        }
    }

    private String submitSql(String sessionId, String sql, Boolean glutenSession) {
        try {
            SqlQueryRequest request = new SqlQueryRequest();
            request.setRunAsync("true");
            request.setQueryTimeout(10000L);
            request.setStatement(sql);
            String sqlQueryPath = String.format(PATH_SQL_QUERY, sessionId);
            KyuubiClient kyuubiClient = getKyuubiClient(glutenSession);
            SqlQueryResponse response = kyuubiClient.requestToKyuubi(request, sqlQueryPath, RequestMethod.POST, SqlQueryResponse.class);
            return response.getIdentifier();
        } catch (Exception e) {
            log.error("Failed to submit sql, sessionId:{}, sql:{}", sessionId, sql, e);
            throw new GlutenSqlCompareException(e);
        }
    }

    private Tuple2<String, String> querySqlJobStatus(String sessionId, String statementId, Boolean glutenSession) {
        try {
            SqlQueryStatusRequest request = new SqlQueryStatusRequest();
            String sqlQueryStatusPath = String.format(PATH_SQL_QUERY_STATUS, statementId);
            KyuubiClient kyuubiClient = getKyuubiClient(glutenSession);
            SqlQueryStatusResponse response = kyuubiClient.requestToKyuubi(request, sqlQueryStatusPath, RequestMethod.GET, SqlQueryStatusResponse.class);
            if (response.getSessionId().equals(sessionId) && response.getStatementId().equals(statementId)) {
                return Tuple2.apply(response.getState(), response.getExceptionMessage());
            } else {
                String errMsg = String.format("Sql query status id not equals, the id should be: %s, %s, but the query result id: %s, %s", sessionId, statementId, response.getSessionId(), response.getStatementId());
                log.error(errMsg);
                throw new GlutenSqlCompareException(errMsg);
            }
        } catch (Exception e) {
            log.error("Failed to query sql job status, sessionId: {}, statementId: {}", sessionId, statementId);
            throw new GlutenSqlCompareException(e);
        }
    }

    private ResultRow[] fetchSqlJobResult(String sessionId, String statementId, Boolean glutenSession) {
        try {
            FetchResultRequest request = new FetchResultRequest();
            String fetchResultPath = String.format(PATH_FETCH_RESULT, statementId);
            KyuubiClient kyuubiClient = getKyuubiClient(glutenSession);
            FetchResultResponse response = kyuubiClient.requestToKyuubi(request, fetchResultPath, RequestMethod.GET, FetchResultResponse.class);
            return response.getRows();
        } catch (Exception e) {
            log.error("Failed to fetch sql job result, sessionId:{}, statementId:{}", sessionId, statementId, e);
            throw new GlutenSqlCompareException(e);
        }
    }

    public String test() {
        return "OK";
    }

    private KyuubiClient getKyuubiClient(Boolean glutenSession) {
        return glutenSession ? glutenKyuubiClient : sparkKyuubiClient;
    }
}
