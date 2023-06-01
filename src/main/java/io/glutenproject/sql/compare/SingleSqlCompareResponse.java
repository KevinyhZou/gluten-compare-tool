package io.glutenproject.sql.compare;

import lombok.Data;

@Data
public class SingleSqlCompareResponse {
    private int code;
    private String message;

    public SingleSqlCompareResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
