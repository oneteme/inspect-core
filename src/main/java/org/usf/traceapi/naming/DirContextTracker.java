package org.usf.traceapi.naming;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.traceapi.core.ExceptionInfo.mainCauseException;
import static org.usf.traceapi.core.Helper.threadName;
import static org.usf.traceapi.core.Session.appendSessionStage;
import static org.usf.traceapi.core.StageTracker.call;
import static org.usf.traceapi.core.StageTracker.exec;
import static org.usf.traceapi.naming.NamingAction.ATTRIB;
import static org.usf.traceapi.naming.NamingAction.CONNECTION;
import static org.usf.traceapi.naming.NamingAction.DISCONNECTION;
import static org.usf.traceapi.naming.NamingAction.LIST;
import static org.usf.traceapi.naming.NamingAction.LOOKUP;
import static org.usf.traceapi.naming.NamingAction.SEARCH;

import java.util.ArrayList;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.usf.traceapi.core.NamingRequest;
import org.usf.traceapi.core.NamingRequestStage;
import org.usf.traceapi.core.SafeCallable;
import org.usf.traceapi.core.StageTracker.StageConsumer;

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
	private DirContext ctx;
	private NamingRequest req;
	
	<T extends Exception> DirContextTracker connection(SafeCallable<DirContext, T> supplier) throws T {
		ctx = call(supplier, (s,e,c,t)-> {
			req = new NamingRequest();
			req.setStart(s);
 			if(nonNull(t)) {
				req.setEnd(e);
			}
 			var url = create(c.getEnvironment().get(PROVIDER_URL).toString());
			req.setProtocol(url.getScheme());
			req.setHost(url.getHost());
			req.setPort(url.getPort());
			req.setUser(c.getEnvironment().get(SECURITY_PRINCIPAL).toString());
			req.setThreadName(threadName());
			req.setActions(new ArrayList<>());
			appendAction(CONNECTION).accept(s, e, c, t);
			appendSessionStage(req);
		});
		return this;
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), appendAction(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), appendAction(LOOKUP, name));
	}
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), appendAction(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), appendAction(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), appendAction(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), appendAction(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), appendAction(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), appendAction(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), appendAction(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), appendAction(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), appendAction(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), appendAction(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), appendAction(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), appendAction(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), appendAction(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), appendAction(SEARCH, name));
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, (s,e,v,t)->{
			appendAction(DISCONNECTION).accept(s, e, v, t);
			req.setEnd(e);
		});
	}

	StageConsumer<Object> appendAction(NamingAction action, String... args) {
		return (s,e,o,t)-> {
			var stg = new NamingRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			stg.setException(mainCauseException(t));
			stg.setArgs(args);
			req.getActions().add(stg);
		};
	}
	
	//dummy spring org.springframework.ldap.NamingException
	public static <T extends Exception> DirContextTracker connect(SafeCallable<DirContext, T> supplier) throws T {
		return new DirContextTracker().connection(supplier);
	}
}
