package org.usf.inspect.naming;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.requestAppender;
import static org.usf.inspect.core.StageTracker.call;
import static org.usf.inspect.core.StageTracker.exec;
import static org.usf.inspect.naming.NamingAction.ATTRIB;
import static org.usf.inspect.naming.NamingAction.CONNECTION;
import static org.usf.inspect.naming.NamingAction.DISCONNECTION;
import static org.usf.inspect.naming.NamingAction.LIST;
import static org.usf.inspect.naming.NamingAction.LOOKUP;
import static org.usf.inspect.naming.NamingAction.SEARCH;

import java.util.ArrayList;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.usf.inspect.core.NamingRequest;
import org.usf.inspect.core.NamingRequestStage;
import org.usf.inspect.core.SafeCallable;
import org.usf.inspect.core.StageTracker.StageCreator;

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
	private final NamingRequest req;

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), namingActionCreator(LOOKUP, name.toString()), req::append);
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), namingActionCreator(LOOKUP, name), req::append);
	}
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), namingActionCreator(LIST, name.toString()), req::append);
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), namingActionCreator(LIST, name), req::append);
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), namingActionCreator(ATTRIB, name.toString()), req::append);
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), namingActionCreator(ATTRIB, name), req::append);
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), namingActionCreator(ATTRIB, name.toString()), req::append);
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), namingActionCreator(ATTRIB, name), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), namingActionCreator(SEARCH, name.toString()), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), namingActionCreator(SEARCH, name), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), namingActionCreator(SEARCH, name.toString()), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), namingActionCreator(SEARCH, name), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), namingActionCreator(SEARCH, name.toString()), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), namingActionCreator(SEARCH, name), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), namingActionCreator(SEARCH, name.toString()), req::append);
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), namingActionCreator(SEARCH, name), req::append);
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, namingActionCreator(DISCONNECTION), stg->{
			req.append(stg);
			req.setEnd(stg.getEnd());
		});
	}

	static StageCreator<Object, NamingRequestStage> namingActionCreator(NamingAction action, String... args) {
		return (s,e,o,t)-> {
			var stg = new NamingRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setEnd(e);
			stg.setArgs(args);
			if(nonNull(t)) {
				stg.setException(mainCauseException(t));
			}
			return stg;
		};
	}
	
	//dummy spring org.springframework.ldap.NamingException
	public static DirContextTracker context(SafeCallable<DirContext, RuntimeException> fn) {
		var req = new NamingRequest();
		var ctx = call(fn, (s,e,c,t)-> {
			req.setStart(s);
 			req.setThreadName(threadName());
 			if(nonNull(c)) {
 	 			var url = create(c.getEnvironment().get(PROVIDER_URL).toString());
 				req.setProtocol(url.getScheme());
 				req.setHost(url.getHost());
 				req.setPort(url.getPort());
 				req.setUser(c.getEnvironment().get(SECURITY_PRINCIPAL).toString());
 			}
 			else if(nonNull(t)) {
				req.setEnd(e);
			}
			req.setActions(new ArrayList<>(3)); //cnx, act, dec
			req.append(namingActionCreator(CONNECTION).create(s, e, c, t));
			return req;
		}, requestAppender());
		return new DirContextTracker(ctx, req);
	}
}
