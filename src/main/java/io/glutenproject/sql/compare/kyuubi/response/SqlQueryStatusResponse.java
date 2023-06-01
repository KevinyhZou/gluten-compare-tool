package io.glutenproject.sql.compare.kyuubi.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.glutenproject.sql.compare.kyuubi.KyuubiResponseBase;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlQueryStatusResponse implements KyuubiResponseBase {
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
}
