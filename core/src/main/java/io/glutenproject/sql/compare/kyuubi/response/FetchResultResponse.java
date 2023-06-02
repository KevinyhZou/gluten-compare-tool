package io.glutenproject.sql.compare.kyuubi.response;

import io.glutenproject.sql.compare.ResultRow;
import io.glutenproject.sql.compare.kyuubi.KyuubiResponseBase;
import lombok.Data;

@Data
public class FetchResultResponse implements KyuubiResponseBase {

    private Integer rowCount;
    private ResultRow[] rows;

}
