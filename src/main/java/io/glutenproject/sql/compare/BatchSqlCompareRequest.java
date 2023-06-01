package io.glutenproject.sql.compare;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BatchSqlCompareRequest {

    @JsonProperty("sql_path")
    private String sqlPath;
    private String user;
}
