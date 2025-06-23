package org.usf.inspect.naming;

import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.submit;
import static org.usf.inspect.naming.NamingAction.ATTRIB;
import static org.usf.inspect.naming.NamingAction.CONNECTION;
import static org.usf.inspect.naming.NamingAction.DISCONNECTION;
import static org.usf.inspect.naming.NamingAction.LIST;
import static org.usf.inspect.naming.NamingAction.LOOKUP;
import static org.usf.inspect.naming.NamingAction.SEARCH;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Function;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorListener;
import org.usf.inspect.core.NamingRequest;
import org.usf.inspect.core.NamingRequestStage;
import org.usf.inspect.core.SafeCallable;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class DirContextTracker implements DirContext {
	
	@Delegate
	private final DirContext ctx;
	private NamingRequest req;
	
	public DirContextTracker(SafeCallable<DirContext, RuntimeException> fn) {
		this.ctx = call(fn, toNamingRequest());
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), namingActionCreator(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), namingActionCreator(LOOKUP, name));
	}
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), namingActionCreator(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), namingActionCreator(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), namingActionCreator(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), namingActionCreator(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), namingActionCreator(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), namingActionCreator(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), namingActionCreator(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), namingActionCreator(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), namingActionCreator(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), namingActionCreator(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), namingActionCreator(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), namingActionCreator(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), namingActionCreator(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), namingActionCreator(SEARCH, name));
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, (s,e,o,t)-> submit(ses-> {
			req.append(newStage(DISCONNECTION, s, e, t));
			req.setEnd(e);
		}));
	}
	
	//dummy spring org.springframework.ldap.NamingException
	ExecutionMonitorListener<DirContext> toNamingRequest() {
		req = new NamingRequest();
		return (s,e,o,t)->{
			req.setThreadName(threadName());
			var url = getEnvironmentVariable(o, PROVIDER_URL, v-> URI.create(v.toString()));  //broke context dependency
			var user = getEnvironmentVariable(o, SECURITY_PRINCIPAL, Object::toString); //broke context dependency
			submit(ses-> {
				req.setStart(s);
				if(nonNull(t)) { //if connection error
					req.setEnd(e);
				}
	 			if(nonNull(url)) {
	 				req.setProtocol(url.getScheme());
	 				req.setHost(url.getHost());
	 				req.setPort(url.getPort());
	 				req.setUser(user);
	 			}
				req.setActions(new ArrayList<>(nonNull(t) ? 1 : 3)); //cnx, act, dec
				req.append(newStage(CONNECTION, s, e, t));
				ses.append(req);
			});
		};
	}
	
	<T> ExecutionMonitorListener<T> namingActionCreator(NamingAction action, String... args) {
		return (s,e,o,t)-> submit(ses-> req.append(newStage(action, s, e, t, args)));
	}
	
	static NamingRequestStage newStage(NamingAction action, Instant start, Instant end, Throwable t, String... args) {
		var stg = new NamingRequestStage();
		stg.setName(action.name());
		stg.setStart(start);
		stg.setEnd(end);
		stg.setArgs(args);
		if(nonNull(t)) {
			stg.setException(mainCauseException(t));
		}
		return stg;
	}
	
	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}

	public static DirContextTracker context(SafeCallable<DirContext, RuntimeException> fn) {
		return new DirContextTracker(fn);
	}
}
