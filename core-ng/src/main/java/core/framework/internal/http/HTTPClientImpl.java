package core.framework.internal.http;

import core.framework.http.HTTPClient;
import core.framework.http.HTTPClientException;
import core.framework.http.HTTPHeaders;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.internal.log.filter.MapLogParam;
import core.framework.log.ActionLogContext;
import core.framework.log.Markers;
import core.framework.util.StopWatch;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * @author neo
 */
public final class HTTPClientImpl implements HTTPClient {
    private final Logger logger = LoggerFactory.getLogger(HTTPClientImpl.class);
    private final String userAgent;
    private final long slowOperationThresholdInNanos;
    private final OkHttpClient client;

    public HTTPClientImpl(OkHttpClient client, String userAgent, Duration slowOperationThreshold) {
        this.client = client;
        this.userAgent = userAgent;
        slowOperationThresholdInNanos = slowOperationThreshold.toNanos();
    }

    @Override
    public HTTPResponse execute(HTTPRequest request) {
        var watch = new StopWatch();
        Request httpRequest = httpRequest(request);
        try (Response httpResponse = client.newCall(httpRequest).execute()) {
            return response(httpResponse);
        } catch (IOException e) {
            throw new HTTPClientException("http request failed, uri=" + request.uri, "HTTP_REQUEST_FAILED", e);
        } finally {
            long elapsed = watch.elapsed();
            ActionLogContext.track("http", elapsed);
            logger.debug("execute, elapsed={}", elapsed);
            if (elapsed > slowOperationThresholdInNanos) {
                logger.warn(Markers.errorCode("SLOW_HTTP"), "slow http operation, elapsed={}", elapsed);
            }
        }
    }

    HTTPResponse response(Response httpResponse) throws IOException {
        int statusCode = httpResponse.code();
        logger.debug("[response] status={}", statusCode);

        Map<String, String> headers = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        Headers httpHeaders = httpResponse.headers();
        for (int i = 0; i < httpHeaders.size(); i++) {
            headers.put(httpHeaders.name(i), httpHeaders.value(i));
        }
        logger.debug("[response] headers={}", new MapLogParam(headers));

        ResponseBody responseBody = httpResponse.body();
        if (responseBody == null) throw new Error("unexpected response body"); // refer to okhttp3.Response.body(), call.execute always return non-null body except for cachedResponse/networkResponse
        byte[] body = responseBody.bytes();

        var response = new HTTPResponse(statusCode, headers, body);
        logger.debug("[response] body={}", BodyLogParam.param(body, response.contentType));
        return response;
    }

    Request httpRequest(HTTPRequest request) {
        Request.Builder builder = new Request.Builder();

        try {
            String url = request.requestURI();
            builder.url(url);
            logger.debug("[request] method={}, url={}", request.method, url);
        } catch (IllegalArgumentException e) {
            throw new HTTPClientException("uri is invalid, uri=" + request.uri, "INVALID_URL", e);
        }

        if (!request.params.isEmpty())
            logger.debug("[request] params={}", new MapLogParam(request.params));   // due to null/empty will be serialized to empty value, so here to log actual params

        request.headers.put(HTTPHeaders.USER_AGENT, userAgent);
        for (Map.Entry<String, String> entry : request.headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        logger.debug("[request] headers={}", new MapLogParam(request.headers));

        if (request.body != null) {
            logger.debug("[request] body={}", BodyLogParam.param(request.body, request.contentType));
            builder.method(request.method.name(), RequestBody.create(request.body, MediaType.get(request.contentType.toString())));
        } else {
            RequestBody body = request.method == HTTPMethod.GET || request.method == HTTPMethod.HEAD ? null : RequestBody.create(new byte[0], null);
            builder.method(request.method.name(), body);
        }

        return builder.build();
    }
}
