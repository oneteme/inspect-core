package org.usf.inspect.jdbc;

import static org.usf.inspect.core.DatabaseAction.EXECUTE;
import static org.usf.inspect.core.DatabaseCommand.GET;
import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Wraps {@link DatabaseMetaData} calls to trace metadata access.
 *
 * @author u$f
 */
@RequiredArgsConstructor
public final class DatabaseMetaDataWrapper implements DatabaseMetaData {
	
	@Delegate
	private final DatabaseMetaData meta;
	private final DatabaseRequestMonitor monitor;
	
	/**
	 * Returns the wrapped connection associated with this metadata.
	 *
	 * @return the wrapped connection
	 * @throws SQLException if the connection cannot be obtained
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return new ConnectionWrapper(meta.getConnection(), monitor); //same monitor !?
	}
	
	/**
	 * Returns the database product name.
	 *
	 * @return the database product name
	 * @throws SQLException if the product name cannot be obtained
	 */
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, monitor.stageHandler(EXECUTE, GET, "PRODUCT"));
	}
	
	/**
	 * Returns the database product version.
	 *
	 * @return the database product version
	 * @throws SQLException if the product version cannot be obtained
	 */
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, monitor.stageHandler(EXECUTE, GET, "PRODUCT"));
	}
	
	/**
	 * Returns the JDBC driver name.
	 *
	 * @return the JDBC driver name
	 * @throws SQLException if the driver name cannot be obtained
	 */
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, monitor.stageHandler(EXECUTE, GET, "DRIVER"));
	}
	
	/**
	 * Returns the JDBC driver version.
	 *
	 * @return the JDBC driver version
	 * @throws SQLException if the driver version cannot be obtained
	 */
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, monitor.stageHandler(EXECUTE, GET, "DRIVER"));
	}
	
	/**
	 * Returns the available catalogs.
	 *
	 * @return the wrapped result set of catalogs
	 * @throws SQLException if the catalogs cannot be obtained
	 */
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return new ResultSetWrapper(call(meta::getCatalogs, monitor.stageHandler(EXECUTE, GET, "CATALOG")), monitor);
	}
	
	/**
	 * Returns the available schemas.
	 *
	 * @return the wrapped result set of schemas
	 * @throws SQLException if the schemas cannot be obtained
	 */
	@Override
	public ResultSet getSchemas() throws SQLException {
		return new ResultSetWrapper(call(meta::getSchemas, monitor.stageHandler(EXECUTE, GET, "SCHEMA")), monitor);
	}
	
	/**
	 * Returns the schemas matching the given filters.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schemaPattern the schema name pattern to use as a filter
	 * @return the wrapped result set of schemas
	 * @throws SQLException if the schemas cannot be obtained
	 */
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getSchemas(catalog, schemaPattern), monitor.stageHandler(EXECUTE, GET, "SCHEMA")), monitor);
	}

	/**
	 * Returns the tables matching the given filters.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schemaPattern the schema name pattern to use as a filter
	 * @param tableNamePattern the table name pattern to use as a filter
	 * @param types the table types to include
	 * @return the wrapped result set of tables
	 * @throws SQLException if the tables cannot be obtained
	 */
	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), monitor.stageHandler(EXECUTE, GET, "TABLE")), monitor);
	}
	
	/**
	 * Returns the table privileges matching the given filters.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schemaPattern the schema name pattern to use as a filter
	 * @param tableNamePattern the table name pattern to use as a filter
	 * @return the wrapped result set of table privileges
	 * @throws SQLException if the privileges cannot be obtained
	 */
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), monitor.stageHandler(EXECUTE, GET, "TABLE")), monitor);
	}
	
	/**
	 * Returns the columns matching the given filters.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schemaPattern the schema name pattern to use as a filter
	 * @param tableNamePattern the table name pattern to use as a filter
	 * @param columnNamePattern the column name pattern to use as a filter
	 * @return the wrapped result set of columns
	 * @throws SQLException if the columns cannot be obtained
	 */
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), monitor.stageHandler(EXECUTE, GET, "COLUMN")), monitor);
	}
	
	/**
	 * Returns the column privileges matching the given filters.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schema the schema name to use as a filter
	 * @param table the table name to use as a filter
	 * @param columnNamePattern the column name pattern to use as a filter
	 * @return the wrapped result set of column privileges
	 * @throws SQLException if the privileges cannot be obtained
	 */
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), monitor.stageHandler(EXECUTE, GET, "COLUMN")), monitor);
	}
	
	/**
	 * Returns the primary keys for the given table.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schema the schema name to use as a filter
	 * @param table the table name to inspect
	 * @return the wrapped result set of primary keys
	 * @throws SQLException if the keys cannot be obtained
	 */
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getPrimaryKeys(catalog, schema, table), monitor.stageHandler(EXECUTE, GET, "KEYS")), monitor);
	}
	
	/**
	 * Returns the imported keys for the given table.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schema the schema name to use as a filter
	 * @param table the table name to inspect
	 * @return the wrapped result set of imported keys
	 * @throws SQLException if the keys cannot be obtained
	 */
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getImportedKeys(catalog, schema, table), monitor.stageHandler(EXECUTE, GET, "KEYS")), monitor);
	}
	
	/**
	 * Returns the exported keys for the given table.
	 *
	 * @param catalog the catalog name to use as a filter
	 * @param schema the schema name to use as a filter
	 * @param table the table name to inspect
	 * @return the wrapped result set of exported keys
	 * @throws SQLException if the keys cannot be obtained
	 */
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return new ResultSetWrapper(call(()-> meta.getExportedKeys(catalog, schema, table), monitor.stageHandler(EXECUTE, GET, "KEYS")), monitor);
	}
}
