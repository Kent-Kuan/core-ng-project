package core.framework.internal.kafka;

import core.framework.internal.log.ActionLog;
import core.framework.internal.log.LogManager;
import core.framework.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * @author neo
 */
class MessagePublisherImplTest {
    private MessagePublisherImpl<TestMessage> publisher;
    private MessageProducer producer;
    private LogManager logManager;

    @BeforeEach
    void createMessagePublisher() {
        producer = Mockito.mock(MessageProducer.class);
        publisher = new MessagePublisherImpl<>(producer, "topic", TestMessage.class);
        logManager = new LogManager();
    }

    @Test
    void publish() {
        ActionLog actionLog = logManager.begin("begin");
        actionLog.correlationIds = List.of("correlationId");

        var message = new TestMessage();
        message.stringField = "value";

        publisher.publish(message);
        verify(producer).send(argThat(record -> {
            assertThat(record.key()).isNull();
            assertThat(new String(record.headers().lastHeader(MessageHeaders.HEADER_CORRELATION_ID).value(), UTF_8)).isEqualTo("correlationId");
            assertThat(new String(record.headers().lastHeader(MessageHeaders.HEADER_REF_ID).value(), UTF_8)).isEqualTo(actionLog.id);
            return true;
        }));

        publisher.publish("topic", "key", message);
        verify(producer).send(argThat(record -> Arrays.equals(Strings.bytes("key"), record.key()) && "topic".equals(record.topic())));

        logManager.end("end");
    }
}
