package core.framework.internal.web.websocket;

import core.framework.http.HTTPMethod;
import core.framework.internal.log.LogManager;
import core.framework.internal.web.session.SessionImpl;
import core.framework.internal.web.session.SessionManager;
import core.framework.web.Session;
import core.framework.web.exception.BadRequestException;
import core.framework.web.websocket.Channel;
import core.framework.web.websocket.ChannelListener;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author neo
 */
class WebSocketHandlerTest {
    private WebSocketHandler handler;
    private SessionManager sessionManager;

    @BeforeEach
    void createWebSocketHandler() {
        sessionManager = mock(SessionManager.class);

        handler = new WebSocketHandler(new LogManager(), sessionManager);
    }

    @Test
    void add() {
        assertThatThrownBy(() -> handler.add("/ws/:name", null))
            .isInstanceOf(Error.class)
            .hasMessageContaining("listener path must be static");

        assertThatThrownBy(() -> handler.add("/ws", new ChannelHandler(null, null, null, (channel, message) -> {
        }))).isInstanceOf(Error.class)
            .hasMessageContaining("listener class must not be anonymous class or lambda");

        ChannelHandler handler = new ChannelHandler(null, null, null, new TestChannelListener());
        this.handler.add("/ws", handler);
        assertThatThrownBy(() -> this.handler.add("/ws", handler))
            .isInstanceOf(Error.class)
            .hasMessageContaining("found duplicate channel listener");
    }

    @Test
    void checkWebSocket() {
        var headers = new HeaderMap()
            .put(Headers.SEC_WEB_SOCKET_KEY, "xxx")
            .put(Headers.SEC_WEB_SOCKET_VERSION, "13");

        assertThat(handler.checkWebSocket(HTTPMethod.GET, headers)).isTrue();
        assertThat(handler.checkWebSocket(HTTPMethod.PUT, headers)).isFalse();

        assertThatThrownBy(() -> handler.checkWebSocket(HTTPMethod.GET, headers.put(Headers.SEC_WEB_SOCKET_VERSION, "07")))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("only support web socket version 13");
    }

    @Test
    void loadSession() {
        when(sessionManager.load(any(), any())).thenReturn(new SessionImpl("localhost"));
        Session session = handler.loadSession(null, null);
        assertThat(session).isInstanceOf(ReadOnlySession.class);

        when(sessionManager.load(any(), any())).thenReturn(null);
        session = handler.loadSession(null, null);
        assertThat(session).isNull();
    }

    static class TestChannelListener implements ChannelListener<String> {
        @Override
        public void onMessage(Channel channel, String message) {

        }
    }
}
