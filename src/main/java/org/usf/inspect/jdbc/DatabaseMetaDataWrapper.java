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
	private final DatabaseConnectionLifecycleTracer tracer;
	
	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(meta.getConnection(), tracer); //same tracer !?
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, tracer.executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, tracer.executeStageListener(GET, "PRODUCT"));
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, tracer.executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, tracer.executeStageListener(GET, "DRIVER"));
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return new ResultSetWrapper(call(meta::getCatalogs, tracer.executeStageListener(GET, "CATALOG")), tracer);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return new ResultSetWrapper(call(meta::getSchemas, tracer.executeStageListener(GET, "SCHEMA")), tracer);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getSchemas(catalog, schemaPattern), tracer.executeStageListener(GET, "SCHEMA")), tracer);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), tracer.executeStageListener(GET, "TABLE")), tracer);
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), tracer.executeStageListener(GET, "TABLE")), tracer);
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), tracer.executeStageListener(GET, "COLUMN")), tracer);
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), tracer.executeStageListener(GET, "COLUMN")), tracer);
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getPrimaryKeys(catalog, schema, table), tracer.executeStageListener(GET, "KEYS")), tracer);
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getImportedKeys(catalog, schema, table), tracer.executeStageListener(GET, "KEYS")), tracer);
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getExportedKeys(catalog, schema, table), tracer.executeStageListener(GET, "KEYS")), tracer);
	}
}
