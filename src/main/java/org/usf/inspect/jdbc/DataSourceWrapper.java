package org.usf.inspect.jdbc;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.core.BeanUtils.logWrappingBean;
import static org.usf.inspect.core.InspectContext.context;
import static org.usf.inspect.jdbc.ConnectionInfo.fromMetadata;

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
	private ConnectionInfo info;

	@Override
	public Connection getConnection() throws SQLException {
		return new DatabaseRequestMonitor().getConnection(ds::getConnection, this::connectionInfo);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return new DatabaseRequestMonitor().getConnection(()-> ds.getConnection(username, password), this::connectionInfo);
	}
	
	ConnectionInfo connectionInfo(Connection cnx) throws SQLException { //nullable cnx
		if(isNull(info) && nonNull(cnx)) {
			info = fromMetadata(cnx.getMetaData());
		}
		return info;
	}
	
	public static DataSource wrap(@NonNull DataSource ds) {
		if(context().getConfiguration().isEnabled()){
			logWrappingBean("dataSource", ds.getClass());
			return new DataSourceWrapper(ds);
		}
		return ds;
	}
}
