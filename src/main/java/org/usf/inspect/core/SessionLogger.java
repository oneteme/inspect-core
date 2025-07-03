package org.usf.inspect.core;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
public final class SessionLogger implements SessionHandler<Metric> { //inspect.client.log : SESSION | REQUEST | STAGE
	
	private final List<Metric> metrics = Helper.synchronizedArrayList();

	@Override //sync. avoid session log collision
	public synchronized void handle(Metric s) {
		metrics.add(s);
    }
	
	@Override
	public void complete() throws Exception {
		metrics.forEach(m -> {
			log.info(m.toString());
		});
	}
}
