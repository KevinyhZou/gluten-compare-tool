package io.glutenproject.sql.compare.kyuubi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.web.bind.annotation.RequestMethod;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;

@Slf4j
public class KyuubiClient {

    private static final int DEFAULT_HTTP_RETRY_COUNT = 3;
    private static final int DEFAULT_HTTP_REQUEST_TIMEOUT = 30000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private String kyuubiRequestUrl;
    private String user;
    private String password;
    private Integer requestTimeout;

    /**
     * generate the httpclient
     *
     * @return
     */
    private static CloseableHttpClient getHttpClient() {
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(100);
        connManager.setDefaultMaxPerRoute(10);
        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(DEFAULT_HTTP_RETRY_COUNT, true))
                .build();
    }

    /**
     * the method to request
     *
     * @param requestBase: the request entity: HttpGet, HttpPost,etc
     * @return tuple2: _1 http status code, _2 the json result
     * @throws IOException
     */
    private Tuple2<Integer, String> request(HttpRequestBase requestBase) throws IOException {
        try (CloseableHttpClient httpClient = getHttpClient()) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(requestTimeout == null ? DEFAULT_HTTP_REQUEST_TIMEOUT : requestTimeout)
                    .setConnectTimeout(requestTimeout == null ? DEFAULT_HTTP_REQUEST_TIMEOUT : requestTimeout)
                    .setConnectionRequestTimeout(requestTimeout == null ? DEFAULT_HTTP_REQUEST_TIMEOUT : requestTimeout)
                    .build();
            requestBase.setConfig(requestConfig);
            HttpResponse response = httpClient.execute(requestBase);
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder resultBuilder = new StringBuilder();
            String s = bufferedReader.readLine();
            while (s != null) {
                resultBuilder.append(s);
                s = bufferedReader.readLine();
            }
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Failed to do the request, with status code:{}", response.getStatusLine().getStatusCode());
            }
            return new Tuple2<>(response.getStatusLine().getStatusCode(), resultBuilder.toString());
        }
    }

    private HttpPost createHttpPostRequest(KyuubiRequestBase request, String requestPath) throws Exception {
        HttpPost httpPost = new HttpPost(kyuubiRequestUrl + requestPath);
        log.info("http post request url:{}",  kyuubiRequestUrl + requestPath);
        String requestBody = OBJECT_MAPPER.writeValueAsString(request);
        log.info("http post request body:{}", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "UTF8");
        httpPost.setEntity(requestEntity);
        httpPost.setHeader("Authorization",
                "Basic " + Base64.getUrlEncoder().encodeToString((user + ":" + password).getBytes()));
        httpPost.setHeader("Content-Type", "application/json");
        return httpPost;
    }

    private HttpGet createHttpGetRequest(KyuubiRequestBase request, String requestPath) {
        String reqParameters = request.convertToHttpRequestParameters();
        String reqUrl = kyuubiRequestUrl + requestPath + "?" + reqParameters;
        log.info("http get request url:{}", reqUrl);
        HttpGet getReq =  new HttpGet(reqUrl);
        getReq.setHeader("Content-Type", "application/json");
        return getReq;
    }

    private HttpDelete createHttpDeleteRequest(KyuubiRequestBase request, String requestPath) throws Exception {
        HttpDelete httpDelete = new HttpDelete(kyuubiRequestUrl + requestPath);
        log.info("http delete request url:{}", kyuubiRequestUrl + requestPath);
        return httpDelete;
    }

    public <T extends KyuubiResponseBase> T requestToKyuubi(
            KyuubiRequestBase request,
            String requestPath,
            RequestMethod method,
            Class<T> respClazz) throws Exception {
        HttpRequestBase httpReq = null;
        if (method == RequestMethod.POST) {
            httpReq = createHttpPostRequest(request, requestPath);
        } else if (method == RequestMethod.GET) {
            httpReq = createHttpGetRequest(request, requestPath);
        } else if (method == RequestMethod.DELETE) {
            httpReq = createHttpDeleteRequest(request, requestPath);
        } else {
            throw new RuntimeException("do not support such kind of http request:" + method);
        }
        Tuple2<Integer, String> respTuple = request(httpReq);
        if (HttpStatus.SC_OK == respTuple._1) {
            String respBody = respTuple._2;
            if (respBody != null && !respBody.isEmpty()) {
                return OBJECT_MAPPER.readValue(respBody, respClazz);
            } else {
                return respClazz.newInstance();
            }
        } else {
            throw new RuntimeException("Request Failed, with http code:" + respTuple._1
                    + "; with error msg:" + respTuple._2);
        }
    }

    public void setKyuubiRequestUrl(String url) {
        this.kyuubiRequestUrl = url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
