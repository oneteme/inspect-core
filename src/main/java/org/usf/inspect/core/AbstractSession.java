package org.usf.inspect.core;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.Helper.log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"lazy", "mutex", "tasks"})
public abstract class AbstractSession implements Session {

	private final AtomicInteger pending = new AtomicInteger();
	private List<RestRequest> restRequests;	//httpRequest
	private List<DatabaseRequest> databaseRequests;
	private List<LocalRequest> localRequests;
	private List<FtpRequest> ftpRequests;
	private List<MailRequest> mailRequests;
	private List<NamingRequest> ldapRequests;
	
	//v1.1
	private List<Trace> traces;
	private List<ExceptionInfo> exceptions;
	
	private List<Task> tasks;
	private volatile boolean lazy;
	private final Object mutex = new Object();
	
	public boolean appendException(ExceptionInfo e) {
		if(isNull(exceptions)) {
			exceptions = new ArrayList<>();
		}
		return exceptions.add(e);
	}

	public boolean submit(SessionStage<?> req) {
		synchronized (mutex) {
			return lazy 
					? tasks.add(ses-> append(req)) 
					: append(req);
		}
	}
	
	@Override
	public <T> boolean submit(SessionStage<T> request, T stage) {
		synchronized (mutex) {
			return lazy 
					? tasks.add(ses-> request.append(stage))
					: request.append(stage);
		}		
	}
	
	@Override
	public boolean submit(Trace trace) {
		synchronized (mutex) {
			return lazy 
					? tasks.add(ses-> getTraces().add(trace)) 
					: getTraces().add(trace);
		}
	}
	
	@Override
	public boolean submit(Task task) {
		synchronized (mutex) {
			return lazy 
					? tasks.add(task)
					: task.runSilently(this);
		}
	}
	
	public void setLazy(boolean value) {
		synchronized (mutex) {
			if(value != lazy) {
				if(value) {
					traces = new ArrayList<>();
				}
				else if(nonNull(tasks)) {
					for(var t : tasks) {
						t.runSilently(this);
					}
				}
				lazy = value;
			}
		}
	}
	
	public void invokeTasks() {
		List<Task> res = null;
		synchronized (mutex) {
			if(nonNull(tasks) && !tasks.isEmpty()) { //peek & clear
				res = new ArrayList<>(tasks);
				tasks = new ArrayList<>(); // release capacity
			}
		}
		if(nonNull(res)) {
			for(var t : res) {
				t.runSilently(this);
			}
		}
	}
	
	@Override
	public void lock(){ //must be called before session end
		pending.incrementAndGet();
	}

	@Override
	public void unlock() {
		pending.decrementAndGet();
	}

	@Override
	public boolean isCompleted() {
		var c = pending.get();
		if(c < 0) {
			log.warn("illegal session lock state={}, {}", c, this);
			return true;
		}
		return c == 0 && nonNull(getEnd());
	}

	@SuppressWarnings("unchecked")
	<T> boolean append(T o) {
		List<T> list = null;
		if(o.getClass() == DatabaseRequest.class) {
			list = (List<T>) getDatabaseRequests();
		}
		else if(o.getClass() == RestRequest.class) {
			list = (List<T>) getRestRequests();
		}
		else if(o.getClass() == LocalRequest.class) {
			list = (List<T>) getLocalRequests();
		}
		else if(o.getClass() == FtpRequest.class) {
			list = (List<T>) getFtpRequests();
		}
		else if(o.getClass() == MailRequest.class) {
			list = (List<T>) getMailRequests();
		}
		else if(o.getClass() == NamingRequest.class) {
			list = (List<T>) getLdapRequests();
		}
		else if(o.getClass() == Trace.class) {
			list = (List<T>) getTraces();
		}
		else {
			return false;
		}
		return list.add(o);
	}
	
	public List<DatabaseRequest> getDatabaseRequests() {
		if(isNull(databaseRequests)) {
			databaseRequests = new ArrayList<>();
		}
		return databaseRequests;
	}
	
	public List<RestRequest> getRestRequests() {
		if(isNull(restRequests)) {
			restRequests = new ArrayList<>();
		}
		return restRequests;
	}
	
	public List<LocalRequest> getLocalRequests() {
		if(isNull(localRequests)) {
			localRequests = new ArrayList<>();
		}
		return localRequests;
	}
	
	public List<FtpRequest> getFtpRequests() {
		if(isNull(ftpRequests)) {
			ftpRequests = new ArrayList<>();
		}
		return ftpRequests;
	}
	
	public List<MailRequest> getMailRequests() {
		if(isNull(mailRequests)) {
			mailRequests = new ArrayList<>();
		}
		return mailRequests;
	}

	public List<NamingRequest> getLdapRequests() {
		if(isNull(ldapRequests)) {
			ldapRequests = new ArrayList<>();
		}
		return ldapRequests;
	}
	
	public List<Trace> getTraces() {
		if(isNull(traces)) {
			traces = new ArrayList<>();
		}
		return traces;
	}
}
