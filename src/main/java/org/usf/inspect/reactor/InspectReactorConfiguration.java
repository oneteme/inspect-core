package org.usf.inspect.reactor;

import static org.usf.inspect.core.Constants.REACTOR_ENABLED;
import static org.usf.inspect.core.ScheduledExecutorServiceWrapper.wrap;
import static reactor.core.publisher.Hooks.onEachOperator;
import static reactor.core.publisher.Hooks.resetOnEachOperator;
import static reactor.core.publisher.Operators.lift;
import static reactor.core.scheduler.Schedulers.onScheduleHook;
import static reactor.core.scheduler.Schedulers.removeExecutorServiceDecorator;
import static reactor.core.scheduler.Schedulers.resetOnScheduleHook;
import static reactor.core.scheduler.Schedulers.setExecutorServiceDecorator;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.usf.inspect.core.ContextPropagators;
import org.usf.inspect.core.InspectCoreConfiguration;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Configuration
@ConditionalOnBean(InspectCoreConfiguration.class) //inspect enabled
@ConditionalOnClass(name = "reactor.core.publisher.Hooks") //reactor dependency present
@ConditionalOnProperty(name = REACTOR_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectReactorConfiguration implements InitializingBean, DisposableBean {

	static final String HOOK_KEY = "inspect-context-propagation";

	@Override
	public void afterPropertiesSet() {
		setExecutorServiceDecorator(HOOK_KEY, (sc, es)-> wrap(es, true, "reactorExecutorServiceDecorator")); //TODO check this
		log.info("registering reactor hooks '{}'", HOOK_KEY);
		onScheduleHook(HOOK_KEY, ContextPropagators::wrapRunnable);
		onEachOperator(HOOK_KEY, lift(CoreSubscriberProxy::lift));
	}

	@Override
	public void destroy() {
		log.info("removing reactor hooks '{}'", HOOK_KEY);
		resetOnEachOperator(HOOK_KEY);
		resetOnScheduleHook(HOOK_KEY);
		removeExecutorServiceDecorator(HOOK_KEY);
	}
}