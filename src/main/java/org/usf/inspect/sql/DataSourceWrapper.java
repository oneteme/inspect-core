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
import java.sql.Wrapper;

import javax.sql.DataSource;

import org.usf.inspect.core.SafeCallable;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class DataSourceWrapper extends AbstractWrapper<DataSource> implements DataSource {

	@Delegate(excludes = Wrapper.class)
	private final DataSource ds;
	private final MetadataProvider metadataProvider;

	public DataSourceWrapper(DataSource ds, MetadataProvider metadataProvider) {
		super(ds);
		this.ds = ds;  // duplicated reference, cannot delegate in super class 
		this.metadataProvider = metadataProvider;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(ds::getConnection);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection(()-> ds.getConnection(username, password));
	}
	
	Connection getConnection(SafeCallable<Connection, SQLException> fn) throws SQLException {
		var trc = new DatabaseConnectionLifecycleTracer(metadataProvider);
		return new ConnectionWrapper(call(fn, trc.connectionListener()), trc);
	}
	
	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException {
		return new ConnectionBuilderWrapper(ds.createConnectionBuilder(), this);
	}
	
	@Override
	public String toString() {
		return "DataSourceWrapper [ds=" + ds + "]";
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
			catch (Exception e) { //may throw SQLException or RuntimeException
				REPORTER.error("DataSourceWrapper.wrap: cannot check if {} is already wrapped", beanName, e);
			}
		}
		return ds;
	}
}
