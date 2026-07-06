package org.usf.inspect.core;


import static java.util.Objects.nonNull;

/**
 *
 * @author Tasnim
 */
public interface ProtocolErrorHandler {


	int checkException(Throwable t);

	static Throwable mainCauseException(Throwable t) {
		if(nonNull(t)) {
			while(nonNull(t.getCause()) && t != t.getCause()) t = t.getCause();
			return t;
		}
		//si t déja null au début ce return va envoyer  null
		return t;

	}


}
