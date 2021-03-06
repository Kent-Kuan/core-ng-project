package core.log.kafka;

import core.framework.inject.Inject;
import core.framework.kafka.BulkMessageHandler;
import core.framework.kafka.Message;
import core.framework.log.message.StatMessage;
import core.log.service.StatService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
public class StatMessageHandler implements BulkMessageHandler<StatMessage> {
    @Inject
    StatService statService;

    @Override
    public void handle(List<Message<StatMessage>> messages) {
        List<StatMessage> stats = new ArrayList<>(messages.size());
        for (Message<StatMessage> message : messages) {
            stats.add(message.value);
        }
        statService.index(stats);
    }
}
