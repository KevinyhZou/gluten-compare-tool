package io.glutenproject.sql.compare;

import lombok.Data;

@Data
public class BatchSqlCompareResponse {
    private int code;
    private String message;

    public BatchSqlCompareResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
