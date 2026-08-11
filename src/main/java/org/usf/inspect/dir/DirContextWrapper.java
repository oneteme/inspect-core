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

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Wraps a {@link DirContext} and monitors its directory operations.
 */
@RequiredArgsConstructor
public class DirContextWrapper implements DirContext {
	
	@Delegate
	private final DirContext ctx;
	private final DirectoryRequestMonitor monitor;
	
	/**
	 * Creates a monitored directory context by invoking the supplied context factory.
	 *
	 * @param fn the function that creates the underlying directory context
	 */
	public DirContextWrapper(SafeCallable<DirContext, RuntimeException> fn) {
		this.monitor = new DirectoryRequestMonitor();
		this.ctx = call(fn, monitor.handleConnection());
	}
	
	/**
	 * Closes the wrapped directory context and records the disconnection stage.
	 *
	 * @throws NamingException if closing the directory context fails
	 */
	@Override
	public void close() throws NamingException {
		exec(ctx::close, monitor.handleDisconnection());
	}

	/**
	 * Looks up an object by its composite name while recording the directory command.
	 *
	 * @param name the composite name of the object to look up
	 * @return the located object
	 * @throws NamingException if the lookup fails
	 */
	@Override
	public Object lookup(Name name) throws NamingException {
		return call(()-> ctx.lookup(name), monitor.executeStageHandler(LOOKUP, name.toString()));
	}

	/**
	 * Looks up an object by its string name while recording the directory command.
	 *
	 * @param name the string name of the object to look up
	 * @return the located object
	 * @throws NamingException if the lookup fails
	 */
	@Override
	public Object lookup(String name) throws NamingException {
		return call(()-> ctx.lookup(name), monitor.executeStageHandler(LOOKUP, name));
	}
	
	/**
	 * Lists the bindings under the supplied composite name while recording the directory command.
	 *
	 * @param name the composite name whose contents should be listed
	 * @return the enumeration of listed name-class pairs
	 * @throws NamingException if the listing fails
	 */
	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return call(()-> ctx.list(name), monitor.executeStageHandler(LIST, name.toString()));
	}

	/**
	 * Lists the bindings under the supplied string name while recording the directory command.
	 *
	 * @param name the string name whose contents should be listed
	 * @return the enumeration of listed name-class pairs
	 * @throws NamingException if the listing fails
	 */
	@Override
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return call(()-> ctx.list(name), monitor.executeStageHandler(LIST, name));
	}

	/**
	 * Retrieves the attributes for the supplied composite name while recording the directory command.
	 *
	 * @param name the composite name whose attributes should be retrieved
	 * @return the retrieved attributes
	 * @throws NamingException if attribute retrieval fails
	 */
	@Override
	public Attributes getAttributes(Name name) throws NamingException {
		return call(()-> ctx.getAttributes(name), monitor.executeStageHandler(ATTRIB, name.toString()));
	}

	/**
	 * Retrieves the attributes for the supplied string name while recording the directory command.
	 *
	 * @param name the string name whose attributes should be retrieved
	 * @return the retrieved attributes
	 * @throws NamingException if attribute retrieval fails
	 */
	@Override
	public Attributes getAttributes(String name) throws NamingException {
		return call(()-> ctx.getAttributes(name), monitor.executeStageHandler(ATTRIB, name));
	}

	/**
	 * Retrieves selected attributes for the supplied composite name while recording the directory command.
	 *
	 * @param name the composite name whose attributes should be retrieved
	 * @param attrIds the identifiers of the requested attributes
	 * @return the retrieved attributes
	 * @throws NamingException if attribute retrieval fails
	 */
	@Override
	public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), monitor.executeStageHandler(ATTRIB, name.toString()));
	}

	/**
	 * Retrieves selected attributes for the supplied string name while recording the directory command.
	 *
	 * @param name the string name whose attributes should be retrieved
	 * @param attrIds the identifiers of the requested attributes
	 * @return the retrieved attributes
	 * @throws NamingException if attribute retrieval fails
	 */
	@Override
	public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
		return call(()-> ctx.getAttributes(name, attrIds), monitor.executeStageHandler(ATTRIB, name));
	}

	/**
	 * Searches from the supplied composite name using matching attributes while recording the directory command.
	 *
	 * @param name the composite name that identifies the search base
	 * @param matchingAttributes the attributes used to match entries
	 * @param attributesToReturn the attributes to include in each result
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	/**
	 * Searches from the supplied string name using matching attributes while recording the directory command.
	 *
	 * @param name the string name that identifies the search base
	 * @param matchingAttributes the attributes used to match entries
	 * @param attributesToReturn the attributes to include in each result
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes, attributesToReturn), monitor.executeStageHandler(SEARCH, name));
	}

	/**
	 * Searches from the supplied composite name using matching attributes while recording the directory command.
	 *
	 * @param name the composite name that identifies the search base
	 * @param matchingAttributes the attributes used to match entries
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	/**
	 * Searches from the supplied string name using matching attributes while recording the directory command.
	 *
	 * @param name the string name that identifies the search base
	 * @param matchingAttributes the attributes used to match entries
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
		return call(()-> ctx.search(name, matchingAttributes), monitor.executeStageHandler(SEARCH, name));
	}

	/**
	 * Searches from the supplied composite name using an LDAP filter while recording the directory command.
	 *
	 * @param name the composite name that identifies the search base
	 * @param filter the LDAP filter expression
	 * @param cons the search controls to apply
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	/**
	 * Searches from the supplied string name using an LDAP filter while recording the directory command.
	 *
	 * @param name the string name that identifies the search base
	 * @param filter the LDAP filter expression
	 * @param cons the search controls to apply
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filter, cons), monitor.executeStageHandler(SEARCH, name));
	}

	/**
	 * Searches from the supplied composite name using a filter expression while recording the directory command.
	 *
	 * @param name the composite name that identifies the search base
	 * @param filterExpr the LDAP filter expression
	 * @param filterArgs the arguments referenced by the filter expression
	 * @param cons the search controls to apply
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), monitor.executeStageHandler(SEARCH, name.toString()));
	}

	/**
	 * Searches from the supplied string name using a filter expression while recording the directory command.
	 *
	 * @param name the string name that identifies the search base
	 * @param filterExpr the LDAP filter expression
	 * @param filterArgs the arguments referenced by the filter expression
	 * @param cons the search controls to apply
	 * @return the search results enumeration
	 * @throws NamingException if the search fails
	 */
	@Override
	public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
		return call(()-> ctx.search(name, filterExpr, cons), monitor.executeStageHandler(SEARCH, name));
	}
}
