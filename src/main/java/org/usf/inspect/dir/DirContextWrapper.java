package org.usf.inspect.dir;

import static org.usf.inspect.core.DirCommand.ATTRIB;
import static org.usf.inspect.core.DirCommand.LIST;
import static org.usf.inspect.core.DirCommand.LOOKUP;
import static org.usf.inspect.core.DirCommand.SEARCH;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectExecutor.exec;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.usf.inspect.core.SafeCallable;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public class DirContextWrapper implements DirContext {
	
	@Delegate
	private final DirContext ctx;
	private final DirectoryRequestListener listner = new DirectoryRequestListener();

	public DirContextWrapper(SafeCallable<DirContext, RuntimeException> callable) {
		this.ctx = call(callable, listner.connectionListener());
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, listner.disconnectionListener());
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), listner.stageHandler(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), listner.stageHandler(LOOKUP, name));
	}
	
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), listner.stageHandler(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), listner.stageHandler(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), listner.stageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), listner.stageHandler(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), listner.stageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), listner.stageHandler(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), listner.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), listner.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), listner.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), listner.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), listner.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), listner.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, filterArgs, cons), listner.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, filterArgs, cons), listner.stageHandler(SEARCH, name));
	}
}
