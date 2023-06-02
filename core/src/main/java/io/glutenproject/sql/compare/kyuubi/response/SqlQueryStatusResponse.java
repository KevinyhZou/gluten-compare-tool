package io.glutenproject.sql.compare.kyuubi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.glutenproject.sql.compare.kyuubi.KyuubiResponseBase;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlQueryStatusResponse implements KyuubiResponseBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private String statementId;
    private String remoteId;
    private String statement;
    private Boolean shouldRunAsync;
    private String state;
    private Long eventTime;
    private Long createTime;
    private Long startTime;
    private Long completeTime;
    private String sessionId;
    private String sessionUser;
    private String eventType;
    private Map exception;

    public String getExceptionMessage() throws Exception {
        if (exception == null) {
            return null;
        }
        if (exception.containsKey("message")) {
            return String.valueOf(exception.get("message"));
        } else {
            return OBJECT_MAPPER.writeValueAsString(exception);
        }
    }
}
