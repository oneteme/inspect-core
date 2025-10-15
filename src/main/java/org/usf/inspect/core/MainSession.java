package org.usf.inspect.core;

import static org.usf.inspect.core.MainSessionType.STARTUP;
import static org.usf.inspect.core.SessionManager.releaseSession;
import static org.usf.inspect.core.SessionManager.releaseStartupSession;
import static org.usf.inspect.core.SessionManager.setCurrentSession;
import static org.usf.inspect.core.SessionManager.setStartupSession;

import java.time.Instant;

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

	@JsonIgnore
	@Delegate(excludes = CompletableMetric.class) //emit(this) + AbstractSession.wasCompleted
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
	public MainSession updateContext() {
		if(STARTUP.name().equals(getType())) {
			setStartupSession(this);
		}
		else {
			setCurrentSession(this);
		}
		return this;
	}

	@Override
	public MainSession releaseContext() {
		if(STARTUP.name().equals(getType())) {
			releaseStartupSession(this);
		}
		else {
			releaseSession(this);
		}
		return this;
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

	@Override
	public String getId() {
		return local.getId();
	}

	@Override
	public Instant getStart() {
		return local.getStart();
	}

	@Override
	public Instant getEnd() {
		return local.getEnd();
	}
}
