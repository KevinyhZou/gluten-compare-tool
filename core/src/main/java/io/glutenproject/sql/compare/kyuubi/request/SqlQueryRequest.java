package io.glutenproject.sql.compare.kyuubi.request;

import io.glutenproject.sql.compare.kyuubi.KyuubiRequestBase;
import lombok.Data;

@Data
public class SqlQueryRequest implements KyuubiRequestBase {
    private String statement;
    private String runAsync;
    private Long queryTimeout;
}
