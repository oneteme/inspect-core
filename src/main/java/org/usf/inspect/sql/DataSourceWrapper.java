package org.usf.inspect.sql;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.Constants.JDBC_ENABLED;
import static org.usf.inspect.core.Constants.REPORTER;
import static org.usf.inspect.core.InspectConfiguration.getConfiguration;
import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
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
	private final MetadataProvider metadataProvider;

	@Override
	public Connection getConnection() throws SQLException {
		var trc = createConnectionLifecycleTracer();
		return new ConnectionWrapper(call(ds::getConnection, trc.connectionListener()), trc);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		var trc = createConnectionLifecycleTracer();
		return new ConnectionWrapper(call(()-> ds.getConnection(username, password), trc.connectionListener()), trc);
	}
	
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this) || ds.isWrapperFor(iface);
	}
	
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return iface.isInstance(this) ? iface.cast(this) : ds.unwrap(iface);
	}
	
	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException {
		return new ConnectionBuilderWrapper(ds.createConnectionBuilder(), this);
	}
	
	@Override
	public String toString() {
		return "DataSourceWrapper [ds=" + ds + "]";
	}
	
	DatabaseConnectionLifecycleTracer createConnectionLifecycleTracer() {
		return new DatabaseConnectionLifecycleTracer(metadataProvider);
	}
	
	public static DataSource wrap(DataSource ds, MetadataProvider provider) {
		return wrap(ds, provider, null);
	}
	
	public static DataSource wrap(DataSource ds, MetadataProvider provider, String beanName) {
		if(nonNull(ds) && getConfiguration().isEnabled(JDBC_ENABLED, true)) {
			if(isNull(beanName)) {
				beanName = "dataSource";
			}
			try {
				if(!ds.isWrapperFor(DataSourceWrapper.class)) {
					logWrappingBean(beanName, ds.getClass());
					return new DataSourceWrapper(ds, provider);
				}
				REPORTER.warn("DataSourceWrapper.wrap: {} is already wrapped", beanName);
			}
			catch (SQLException e) {
				REPORTER.error("DataSourceWrapper.wrap: cannot check if {} is already wrapped", beanName, e);
			}
		}
		return ds;
	}
}
