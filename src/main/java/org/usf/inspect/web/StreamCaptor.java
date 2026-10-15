package org.usf.inspect.web;

import java.util.EventListener;

/**
 * 
 * @author u$f
 *
 */
public interface StreamCaptor {

	byte[] transferedBytes();

	long transferedSize();

	Throwable throwable();

	public interface StreamExchangeListener extends EventListener {

		void onTransmissionStart();

		void onTransmissionEnd();
	}
}