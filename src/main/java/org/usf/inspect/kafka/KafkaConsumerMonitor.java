package org.usf.inspect.kafka;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.KafkaAction.COMMIT;
import static org.usf.inspect.core.KafkaAction.CONSUME;
import static org.usf.inspect.core.KafkaCommand.OFFSET_COMMIT;
import static org.usf.inspect.core.KafkaCommand.POLL;

import org.apache.kafka.clients.consumer.Consumer;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.KafkaAction;
import org.usf.inspect.core.KafkaCommand;
import org.usf.inspect.core.KafkaRequestSignal;
import org.usf.inspect.core.KafkaRequestUpdate;
import org.usf.inspect.core.Monitor.StatefulMonitor;
import org.usf.inspect.core.SessionContextManager;

/**
 *
 * @author Tasnim
 *
 */
final class KafkaConsumerMonitor extends StatefulMonitor<KafkaRequestSignal, KafkaRequestUpdate> {


    ExecutionListener<Object> pollHandler(Object consumer) {
        return traceBegin(SessionContextManager::createKafkaRequest, (req, o) -> {
            var topics = ((Consumer<?, ?>) consumer).subscription();
            if (nonNull(topics)) {
                req.setTopic(topics.iterator().next());
            }
            var partitions = ((Consumer<?, ?>) consumer).assignment();
            if (nonNull(partitions)) {
                req.setPartition(partitions.iterator().next().partition());
            }
        }, stageHandler(CONSUME, POLL));
    }

    @Override
    protected KafkaRequestUpdate createCallback(KafkaRequestSignal session) {
        return session.createCallback();
    }

    ExecutionListener<Object> commitHandler() {
        return traceEnd(stageHandler(COMMIT, OFFSET_COMMIT));
    }

    <T> ExecutionListener<T> stageHandler(KafkaAction action, KafkaCommand cmd) {
        return traceStep((s, e, o, t) -> getCallback().createStage(action, s, e, t, cmd));
    }
}