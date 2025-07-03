package org.usf.inspect.naming;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.TraceBroadcast.emit;
import static org.usf.inspect.core.SessionManager.startNamingRequest;
import static org.usf.inspect.naming.NamingAction.ATTRIB;
import static org.usf.inspect.naming.NamingAction.CONNECTION;
import static org.usf.inspect.naming.NamingAction.DISCONNECTION;
import static org.usf.inspect.naming.NamingAction.LIST;
import static org.usf.inspect.naming.NamingAction.LOOKUP;
import static org.usf.inspect.naming.NamingAction.SEARCH;

import java.time.Instant;
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
		this.ctx = call(fn, ldapRequestListener());
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), ldapStageListener(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), ldapStageListener(LOOKUP, name));
	}
	
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), ldapStageListener(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), ldapStageListener(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), ldapStageListener(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), ldapStageListener(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), ldapStageListener(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), ldapStageListener(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), ldapStageListener(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), ldapStageListener(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), ldapStageListener(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), ldapStageListener(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), ldapStageListener(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), ldapStageListener(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), ldapStageListener(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), ldapStageListener(SEARCH, name));
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, (s,e,o,t)-> {
			emit(ldapStage(DISCONNECTION, s, e, t));
			req.lazy(()-> {
				if(nonNull(t)) {
					req.setFailed(true);
				}
				req.setEnd(e);
				emit(req);
			});
		});
	}
	
	//dummy spring org.springframework.ldap.NamingException
	ExecutionMonitorListener<DirContext> ldapRequestListener() {
		req = startNamingRequest();
		return (s,e,o,t)->{
			req.setThreadName(threadName());
			req.setStart(s);
			if(nonNull(t)) { //if connection error
				req.setFailed(true);
				req.setEnd(e);
			}
			var url = getEnvironmentVariable(o, PROVIDER_URL, v-> create(v.toString()));  //broke context dependency
 			if(nonNull(url)) {
 				req.setProtocol(url.getScheme());
 				req.setHost(url.getHost());
 				req.setPort(url.getPort());
 			}
 			var user = getEnvironmentVariable(o, SECURITY_PRINCIPAL, Object::toString); //broke context dependency
 			if(nonNull(user)) {
 				req.setUser(user);
 			}
 			emit(req);
			emit(ldapStage(CONNECTION, s, e, t));
		};
	}
	
	<T> ExecutionMonitorListener<T> ldapStageListener(NamingAction action, String... args) {
		return (s,e,o,t)-> {
			emit(ldapStage(action, s, e, t, args));
			if(nonNull(t)) {
				req.lazy(()-> req.setFailed(true));
			}
		};
	}
	
	NamingRequestStage ldapStage(NamingAction action, Instant start, Instant end, Throwable t, String... args) {
		var stg = req.createStage(action.name(), start, end, t);
		stg.setArgs(args);
		return stg;
	}
	
	static <T> T getEnvironmentVariable(DirContext o, String key, Function<Object, T> fn) throws NamingException {
		var env = o.getEnvironment();
		if(nonNull(env) && env.containsKey(key)) {
			return fn.apply(env.get(key));
		}
		return null;
	}
}
