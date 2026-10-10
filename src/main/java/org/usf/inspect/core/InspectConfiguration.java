package org.usf.inspect.core;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logLoadingBean;
import static org.usf.inspect.core.Constants.INSPECT_ENABLED;
import static org.usf.inspect.core.Constants.SYSTEM_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@Getter
@Configuration
@ConditionalOnProperty(name = INSPECT_ENABLED, havingValue = "true", matchIfMissing = false) 
public class InspectConfiguration {

	private final Environment environment;
	private final InspectCollectorProperties properties;
	
	InspectConfiguration(Environment environment) {
		this.environment = environment;
		this.properties = loadConfiguration(environment);
	}
	
	@Bean //hook must be loaded before hubDispatcher
	@ConditionalOnProperty(name = SYSTEM_ENABLED, havingValue = "true", matchIfMissing = true)
	SystemMonitor systemMonitor() {
		logLoadingBean("systemMonitor", SystemMonitor.class);
		return new SystemMonitor(properties.getMonitoring().getSystem());
	}
	
	public String getProperty(String key) {
		return environment.getProperty(key);
	}
	
	public boolean getBooleanProperty(String key, boolean matchIfMissing) {
		var v = environment.getProperty(key);
		return nonNull(v) ? parseBoolean(v) : matchIfMissing;
	}
	
	static InspectCollectorProperties loadConfiguration(Environment env) {
		log.info("loading 'inspect.collector' configuration from environment");
		var bnd = Binder.get(env);
        var conf = bnd.bind("inspect.collector", InspectCollectorProperties.class)
        	.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector' configuration"));
        var mode = bnd.bind("inspect.collector.tracing.remote.mode", DispatchMode.class).orElse(null);
        if(nonNull(mode)) {
        	var expr = switch (mode) {
        	case REST -> bnd.bind("inspect.collector.tracing.remote", RestRemoteServerProperties.class)
        		.orElseThrow(()-> new IllegalStateException("cannot bind 'inspect.collector' configuration"));
        	default -> throw new UnsupportedOperationException(format("dispatching type '%s' is not supported, ", mode));
        	};
        	conf.getTracing().setRemote(expr);
        }
        conf.validate();
        return conf;
	}
}
