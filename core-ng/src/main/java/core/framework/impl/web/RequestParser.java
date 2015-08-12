package core.framework.impl.web;

import core.framework.api.http.HTTPMethod;
import core.framework.api.util.Charsets;
import core.framework.api.util.InputStreams;
import core.framework.api.util.Strings;
import core.framework.impl.log.ActionLog;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author neo
 */
public class RequestParser {
    private final Logger logger = LoggerFactory.getLogger(RequestParser.class);
    private final FormParserFactory formParserFactory = FormParserFactory.builder().build();

    void parse(RequestImpl request, HttpServerExchange exchange, ActionLog actionLog) throws IOException {
        request.method = HTTPMethod.valueOf(exchange.getRequestMethod().toString());
        actionLog.putContext("method", request.method());

        HeaderMap headers = exchange.getRequestHeaders();
        for (HeaderValues header : headers) {
            logger.debug("[request:header] {}={}", header.getHeaderName(), header.toArray());
        }

        String xForwardedFor = headers.getFirst(Headers.X_FORWARDED_FOR);
        String remoteAddress = exchange.getSourceAddress().getAddress().getHostAddress();
        logger.debug("[request] remoteAddress={}", remoteAddress);

        String clientIP = clientIP(remoteAddress, xForwardedFor);
        request.clientIP = clientIP;
        actionLog.putContext("clientIP", clientIP);

        String xForwardedProto = headers.getFirst(Headers.X_FORWARDED_PROTO);
        String requestScheme = exchange.getRequestScheme();
        logger.debug("[request] requestScheme={}", requestScheme);
        request.scheme = xForwardedProto != null ? xForwardedProto : requestScheme;

        String xForwardedPort = headers.getFirst(Headers.X_FORWARDED_PORT);
        int hostPort = exchange.getHostPort();
        logger.debug("[request] hostPort={}", hostPort);
        request.port = port(hostPort, xForwardedPort);

        actionLog.putContext("path", request.path());

        String requestURL = requestURL(request, exchange);
        request.requestURL = requestURL;
        logger.debug("[request] requestURL={}", requestURL);
        logger.debug("[request] queryString={}", exchange.getQueryString());

        String userAgent = headers.getFirst(Headers.USER_AGENT);
        if (userAgent != null) actionLog.putContext("userAgent", userAgent);

        if (request.method == HTTPMethod.POST || request.method == HTTPMethod.PUT) {
            request.contentType = headers.getFirst(Headers.CONTENT_TYPE);
            parseBody(request, exchange);
        }
    }

    void parseBody(RequestImpl request, HttpServerExchange exchange) throws IOException {
        if (request.contentType.startsWith("application/json")) {
            exchange.startBlocking();
            byte[] bytes = InputStreams.bytes(exchange.getInputStream());
            request.body = new String(bytes, Charsets.UTF_8);
            logger.debug("[request] body={}", request.body);
        } else if (request.method() == HTTPMethod.POST) {
            FormDataParser parser = formParserFactory.createParser(exchange);
            if (parser != null) {
                request.formData = parser.parseBlocking();
                for (String name : request.formData) {
                    logger.debug("[request:form] {}={}", name, request.formData.get(name));
                }
            }
        }
    }

    String clientIP(String remoteAddress, String xForwardedFor) {
        if (Strings.empty(xForwardedFor))
            return remoteAddress;
        int index = xForwardedFor.indexOf(',');
        if (index > 0)
            return xForwardedFor.substring(0, index);
        return xForwardedFor;
    }

    int port(int hostPort, String xForwardedPort) {
        if (xForwardedPort != null) {
            int index = xForwardedPort.indexOf(',');
            if (index > 0)
                return Integer.parseInt(xForwardedPort.substring(0, index));
            else
                return Integer.parseInt(xForwardedPort);
        }
        return hostPort;
    }

    String requestURL(RequestImpl request, HttpServerExchange exchange) {
        if (exchange.isHostIncludedInRequestURI()) {    // GET can use absolute url as request uri, http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
            return exchange.getRequestURI();
        } else {
            String scheme = request.scheme;
            int port = request.port;

            StringBuilder builder = new StringBuilder(scheme)
                .append("://")
                .append(exchange.getHostName());

            if (!(("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443))) {
                builder.append(':').append(port);
            }

            builder.append(exchange.getRequestURI());
            return builder.toString();
        }
    }
}