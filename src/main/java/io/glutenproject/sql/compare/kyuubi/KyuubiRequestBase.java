package io.glutenproject.sql.compare.kyuubi;

public interface KyuubiRequestBase {

    default String convertToHttpRequestParameters() {
        return "";
    }

}
