/**
 * ChoobConnectionWrapper.
 * @author James Ross
 */
package uk.co.uwcs.choob.support;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public final class ChoobConnectionWrapper implements Connection
{
	Connection conn;

	static void logSQL(final String sql) {
		System.out.println(System.currentTimeMillis() + " " + sql);
	}

	public ChoobConnectionWrapper(final Connection conn) {
		this.conn = conn;
	}

	@Override public void clearWarnings() throws SQLException {
		conn.clearWarnings();
	}

	@Override public void close() throws SQLException {
		conn.close();
	}

	@Override public void commit() throws SQLException {
		conn.commit();
	}

	@Override public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
		return conn.createArrayOf(typeName, elements);
	}

	@Override public Blob createBlob() throws SQLException {
		return conn.createBlob();
	}

	@Override public Clob createClob() throws SQLException {
		return conn.createClob();
	}

	@Override public NClob createNClob() throws SQLException {
		return conn.createNClob();
	}

	@Override public SQLXML createSQLXML() throws SQLException {
		return conn.createSQLXML();
	}

	@Override public Statement createStatement() throws SQLException {
		return new ChoobStatementWrapper(conn.createStatement());
	}

	@Override public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
		return new ChoobStatementWrapper(conn.createStatement(resultSetType, resultSetConcurrency));
	}

	@Override public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
		return new ChoobStatementWrapper(conn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
	}

	@Override public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
		return conn.createStruct(typeName, attributes);
	}

	@Override public boolean getAutoCommit() throws SQLException {
		return conn.getAutoCommit();
	}

	@Override public String getCatalog() throws SQLException {
		return conn.getCatalog();
	}

	@Override public Properties getClientInfo() throws SQLException {
		return conn.getClientInfo();
	}

	@Override public String getClientInfo(final String name) throws SQLException {
		return conn.getClientInfo(name);
	}

	@Override public int getHoldability() throws SQLException {
		return conn.getHoldability();
	}

	@Override public DatabaseMetaData getMetaData() throws SQLException {
		return conn.getMetaData();
	}

	@Override public int getTransactionIsolation() throws SQLException {
		return conn.getTransactionIsolation();
	}

	@Override public Map<String,Class<?>> getTypeMap() throws SQLException {
		return conn.getTypeMap();
	}

	@Override public SQLWarning getWarnings() throws SQLException {
		return conn.getWarnings();
	}

	@Override public boolean isClosed() throws SQLException {
		return conn.isClosed();
	}

	@Override public boolean isReadOnly() throws SQLException {
		return conn.isReadOnly();
	}

	@Override public boolean isValid(final int timeout) throws SQLException {
		return conn.isValid(timeout);
	}

	@Override public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return conn.isWrapperFor(iface);
	}

	@Override public String nativeSQL(final String sql) throws SQLException {
		return conn.nativeSQL(sql);
	}

	@Override public CallableStatement prepareCall(final String sql) throws SQLException {
		logSQL(sql);
		return conn.prepareCall(sql);
	}

	@Override public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
		logSQL(sql);
		return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
	}

	@Override public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
		logSQL(sql);
		return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override public PreparedStatement prepareStatement(final String sql) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql);
	}

	@Override public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql, autoGeneratedKeys);
	}

	@Override public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql, columnIndexes);
	}

	@Override public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}

	@Override public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	@Override public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
		logSQL(sql);
		return conn.prepareStatement(sql, columnNames);
	}

	@Override public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
		conn.releaseSavepoint(savepoint);
	}

	@Override public void rollback() throws SQLException {
		conn.rollback();
	}

	@Override public void rollback(final Savepoint savepoint) throws SQLException {
		conn.rollback(savepoint);
	}

	@Override public void setAutoCommit(final boolean autoCommit) throws SQLException {
		conn.setAutoCommit(autoCommit);
	}

	@Override public void setCatalog(final String catalog) throws SQLException {
		conn.setCatalog(catalog);
	}

	@Override public void setClientInfo(final Properties properties) throws SQLClientInfoException {
		conn.setClientInfo(properties);
	}

	@Override public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
		conn.setClientInfo(name, value);
	}

	@Override public void setHoldability(final int holdability) throws SQLException {
		conn.setHoldability(holdability);
	}

	@Override public void setReadOnly(final boolean readOnly) throws SQLException {
		conn.setReadOnly(readOnly);
	}

	@Override public Savepoint setSavepoint() throws SQLException {
		return conn.setSavepoint();
	}

	@Override public Savepoint setSavepoint(final String name) throws SQLException {
		return conn.setSavepoint(name);
	}

	@Override public void setTransactionIsolation(final int level) throws SQLException {
		conn.setTransactionIsolation(level);
	}

	@Override public void setTypeMap(final Map<String,Class<?>> map) throws SQLException {
		conn.setTypeMap(map);
	}

	@Override public <T> T unwrap(final Class<T> iface) throws SQLException {
		return conn.unwrap(iface);
	}

	public void setSchema(String schema) throws SQLException {
		// silently ignoring is fine
	}

	public String getSchema() throws SQLException {
		// null is an acceptable return value
		return null;
	}

	public void abort(Executor executor) throws SQLException {
		throw new SQLException("Method stub; not available in Java 6 and not supported: abort");
	}

	public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
		throw new SQLException("Method stub; not available in Java 6 and not supported: setNetworkTimeout");

	}

	public int getNetworkTimeout() throws SQLException {
		// no timeout; acceptable
		return 0;
	}
}
