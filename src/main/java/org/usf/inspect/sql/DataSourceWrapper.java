package org.usf.inspect.sql;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.Constants.JDBC_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataSourceWrapper implements DataSource {
	
	@Delegate
	private final DataSource ds;
	private final ConnectionMetadataCache cache = new ConnectionMetadataCache();

	@Override
	public Connection getConnection() throws SQLException {
		var tracer = new DatabaseConnectionLifecycleTracer(cache);
		return new ConnectionWrapper(call(ds::getConnection, tracer.connectionListener()), tracer);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		var tracer = new DatabaseConnectionLifecycleTracer(cache);
		return new ConnectionWrapper(call(()-> ds.getConnection(username, password), tracer.connectionListener()), tracer);
	}
	
	public static DataSource wrap(DataSource ds) {
		return wrap(ds, null);
	}
	
	public static DataSource wrap(DataSource ds, String beanName) {
		if(nonNull(ds) && getConfiguration().isEnabled(JDBC_ENABLED, true)) {
			if(ds.getClass() != DataSourceWrapper.class) {
				logWrappingBean(requireNonNullElse(beanName, "dataSource"), ds.getClass());
				return new DataSourceWrapper(ds);
			}
			else {
				REPORTER.warn("ContextSourceWrapper.wrap: {} is already wrapped", requireNonNullElse(beanName, "dataSource"));
			}
		}
		return ds;
	}
}
