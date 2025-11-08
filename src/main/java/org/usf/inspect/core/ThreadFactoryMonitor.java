package org.usf.inspect.core;

import static java.util.concurrent.Executors.defaultThreadFactory;
import static org.usf.inspect.core.SessionManager.aroundRunnable;

import java.util.concurrent.ThreadFactory;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ThreadFactoryMonitor implements ThreadFactory {

	private final ThreadFactory factory;

	public ThreadFactoryMonitor() {
		this(defaultThreadFactory());
	}

	@Override
	public Thread newThread(Runnable r) {
		return factory.newThread(aroundRunnable(r));
	}
}
