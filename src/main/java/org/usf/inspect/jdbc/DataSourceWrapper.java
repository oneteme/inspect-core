package org.usf.inspect.jdbc;

import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.core.InspectContext.context;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;
	private final ConnectionMetadataCache cache = new ConnectionMetadataCache();

	@Override
	public Connection getConnection() throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(ds::getConnection, monitor::handleConnection), monitor);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(()-> ds.getConnection(username, password), monitor::handleConnection), monitor);
	}
	
	public static DataSource wrap(@NonNull DataSource ds) {
		if(context().getConfiguration().isEnabled()){
			logWrappingBean("dataSource", ds.getClass());
			return new DataSourceWrapper(ds);
		}
		return ds;
	}
}
