package org.usf.inspect.kafka;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.time.Duration;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;
import org.apache.kafka.common.TopicPartition;

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
public final class ConsumerWrapper<K, V> implements Consumer<K, V> {

	@Delegate
	private final Consumer<K, V> consumer;

	public ConsumerRecords<K, V> poll(Duration timeout) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		return call(() -> consumer.poll(timeout), monitor.pollHandler(consumer));
	}

	public void commitSync() {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(consumer::commitSync, monitor.commitHandler());
	}

	public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.commitSync(offsets), monitor.commitHandler());
	}

	public void commitSync(Duration timeout) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.commitSync(timeout), monitor.commitHandler());
	}

	public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets, Duration timeout) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.commitSync(offsets, timeout), monitor.commitHandler());
	}

	public void commitAsync() {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(consumer::commitAsync, monitor.commitHandler());
	}

	public void commitAsync(OffsetCommitCallback callback) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.commitAsync(callback), monitor.commitHandler());
	}

	public void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.commitAsync(offsets, callback), monitor.commitHandler());
	}

	public void close() {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(consumer::close, monitor.commitHandler());
	}

	public void close(Duration timeout) {
		KafkaConsumerMonitor monitor = new KafkaConsumerMonitor();
		exec(() -> consumer.close(timeout), monitor.commitHandler());
	}

	public static <K, V> ConsumerWrapper<K, V> wrap(Consumer<K, V> consumer) {
		return wrap(consumer, null);
	}

	public static <K, V> ConsumerWrapper<K, V> wrap(@NonNull Consumer<K, V> consumer, String beanName) {
		if (hub().getConfiguration().isEnabled()) {
			logWrappingBean(requireNonNullElse(beanName, "kafkaConsumer"), consumer.getClass());
		}
		return new ConsumerWrapper<>(consumer);
	}
}