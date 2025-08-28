package org.usf.inspect.jdbc;

import static org.usf.inspect.core.ExecutionMonitor.call;
import static org.usf.inspect.jdbc.JDBCAction.CATALOG;
import static org.usf.inspect.jdbc.JDBCAction.COLUMN;
import static org.usf.inspect.jdbc.JDBCAction.DRIVER;
import static org.usf.inspect.jdbc.JDBCAction.KEYS;
import static org.usf.inspect.jdbc.JDBCAction.PRODUCT;
import static org.usf.inspect.jdbc.JDBCAction.SCHEMA;
import static org.usf.inspect.jdbc.JDBCAction.TABLE;
import static org.usf.inspect.jdbc.SqlCommand.EXPLORE;

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
	private final DatabaseRequestMonitor monitor;
	
	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(meta.getConnection(), monitor); //same tracer !?
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, monitor.stageHandler(PRODUCT, EXPLORE));
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, monitor.stageHandler(PRODUCT, EXPLORE));
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, monitor.stageHandler(DRIVER, EXPLORE));
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, monitor.stageHandler(DRIVER, EXPLORE));
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return new ResultSetWrapper(call(meta::getCatalogs, monitor.stageHandler(CATALOG, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return new ResultSetWrapper(call(meta::getSchemas, monitor.stageHandler(SCHEMA, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getSchemas(catalog, schemaPattern), monitor.stageHandler(SCHEMA, EXPLORE)), monitor);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), monitor.stageHandler(TABLE, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), monitor.stageHandler(TABLE, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), monitor.stageHandler(COLUMN, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), monitor.stageHandler(COLUMN, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getPrimaryKeys(catalog, schema, table), monitor.stageHandler(KEYS, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getImportedKeys(catalog, schema, table), monitor.stageHandler(KEYS, EXPLORE)), monitor);
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getExportedKeys(catalog, schema, table), monitor.stageHandler(KEYS, EXPLORE)), monitor);
	}
}
