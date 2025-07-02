package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@Getter
@Setter
@JsonTypeName("main")
public class MainSession extends AbstractSession {
	
	@Delegate
	@JsonIgnore
	private final LocalRequest local = new LocalRequest(); //!exception
//	inherits String type //@see MainSessionType
	
	@Override
	public Metric copy() {
		var ses = new MainSession();
		local.fill(ses.local);
		return ses;
	}
	
	public void setException(ExceptionInfo exceptions) {
		throw new UnsupportedOperationException();
	}
}
