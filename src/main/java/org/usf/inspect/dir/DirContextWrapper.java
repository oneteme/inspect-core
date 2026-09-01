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
	private final DirectoryRequestListener listener;

	
	/**
	 * 
	 * supports both javax.naming.NamingException & org.springframework.ldap.NamingException
	 */
	<E extends Throwable> DirContextWrapper(SafeCallable<DirContext, E> callable) throws E { 
		this(new DirectoryRequestListener(), callable);
	}
	
	<E extends Throwable> DirContextWrapper(DirectoryRequestListener listener, SafeCallable<DirContext, E> callable) throws E {
		this.listener = listener;
		this.ctx = call(callable, this.listener.connectionListener());
	}
	
	@Override
	public void close() throws NamingException {
		exec(ctx::close, listener.disconnectionListener());
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), listener.stageHandler(LOOKUP, name.toString()));
	}

	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), listener.stageHandler(LOOKUP, name));
	}
	
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), listener.stageHandler(LIST, name.toString()));
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), listener.stageHandler(LIST, name));
	}

	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), listener.stageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), listener.stageHandler(ATTRIB, name));
	}

	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), listener.stageHandler(ATTRIB, name.toString()));
	}

	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), listener.stageHandler(ATTRIB, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), listener.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), listener.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), listener.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), listener.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), listener.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), listener.stageHandler(SEARCH, name));
	}

	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, filterArgs, cons), listener.stageHandler(SEARCH, name.toString()));
	}

	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, filterArgs, cons), listener.stageHandler(SEARCH, name));
	}
}
