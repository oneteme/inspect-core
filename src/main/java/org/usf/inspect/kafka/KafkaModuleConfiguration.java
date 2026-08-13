package org.usf.inspect.kafka;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.clients.consumer.Consumer;

/**
 * Configuration pour l'intégration Kafka avec inspect-core.
 *
 * Intercepte les factories Kafka Spring pour draper (wrapper) automatiquement
 * les instances de Consumer et Producer créées au runtime.
 *
 * @author Tasnim
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(prefix = "inspect.collector", name = "enabled", havingValue = "true")
public class KafkaModuleConfiguration {

	@Bean
	@DependsOn("inspectHub")
	public BeanPostProcessor kafkaFactoryPostProcessor() {
		logRegistringBean("kafkaFactoryPostProcessor", KafkaModuleConfiguration.class);

		return new BeanPostProcessor() {

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

				if (bean instanceof ProducerFactory<?, ?> pf) {
					pf.addPostProcessor(producer -> ProducerWrapper.wrap(producer, beanName));
				}

				if (bean instanceof ConsumerFactory<?, ?> cf) {
					cf.addPostProcessor(consumer -> ConsumerWrapper.wrap(consumer, beanName));
				}

				return bean;
			}
		};
	}
}