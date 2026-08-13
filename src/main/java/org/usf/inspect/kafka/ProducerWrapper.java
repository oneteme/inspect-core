package org.usf.inspect.kafka;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Tasnim
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProducerWrapper<K, V> implements Producer<K, V> {

	@Delegate
	private final Producer<K, V> producer;

	public Future<RecordMetadata> send(ProducerRecord<K, V> record) {
		KafkaProducerMonitor monitor = new KafkaProducerMonitor();
		return call(() -> producer.send(record), monitor.sendHandler(producer));
	}

	public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback) {
		KafkaProducerMonitor monitor = new KafkaProducerMonitor();
		return call(() -> producer.send(record, callback), monitor.sendHandler(producer));
	}

	public static <K, V> ProducerWrapper<K, V> wrap(Producer<K, V> producer) {
		return wrap(producer, null);
	}

	public static <K, V> ProducerWrapper<K, V> wrap(@NonNull Producer<K, V> producer, String beanName) {
		if (hub().getConfiguration().isEnabled()) {
			logWrappingBean(requireNonNullElse(beanName, "kafkaProducer"), producer.getClass());
		}
		return new ProducerWrapper<>(producer);
	}
}