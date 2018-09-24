package core.framework.impl.http;

import core.framework.http.ContentType;
import core.framework.impl.log.filter.BytesLogParam;
import core.framework.impl.log.filter.JSONLogParam;
import core.framework.util.Strings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class BodyLogParamTest {
    @Test
    void bodyParam() {
        assertThat(BodyLogParam.param(Strings.bytes("{}"), ContentType.APPLICATION_JSON))
                .isInstanceOf(JSONLogParam.class);

        assertThat(BodyLogParam.param(Strings.bytes("value"), null))
                .isEqualTo("byte[5]");

        assertThat(BodyLogParam.param(Strings.bytes("<xml/>"), ContentType.TEXT_XML))
                .isInstanceOf(BytesLogParam.class);

        assertThat(BodyLogParam.param(new byte[10], ContentType.IMAGE_PNG))
                .isEqualTo("byte[10]");
    }
}
