package org.usf.inspect.dir;

import static org.usf.inspect.core.DirCommand.ATTRIB;
import static org.usf.inspect.core.DirCommand.LIST;
import static org.usf.inspect.core.DirCommand.LOOKUP;
import static org.usf.inspect.core.DirCommand.SEARCH;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.ExecutionMonitor.exec;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.usf.inspect.core.SafeCallable;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public class DirContextWrapper implements DirContext {
	
	@Delegate
	private final DirContext ctx;
	private final DirectoryRequestMonitor monitor;
	
	public DirContextWrapper(SafeCallable<DirContext, RuntimeException> fn) {
		this.monitor = new DirectoryRequestMonitor();
		this.ctx = call(fn, monitor::handleConnection);
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), monitor.executeStageHandler(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), monitor.executeStageHandler(LOOKUP, name));
	}
	
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), monitor.executeStageHandler(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), monitor.executeStageHandler(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), monitor.executeStageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), monitor.executeStageHandler(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), monitor.executeStageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), monitor.executeStageHandler(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), monitor.executeStageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), monitor.executeStageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), monitor.executeStageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), monitor.executeStageHandler(SEARCH, name));
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, monitor::handleDisconnection);
	}
}
