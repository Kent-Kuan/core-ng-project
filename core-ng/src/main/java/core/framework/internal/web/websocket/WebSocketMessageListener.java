package core.framework.internal.web.websocket;

import core.framework.internal.log.ActionLog;
import core.framework.internal.log.LogManager;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author neo
 */
final class WebSocketMessageListener extends AbstractReceiveListener {
    private static final long MAX_TEXT_MESSAGE_SIZE = 10L * 1024 * 1024;     // 10M as max text message size to match max POST entity
    private final Logger logger = LoggerFactory.getLogger(WebSocketMessageListener.class);
    private final LogManager logManager;

    WebSocketMessageListener(LogManager logManager) {
        this.logManager = logManager;
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage textMessage) {
        var wrapper = (ChannelImpl) channel.getAttribute(WebSocketHandler.CHANNEL_KEY);
        ActionLog actionLog = logManager.begin("=== ws message handling begin ===");
        try {
            actionLog.action(wrapper.action);
            linkContext(channel, wrapper, actionLog);

            String data = textMessage.getData();
            logger.debug("[channel] message={}", data);     // not mask, assume ws message not containing sensitive info, the data can be json or plain text
            actionLog.track("ws", 0, 1, 0);

            Object message = wrapper.handler.fromClientMessage(data);
            wrapper.handler.listener.onMessage(wrapper, message);
        } catch (Throwable e) {
            logManager.logError(e);
            WebSockets.sendClose(CloseMessage.UNEXPECTED_ERROR, e.getMessage(), channel, ChannelCallback.INSTANCE);
        } finally {
            logManager.end("=== ws message handling end ===");
        }
    }

    @Override
    protected void onCloseMessage(CloseMessage message, WebSocketChannel channel) {
        var wrapper = (ChannelImpl) channel.getAttribute(WebSocketHandler.CHANNEL_KEY);
        ActionLog actionLog = logManager.begin("=== ws close message handling begin ===");
        try {
            actionLog.action(wrapper.action + ":close");
            linkContext(channel, wrapper, actionLog);

            int code = message.getCode();
            String reason = message.getReason();
            actionLog.context("code", code);
            logger.debug("[channel] reason={}", reason);
            actionLog.track("ws", 0, 1, 0);

            wrapper.handler.listener.onClose(wrapper, code, reason);
        } catch (Throwable e) {
            logManager.logError(e);
        } finally {
            logManager.end("=== ws close message handling end ===");
        }
    }

    private void linkContext(WebSocketChannel channel, ChannelImpl wrapper, ActionLog actionLog) {
        actionLog.context("channel", wrapper.id);
        logger.debug("refId={}", wrapper.refId);
        List<String> refIds = List.of(wrapper.refId);
        actionLog.refIds = refIds;
        actionLog.correlationIds = refIds;
        logger.debug("[channel] url={}", channel.getUrl());
        logger.debug("[channel] remoteAddress={}", channel.getSourceAddress().getAddress().getHostAddress());
        actionLog.context("client_ip", wrapper.clientIP);
        actionLog.context("listener", wrapper.handler.listener.getClass().getCanonicalName());
        actionLog.context("room", wrapper.rooms.toArray());
    }

    @Override
    protected long getMaxTextBufferSize() {
        return MAX_TEXT_MESSAGE_SIZE;
    }
}
