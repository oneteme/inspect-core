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
		local.copyIn(ses.local);
		return ses;
	}
	
	@Override
	public String toString() {
		return local.toString();
	}
}
