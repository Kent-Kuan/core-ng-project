package core.framework.http;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class ContentTypeTest {
    @Test
    void parse() {
        ContentType type = ContentType.parse("application/json; charset=utf-8");    // within cache
        assertThat(type.mediaType).isEqualTo("application/json");
        assertThat(type.charset()).get().isEqualTo(StandardCharsets.UTF_8);

        type = ContentType.parse("application/javascript; charset=utf-8");    // not in cache
        assertThat(type.mediaType).isEqualTo("application/javascript");
        assertThat(type.charset()).get().isEqualTo(StandardCharsets.UTF_8);

        type = ContentType.parse("application/vnd.maxmind.com-country+json; charset=UTF-8; version=2.1");
        assertThat(type.mediaType).isEqualTo("application/vnd.maxmind.com-country+json");
        assertThat(type.charset()).get().isEqualTo(StandardCharsets.UTF_8);

        type = ContentType.parse("image/png");
        assertThat(type.mediaType).isEqualTo("image/png");
        assertThat(type.charset()).isNotPresent();

        type = ContentType.parse("multipart/form-data; boundary=----WebKitFormBoundaryaANA7UQAvnwa2EkM");
        assertThat(type.mediaType).isEqualTo("multipart/form-data");
        assertThat(type.charset()).isNotPresent();
    }

    @Test
    void value() {
        assertThat(ContentType.APPLICATION_JSON.toString()).isEqualTo("application/json; charset=utf-8");
        assertThat(ContentType.APPLICATION_OCTET_STREAM.toString()).isEqualTo("application/octet-stream");
    }

    @Test
    void ignoreUnsupportedCharset() {
        ContentType type = ContentType.parse("image/jpeg; charset=binary");

        assertThat(type.mediaType).isEqualTo("image/jpeg");
        assertThat(type.charset()).isNotPresent();

        type = ContentType.parse("text/xml; charset=\"invalid\"");  // charset should not have double quote

        assertThat(type.mediaType).isEqualTo("text/xml");
        assertThat(type.charset()).isNotPresent();
    }
}
