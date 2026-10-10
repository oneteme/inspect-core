package org.usf.inspect.core;

import static org.usf.inspect.core.BeanUtils.logRegistringBean;
import static org.usf.inspect.core.Constants.ANNOTATION_ENABLED;
import static org.usf.inspect.core.Constants.SYSTEM_ENABLED;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
@ConditionalOnBean(InspectConfiguration.class) //inspect enabled
@ConditionalOnProperty(name = SYSTEM_ENABLED, havingValue = "true", matchIfMissing = true)
public class InspectSystemMonitorConfiguration {

    @Bean
    @ConditionalOnProperty(name = ANNOTATION_ENABLED, havingValue = "true", matchIfMissing = true)
    MethodExecutionMonitor methodExecutionMonitor(AspectUserProvider aspectUser, InspectConfiguration config) {
    	logRegistringBean("methodExecutionMonitor", MethodExecutionMonitor.class);
    	return new MethodExecutionMonitor(aspectUser, config.getProperties().getMonitoring().getAnnotation());
    }
}
