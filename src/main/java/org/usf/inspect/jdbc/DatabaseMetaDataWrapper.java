package org.usf.inspect.jdbc;

import static org.usf.inspect.core.DatabaseCommand.GET;
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
	private final DatabaseRequestListener monitor;
	
	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(meta.getConnection(), monitor); //same monitor !?
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, monitor.executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, monitor.executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, monitor.executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, monitor.executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return new ResultSetWrapper(call(meta::getCatalogs, monitor.executeStageListener(GET, "CATALOG")), monitor);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return new ResultSetWrapper(call(meta::getSchemas, monitor.executeStageListener(GET, "SCHEMA")), monitor);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getSchemas(catalog, schemaPattern), monitor.executeStageListener(GET, "SCHEMA")), monitor);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), monitor.executeStageListener(GET, "TABLE")), monitor);
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), monitor.executeStageListener(GET, "TABLE")), monitor);
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), monitor.executeStageListener(GET, "COLUMN")), monitor);
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), monitor.executeStageListener(GET, "COLUMN")), monitor);
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getPrimaryKeys(catalog, schema, table), monitor.executeStageListener(GET, "KEYS")), monitor);
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getImportedKeys(catalog, schema, table), monitor.executeStageListener(GET, "KEYS")), monitor);
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getExportedKeys(catalog, schema, table), monitor.executeStageListener(GET, "KEYS")), monitor);
	}
}
