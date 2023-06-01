package io.glutenproject.sql.compare.kyuubi.response;

import io.glutenproject.sql.compare.kyuubi.KyuubiResponseBase;
import lombok.Data;

@Data
public class OpenSessionResponse implements KyuubiResponseBase {
    private String identifier;
}
