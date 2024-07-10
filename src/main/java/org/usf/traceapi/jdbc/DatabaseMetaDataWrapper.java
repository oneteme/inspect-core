package org.usf.traceapi.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class DatabaseMetaDataWrapper implements DatabaseMetaData  {
	
	@Delegate
	private final DatabaseMetaData meta;
	private final DatabaseStageTracker tracer;
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return tracer.databaseInfo(meta::getDatabaseProductName);
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return tracer.databaseInfo(meta::getDatabaseProductName);
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return tracer.schemaInfo(meta::getCatalogs);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return tracer.schemaInfo(meta::getSchemas);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getSchemas(catalog, schemaPattern));
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return tracer.schemaInfo(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types));
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern));
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern));
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern));
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.schemaInfo(()-> meta.getPrimaryKeys(catalog, schema, table));
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.schemaInfo(()-> meta.getImportedKeys(catalog, schema, table));
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return tracer.schemaInfo(()-> meta.getExportedKeys(catalog, schema, table));
	}
	
	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getProcedures(catalog, schemaPattern, procedureNamePattern));
	}
	
	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
		return tracer.schemaInfo(()-> meta.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern));
	}
}
