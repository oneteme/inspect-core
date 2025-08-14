package org.usf.inspect.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class MainSession extends AbstractSession {
	
	@Delegate
	@JsonIgnore
	private final LocalRequest local; //!exception
//	inherits String type //@see MainSessionType

	@JsonCreator 
	public MainSession() {
		this.local = new LocalRequest();
	}

	MainSession(MainSession ses) {
		super(ses);
		this.local = new LocalRequest(ses.local);
	}
	
	@Override
	public MainSession copy() {
		return new MainSession(this);
	}
	
	@Override
	public String toString() {
		return local.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		return CompletableMetric.areEquals(this, obj);
	}
	
	@Override
	public int hashCode() {
		return CompletableMetric.hashCodeOf(this);
	}
}
