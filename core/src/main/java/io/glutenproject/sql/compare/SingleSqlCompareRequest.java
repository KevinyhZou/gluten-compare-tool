package io.glutenproject.sql.compare;

import lombok.Data;

@Data
public class SingleSqlCompareRequest {
    private String user;
    private String sql;
}
