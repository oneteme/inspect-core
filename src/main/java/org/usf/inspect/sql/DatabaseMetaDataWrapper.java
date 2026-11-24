package org.usf.inspect.sql;

import static org.usf.inspect.DatabaseCommand.DESCRIBE;
import static org.usf.inspect.DatabaseCommand.GET;
import static org.usf.inspect.core.InspectExecutor.call;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Wrapper;

import lombok.experimental.Delegate;

/**
 * 
 * @author u$f
 *
 */
public final class DatabaseMetaDataWrapper extends AbstractWrapper<DatabaseMetaData> implements DatabaseMetaData {

	@Delegate(excludes = Wrapper.class)
	private final DatabaseMetaData meta;
	private final ConnectionWrapper connection;
	
	public DatabaseMetaDataWrapper(DatabaseMetaData meta, ConnectionWrapper connection) {
		super(meta);
		this.meta = meta;  // duplicated reference, cannot delegate in super class 
		this.connection = connection;
	}
	
	@Override
	public String getDatabaseProductName() throws SQLException {
		return call(meta::getDatabaseProductName, connection.getTracer().executeStageListener(GET, "PRODUCT_NAME"));
	}
	
	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return call(meta::getDatabaseProductVersion, connection.getTracer().executeStageListener(GET, "PRODUCT_VERSION"));
	}
	
	@Override
	public String getDriverName() throws SQLException {
		return call(meta::getDriverName, connection.getTracer().executeStageListener(GET, "DRIVER_NAME"));
	}
	
	@Override
	public String getDriverVersion() throws SQLException {
		return call(meta::getDriverVersion, connection.getTracer().executeStageListener(GET, "DRIVER_VERSION"));
	}
	
	@Override
	public ResultSet getCatalogs() throws SQLException {
		return connection.resultSet(meta::getCatalogs, 
				connection.getTracer().executeStageListener(DESCRIBE, "CATALOGS"), null);
	}
	
	@Override
	public ResultSet getSchemas() throws SQLException {
		return connection.resultSet(meta::getSchemas, 
				connection.getTracer().executeStageListener(DESCRIBE, "SCHEMAS"), null);
	}
	
	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
		return connection.resultSet(()-> meta.getSchemas(catalog, schemaPattern), 
				connection.getTracer().executeStageListener(DESCRIBE, "SCHEMAS"), null);
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
		return connection.resultSet(()-> meta.getTables(catalog, schemaPattern, tableNamePattern, types), 
				connection.getTracer().executeStageListener(DESCRIBE, "TABLES"), null);
	}
	
	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
		return connection.resultSet(()-> meta.getTablePrivileges(catalog, schemaPattern, tableNamePattern), 
				connection.getTracer().executeStageListener(DESCRIBE, "TABLE_PRIVILEGES"), null);
	}
	
	@Override
	public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
		return connection.resultSet(()-> meta.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern), 
				connection.getTracer().executeStageListener(DESCRIBE, "COLUMNS"), null);
	}
	
	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
		return connection.resultSet(()-> meta.getColumnPrivileges(catalog, schema, table, columnNamePattern), 
				connection.getTracer().executeStageListener(DESCRIBE, "COLUMN_PRIVILEGES"), null);
	}
	
	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
		return connection.resultSet(()-> meta.getPrimaryKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(DESCRIBE, "PRIMARY_KEYS"), null);
	}
	
	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
		return connection.resultSet(()-> meta.getImportedKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(DESCRIBE, "IMPORTED_KEYS"), null);
	}
	
	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
		return connection.resultSet(()-> meta.getExportedKeys(catalog, schema, table), 
				connection.getTracer().executeStageListener(DESCRIBE, "EXPORTED_KEYS"), null);
	}
	
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate)
			throws SQLException {
		return connection.resultSet(()-> meta.getIndexInfo(catalog, schema, table, unique, approximate), 
				connection.getTracer().executeStageListener(DESCRIBE, "INDEXS"), null);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		return connection;
	}
}
