package org.usf.inspect.http;

import java.util.EventListener;

/**
 * 
 * @author u$f
 *
 */
public interface TransferPayload {
	
	byte[] bytes();
	
	long size();
	
	public interface StreamExchangeListener extends EventListener { //input/output stream payload
		
		void onTransmissionStart();
		
		void onTransmissionEnd();
	}

}