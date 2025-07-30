package org.usf.inspect.jdbc;

import static org.usf.inspect.jdbc.JDBCAction.CATALOG;
import static org.usf.inspect.jdbc.JDBCAction.COLUMN;
import static org.usf.inspect.jdbc.JDBCAction.DRIVER;
import static org.usf.inspect.jdbc.JDBCAction.KEYS;
import static org.usf.inspect.jdbc.JDBCAction.PRODUCT;
import static org.usf.inspect.jdbc.JDBCAction.SCHEMA;
import static org.usf.inspect.jdbc.JDBCAction.TABLE;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class DatabaseMetaDataWrapper implements DatabaseMetaData {
	
	@Delegate
	private final DatabaseMetaData meta;
	private final DatabaseRequestMonitor tracer;
	
	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(meta.getConnection(), tracer); //same tracer !?
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return tracer.metaInfo(PRODUCT, meta::getDatabaseProductName);
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return tracer.metaInfo(PRODUCT, meta::getDatabaseProductVersion);
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return tracer.metaInfo(DRIVER, meta::getDriverName);
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return tracer.metaInfo(DRIVER, meta::getDriverVersion);
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return tracer.metaData(CATALOG, meta::getCatalogs);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return tracer.metaData(SCHEMA, meta::getSchemas);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return tracer.metaData(SCHEMA, ()-> meta.getSchemas(catalog, schemaPattern));
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return tracer.metaData(TABLE, ()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types));
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return tracer.metaData(TABLE, ()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern));
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return tracer.metaData(COLUMN, ()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern));
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return tracer.metaData(COLUMN, ()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern));
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.metaData(KEYS, ()-> meta.getPrimaryKeys(catalog, schema, table));
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.metaData(KEYS, ()-> meta.getImportedKeys(catalog, schema, table));
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.metaData(KEYS, ()-> meta.getExportedKeys(catalog, schema, table));
	}
}
