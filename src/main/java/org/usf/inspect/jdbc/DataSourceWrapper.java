package org.usf.inspect.jdbc;

import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectExecutor.call;
import static org.usf.inspect.core.InspectContext.context;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author u$f
 *
 */
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;
	private final ConnectionMetadataCache cache = new ConnectionMetadataCache();

	@Override
	public Connection getConnection() throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(ds::getConnection, monitor.handleConnection()), monitor);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		var monitor = new DatabaseRequestMonitor(cache);
		return new ConnectionWrapper(call(()-> ds.getConnection(username, password), monitor.handleConnection()), monitor);
	}
	
	public static DataSource wrap(@NonNull DataSource ds) {
		return wrap(ds, null);
	}
	
	public static DataSource wrap(@NonNull DataSource ds, String beanName) {
		if(context().getConfiguration().isEnabled()){
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
