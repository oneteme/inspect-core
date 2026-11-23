package org.usf.inspect.sql;

import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class ConnectionBuilderWrapper implements ConnectionBuilder {
	
	private final ConnectionBuilder cb;
	private final DataSourceWrapper ds;
	
	@Override
	public ConnectionBuilder user(String username) {
		cb.user(username);
		return this;
	}

	@Override
	public ConnectionBuilder password(String password) {
		cb.password(password);
		return this;
	}

	@Override
	public ConnectionBuilder shardingKey(ShardingKey shardingKey) {
		cb.shardingKey(shardingKey);
		return this;
	}

	@Override
	public ConnectionBuilder superShardingKey(ShardingKey superShardingKey) {
		cb.superShardingKey(superShardingKey);
		return this;
	}

	@Override
	public Connection build() throws SQLException {
		var trc = ds.createConnectionLifecycleTracer();
		return new ConnectionWrapper(call(cb::build, trc.connectionListener()), trc);
	}
}
