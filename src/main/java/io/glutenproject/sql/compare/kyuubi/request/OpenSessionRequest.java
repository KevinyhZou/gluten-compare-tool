package io.glutenproject.sql.compare.kyuubi.request;

import io.glutenproject.sql.compare.kyuubi.KyuubiRequestBase;
import lombok.Data;

import java.util.Map;

@Data
public class OpenSessionRequest implements KyuubiRequestBase {

    private Integer protocolVersion;
    private String user;
    private String password;
    private String ipAddr;
    private Map<String, Object> configs;

}
