package org.usf.inspect.sql;

import java.sql.SQLException;
import java.sql.Wrapper;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
class AbstractWrapper<T extends Wrapper> implements Wrapper {
	
	protected final T delegate;

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this) || delegate.isWrapperFor(iface);
	}
	
	@Override
	public <U> U unwrap(Class<U> iface) throws SQLException {
		return iface.isInstance(this) ? iface.cast(this) : delegate.unwrap(iface);
	}
	
	protected void ensureTraced() {
		//do nothing by default
	}
}
