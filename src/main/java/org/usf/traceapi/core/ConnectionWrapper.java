package org.usf.traceapi.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@RequiredArgsConstructor
public final class ConnectionWrapper implements Connection {
	
	@Delegate
	private final Connection cn;
	private final DatabaseActionTracer tracer;

	@Override
	public Statement createStatement() throws SQLException {
		return tracer.statement(cn::createStatement);
	}
	
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
		return tracer.statement(()-> cn.createStatement(resultSetType, resultSetConcurrency));
	}
	
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return tracer.statement(()-> cn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql, resultSetType, resultSetConcurrency));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql, autoGeneratedKeys));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql, columnIndexes));
	}
	
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
		return tracer.preparedStatement(()-> cn.prepareStatement(sql, columnNames));
	}	
}
