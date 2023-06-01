package io.glutenproject.sql.compare;

public class GlutenSqlCompareException extends RuntimeException {

    public GlutenSqlCompareException(String msg) {
        super(msg);
    }

    public GlutenSqlCompareException(Exception e) {
        super(e);
    }
}
