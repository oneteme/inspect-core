package org.usf.inspect.naming;

import static java.net.URI.create;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.ExceptionInfo.mainCauseException;
import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionManager.appendStage;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;
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
import org.usf.inspect.core.ExecutionMonitor.ExecutionMonitorFactory;

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
		exec(ctx::close, namingActionCreator(DISCONNECTION));
	}


	<T> ExecutionMonitorFactory<T> namingActionCreator(NamingAction action, String... args) {
		return namingActionCreator(req, action, args);
	}
	
	//dummy spring org.springframework.ldap.NamingException
	public static DirContextTracker context(SafeCallable<DirContext, RuntimeException> fn) {
		var req = new NamingRequest();
		var ctx = call(fn, s-> {
			req.setStart(s);
 			req.setThreadName(threadName());
			req.setActions(new ArrayList<>(3)); //cnx, act, dec
			appendStage(req);
			ExecutionMonitorFactory<DirContext> stg = namingActionCreator(req, CONNECTION);
			return stg.get(s).then((e,o,t)->{
	 			if(nonNull(o)) {
	 	 			var url = create(o.getEnvironment().get(PROVIDER_URL).toString());
	 				req.setProtocol(url.getScheme());
	 				req.setHost(url.getHost());
	 				req.setPort(url.getPort());
	 				req.setUser(o.getEnvironment().get(SECURITY_PRINCIPAL).toString());
	 			}
			});
		});
		return new DirContextTracker(ctx, req);
	}

	static <T> ExecutionMonitorFactory<T> namingActionCreator(NamingRequest req, NamingAction action, String... args) {
		return s-> {
			var stg = new NamingRequestStage();
			stg.setName(action.name());
			stg.setStart(s);
			stg.setArgs(args);
			req.append(stg); //at after setting
			return (e, o, t) -> {
				if(nonNull(t)) {
					stg.setException(mainCauseException(t));
				}
				stg.setEnd(e);
				if(action == DISCONNECTION || (action == CONNECTION && nonNull(t))) {
					req.setEnd(e);
				}
			};
		};
	}
}
