package org.usf.inspect.kafka;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.KafkaAction.PRODUCE;
import static org.usf.inspect.core.KafkaCommand.SEND;

import org.apache.kafka.clients.producer.Producer;
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
final class KafkaProducerMonitor extends StatefulMonitor<KafkaRequestSignal, KafkaRequestUpdate> {

    @Override
    protected KafkaRequestUpdate createCallback(KafkaRequestSignal session) {
        return session.createCallback();
    }

    ExecutionListener<Object> sendHandler(Object producer) {
        return traceBegin(SessionContextManager::createKafkaRequest, (req, o) -> {
            var partitions = ((Producer<?, ?>) producer).partitionsFor(null); // get cluster metadata
            if (nonNull(partitions) && !partitions.isEmpty()) {
                req.setPartition(partitions.get(0).partition());
            }
        }, stageHandler(PRODUCE, SEND));
    }

    <T> ExecutionListener<T> stageHandler(KafkaAction action, KafkaCommand cmd) {
        return traceStep((s, e, o, t) -> getCallback().createStage(action, s, e, t, cmd));
    }
}