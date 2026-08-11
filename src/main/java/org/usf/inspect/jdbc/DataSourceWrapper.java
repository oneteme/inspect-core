package org.usf.inspect.jdbc;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.TraceDispatcherHub.hub;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * Wraps a {@link DataSource} to trace JDBC connections and statements.
 *
 * @author u$f
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;
	private final ConnectionMetadataCache cache = new ConnectionMetadataCache();

	/**
	 * Obtains and wraps a connection from the delegated data source.
	 *
	 * @return the wrapped connection
	 * @throws SQLException if the connection cannot be obtained
	 */
	@Override
	public Connection getConnection() throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(ds::getConnection, monitor.connectionHandler()), monitor);
	}

	/**
	 * Obtains and wraps a connection from the delegated data source using the given credentials.
	 *
	 * @param username the database user name
	 * @param password the database password
	 * @return the wrapped connection
	 * @throws SQLException if the connection cannot be obtained
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(()-> ds.getConnection(username, password), monitor.connectionHandler()), monitor);
	}
	
	/**
	 * Wraps the given data source when inspection is enabled.
	 *
	 * @param ds the data source to wrap
	 * @return the wrapped data source or the original instance
	 */
	public static DataSource wrap(DataSource ds) {
		return wrap(ds, null);
	}
	
	/**
	 * Wraps the given data source when inspection is enabled.
	 *
	 * @param ds the data source to wrap
	 * @param beanName the bean name used for logging
	 * @return the wrapped data source or the original instance
	 */
	public static DataSource wrap(@NonNull DataSource ds, String beanName) {
		if(hub().getConfiguration().isEnabled()){
			if(ds.getClass() != DataSourceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "dataSource"), ds.getClass());
				return new DataSourceWrapper(ds);
			}
			else {
				log.warn("{}: {} is already wrapped", beanName, ds);
			}
		}
		return ds;
	}
}
