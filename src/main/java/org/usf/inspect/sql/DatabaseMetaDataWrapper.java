package org.usf.inspect.sql;

import static org.usf.inspect.DatabaseCommand.GET;
import static org.usf.inspect.DatabaseCommand.SUPPORT;
import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class DatabaseMetaDataWrapper implements DatabaseMetaData {
	
	@Delegate
	private final DatabaseMetaData meta;
	private final ConnectionWrapper connection;
	
	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		return call(meta::supportsBatchUpdates, connection.getTracer().executeStageListener(SUPPORT, "BATCH_UPDATES"));
	}
	
	@Override
	public boolean supportsSavepoints() throws SQLException {
		return call(meta::supportsSavepoints, connection.getTracer().executeStageListener(SUPPORT, "SAVEPOINTS"));
	}
	
	@Override
	public boolean supportsTransactions() throws SQLException {
		return call(meta::supportsTransactions, connection.getTracer().executeStageListener(SUPPORT, "TRANSACTIONS"));
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, connection.getTracer().executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, connection.getTracer().executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, connection.getTracer().executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, connection.getTracer().executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return new ResultSetWrapper(call(meta::getCatalogs, connection.getTracer().executeStageListener(GET, "CATALOG")), connection);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return new ResultSetWrapper(call(meta::getSchemas, connection.getTracer().executeStageListener(GET, "SCHEMA")), connection);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getSchemas(catalog, schemaPattern), 
				connection.getTracer().executeStageListener(GET, "SCHEMA")), connection);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), 
				connection.getTracer().executeStageListener(GET, "TABLE")), connection);
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), 
				connection.getTracer().executeStageListener(GET, "TABLE")), connection);
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), 
				connection.getTracer().executeStageListener(GET, "COLUMN")), connection);
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), 
				connection.getTracer().executeStageListener(GET, "COLUMN")), connection);
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getPrimaryKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(GET, "KEYS")), connection);
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getImportedKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(GET, "KEYS")), connection);
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getExportedKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(GET, "KEYS")), connection);
	}
}
