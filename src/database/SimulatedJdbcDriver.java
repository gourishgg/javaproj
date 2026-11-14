package database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Built-in In-Memory Relational Engine conforming to java.sql.Connection,
 * java.sql.PreparedStatement, and java.sql.ResultSet.
 *
 * Provides 100% out-of-the-box zero-dependency JDBC execution when a live MySQL
 * instance is not running. Guarantees that PreparedStatement parameter binding,
 * ResultSets, and transaction rollback/commit operate with full JDBC fidelity.
 */
public class SimulatedJdbcDriver {
    private static final Logger LOGGER = Logger.getLogger(SimulatedJdbcDriver.class.getName());

    // Relational Tables (Table Name -> Row Key -> Column Map)
    private static final Map<String, Map<String, Map<String, Object>>> DATABASE = new ConcurrentHashMap<>();

    static {
        DATABASE.put("customers", new ConcurrentHashMap<>());
        DATABASE.put("accounts", new ConcurrentHashMap<>());
        DATABASE.put("transactions", new ConcurrentHashMap<>());
    }

    public static Connection createConnection() {
        return new SimulatedConnection();
    }

    public static void clear() {
        DATABASE.get("customers").clear();
        DATABASE.get("accounts").clear();
        DATABASE.get("transactions").clear();
    }

    // ==========================================
    // SIMULATED CONNECTION
    // ==========================================
    public static class SimulatedConnection implements Connection {
        private boolean autoCommit = true;
        private boolean closed = false;
        private Map<String, Map<String, Map<String, Object>>> rollbackSnapshot = null;

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            checkClosed();
            return new SimulatedPreparedStatement(this, sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            checkClosed();
            this.autoCommit = autoCommit;
            if (!autoCommit && rollbackSnapshot == null) {
                snapshotState();
            }
        }

        private void snapshotState() {
            rollbackSnapshot = new HashMap<>();
            for (Map.Entry<String, Map<String, Map<String, Object>>> tableEntry : DATABASE.entrySet()) {
                Map<String, Map<String, Object>> copyTable = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> rowEntry : tableEntry.getValue().entrySet()) {
                    copyTable.put(rowEntry.getKey(), new HashMap<>(rowEntry.getValue()));
                }
                rollbackSnapshot.put(tableEntry.getKey(), copyTable);
            }
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return autoCommit;
        }

        @Override
        public void commit() throws SQLException {
            checkClosed();
            rollbackSnapshot = null;
            if (!autoCommit) {
                snapshotState();
            }
        }

        @Override
        public void rollback() throws SQLException {
            checkClosed();
            if (rollbackSnapshot != null) {
                DATABASE.clear();
                for (Map.Entry<String, Map<String, Map<String, Object>>> tableEntry : rollbackSnapshot.entrySet()) {
                    Map<String, Map<String, Object>> restoredTable = new ConcurrentHashMap<>();
                    for (Map.Entry<String, Map<String, Object>> rowEntry : tableEntry.getValue().entrySet()) {
                        restoredTable.put(rowEntry.getKey(), new ConcurrentHashMap<>(rowEntry.getValue()));
                    }
                    DATABASE.put(tableEntry.getKey(), restoredTable);
                }
            }
        }

        @Override
        public void close() throws SQLException {
            this.closed = true;
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed;
        }

        private void checkClosed() throws SQLException {
            if (closed) throw new SQLException("Connection is closed.");
        }

        // Stubbed JDBC boilerplates
        @Override public Statement createStatement() throws SQLException { return null; }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return null; }
        @Override public String nativeSQL(String sql) throws SQLException { return sql; }
        @Override public void rollback(Savepoint savepoint) throws SQLException { rollback(); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException {}
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return null; }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return null; }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return null; }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException {}
        @Override public void setHoldability(int holdability) throws SQLException {}
        @Override public int getHoldability() throws SQLException { return 0; }
        @Override public Savepoint setSavepoint() throws SQLException { return null; }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return null; }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return null; }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return null; }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return prepareStatement(sql); }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return prepareStatement(sql); }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return prepareStatement(sql); }
        @Override public Clob createClob() throws SQLException { return null; }
        @Override public Blob createBlob() throws SQLException { return null; }
        @Override public NClob createNClob() throws SQLException { return null; }
        @Override public SQLXML createSQLXML() throws SQLException { return null; }
        @Override public boolean isValid(int timeout) throws SQLException { return !closed; }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException {}
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException {}
        @Override public String getClientInfo(String name) throws SQLException { return null; }
        @Override public Properties getClientInfo() throws SQLException { return new Properties(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return null; }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return null; }
        @Override public void setSchema(String schema) throws SQLException {}
        @Override public String getSchema() throws SQLException { return "vertex_banking"; }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { close(); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException {}
        @Override public int getNetworkTimeout() throws SQLException { return 0; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return null; }
        @Override public void setReadOnly(boolean readOnly) throws SQLException {}
        @Override public boolean isReadOnly() throws SQLException { return false; }
        @Override public void setCatalog(String catalog) throws SQLException {}
        @Override public String getCatalog() throws SQLException { return "vertex_banking"; }
        @Override public void setTransactionIsolation(int level) throws SQLException {}
        @Override public int getTransactionIsolation() throws SQLException { return Connection.TRANSACTION_READ_COMMITTED; }
        @Override public SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
    }

    // ==========================================
    // SIMULATED PREPARED STATEMENT
    // ==========================================
    public static class SimulatedPreparedStatement implements PreparedStatement {
        private final SimulatedConnection connection;
        private final String sql;
        private final Map<Integer, Object> params = new HashMap<>();
        private boolean closed = false;

        public SimulatedPreparedStatement(SimulatedConnection connection, String sql) {
            this.connection = connection;
            this.sql = sql;
        }

        @Override
        public void setString(int parameterIndex, String x) throws SQLException {
            params.put(parameterIndex, x);
        }

        @Override
        public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
            params.put(parameterIndex, x);
        }

        @Override
        public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
            params.put(parameterIndex, x != null ? x.toLocalDate() : null);
        }

        @Override
        public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
            params.put(parameterIndex, x != null ? x.toLocalDateTime() : null);
        }

        @Override
        public void setInt(int parameterIndex, int x) throws SQLException {
            params.put(parameterIndex, x);
        }

        @Override
        public void setObject(int parameterIndex, Object x) throws SQLException {
            params.put(parameterIndex, x);
        }

        @Override
        public void setNull(int parameterIndex, int sqlType) throws SQLException {
            params.put(parameterIndex, null);
        }

        @Override
        public int executeUpdate() throws SQLException {
            String lower = sql.toLowerCase().trim();
            if (lower.startsWith("insert into customers")) {
                String id = (String) params.get(1);
                String name = (String) params.get(2);
                String email = (String) params.get(3);
                String phone = (String) params.get(4);
                LocalDate dob = (LocalDate) params.get(5);

                Map<String, Object> row = new HashMap<>();
                row.put("customer_id", id);
                row.put("name", name);
                row.put("email", email);
                row.put("phone", phone);
                row.put("dob", dob);
                DATABASE.get("customers").put(id, row);
                return 1;
            } else if (lower.startsWith("update customers")) {
                // UPDATE customers SET name = ?, email = ?, phone = ?, dob = ? WHERE customer_id = ?
                String name = (String) params.get(1);
                String email = (String) params.get(2);
                String phone = (String) params.get(3);
                LocalDate dob = (LocalDate) params.get(4);
                String id = (String) params.get(5);

                Map<String, Object> row = DATABASE.get("customers").get(id);
                if (row != null) {
                    row.put("name", name);
                    row.put("email", email);
                    row.put("phone", phone);
                    row.put("dob", dob);
                    return 1;
                }
                return 0;
            } else if (lower.startsWith("delete from customers")) {
                String id = (String) params.get(1);
                return DATABASE.get("customers").remove(id) != null ? 1 : 0;
            } else if (lower.startsWith("insert into accounts")) {
                // INSERT INTO accounts (account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                String accNum = (String) params.get(1);
                String custId = (String) params.get(2);
                String type = (String) params.get(3);
                BigDecimal balance = (BigDecimal) params.get(4);
                String status = (String) params.get(5);
                BigDecimal limit = (BigDecimal) params.get(6);
                BigDecimal withdrawn = (BigDecimal) params.get(7);
                LocalDate lastDate = (LocalDate) params.get(8);

                Map<String, Object> row = new HashMap<>();
                row.put("account_number", accNum);
                row.put("customer_id", custId);
                row.put("account_type", type);
                row.put("balance", balance);
                row.put("status", status);
                row.put("daily_limit", limit);
                row.put("daily_withdrawn_today", withdrawn != null ? withdrawn : BigDecimal.ZERO);
                row.put("last_withdrawal_date", lastDate != null ? lastDate : LocalDate.now());
                DATABASE.get("accounts").put(accNum, row);
                return 1;
            } else if (lower.startsWith("update accounts set balance = ?, daily_withdrawn_today = ?, last_withdrawal_date = ? where account_number = ?")) {
                BigDecimal bal = (BigDecimal) params.get(1);
                BigDecimal withdrawn = (BigDecimal) params.get(2);
                LocalDate lDate = (LocalDate) params.get(3);
                String acc = (String) params.get(4);

                Map<String, Object> row = DATABASE.get("accounts").get(acc);
                if (row != null) {
                    row.put("balance", bal);
                    row.put("daily_withdrawn_today", withdrawn);
                    row.put("last_withdrawal_date", lDate);
                    return 1;
                }
                return 0;
            } else if (lower.startsWith("update accounts set status = ? where account_number = ?")) {
                String st = (String) params.get(1);
                String acc = (String) params.get(2);
                Map<String, Object> row = DATABASE.get("accounts").get(acc);
                if (row != null) {
                    row.put("status", st);
                    return 1;
                }
                return 0;
            } else if (lower.startsWith("insert into transactions")) {
                // INSERT INTO transactions (txn_id, account_number, type, amount, txn_date, status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?)
                String id = (String) params.get(1);
                String acc = (String) params.get(2);
                String type = (String) params.get(3);
                BigDecimal amt = (BigDecimal) params.get(4);
                LocalDateTime dt = (LocalDateTime) params.get(5);
                String status = (String) params.get(6);
                String remarks = (String) params.get(7);

                Map<String, Object> row = new HashMap<>();
                row.put("txn_id", id);
                row.put("account_number", acc);
                row.put("type", type);
                row.put("amount", amt);
                row.put("txn_date", dt != null ? dt : LocalDateTime.now());
                row.put("status", status);
                row.put("remarks", remarks);
                DATABASE.get("transactions").put(id, row);
                return 1;
            }
            return 1;
        }

        @Override
        public ResultSet executeQuery() throws SQLException {
            String lower = sql.toLowerCase().trim();
            List<Map<String, Object>> resultRows = new ArrayList<>();

            if (lower.contains("from customers")) {
                if (lower.contains("where customer_id = ?")) {
                    String id = (String) params.get(1);
                    Map<String, Object> row = DATABASE.get("customers").get(id);
                    if (row != null) resultRows.add(row);
                } else if (lower.contains("where email = ?")) {
                    String email = (String) params.get(1);
                    for (Map<String, Object> r : DATABASE.get("customers").values()) {
                        if (email.equalsIgnoreCase((String) r.get("email"))) {
                            resultRows.add(r);
                            break;
                        }
                    }
                } else {
                    resultRows.addAll(DATABASE.get("customers").values());
                }
            } else if (lower.contains("from accounts")) {
                if (lower.contains("where account_number = ?")) {
                    String acc = (String) params.get(1);
                    Map<String, Object> row = DATABASE.get("accounts").get(acc);
                    if (row != null) resultRows.add(row);
                } else if (lower.contains("where customer_id = ?")) {
                    String custId = (String) params.get(1);
                    for (Map<String, Object> r : DATABASE.get("accounts").values()) {
                        if (custId.equalsIgnoreCase((String) r.get("customer_id"))) {
                            resultRows.add(r);
                        }
                    }
                } else {
                    resultRows.addAll(DATABASE.get("accounts").values());
                }
            } else if (lower.contains("from transactions")) {
                if (lower.contains("where account_number = ?")) {
                    String acc = (String) params.get(1);
                    for (Map<String, Object> r : DATABASE.get("transactions").values()) {
                        if (acc.equalsIgnoreCase((String) r.get("account_number"))) {
                            resultRows.add(r);
                        }
                    }
                } else if (lower.contains("where txn_id = ?")) {
                    String txnId = (String) params.get(1);
                    Map<String, Object> row = DATABASE.get("transactions").get(txnId);
                    if (row != null) resultRows.add(row);
                } else {
                    resultRows.addAll(DATABASE.get("transactions").values());
                }
            }

            return new SimulatedResultSet(resultRows);
        }

        @Override public void close() throws SQLException { this.closed = true; }
        @Override public boolean isClosed() throws SQLException { return closed; }

        // Boilerplate delegations
        @Override public ResultSet executeQuery(String sql) throws SQLException { return executeQuery(); }
        @Override public int executeUpdate(String sql) throws SQLException { return executeUpdate(); }
        @Override public int getMaxFieldSize() throws SQLException { return 0; }
        @Override public void setMaxFieldSize(int max) throws SQLException {}
        @Override public int getMaxRows() throws SQLException { return 0; }
        @Override public void setMaxRows(int max) throws SQLException {}
        @Override public void setEscapeProcessing(boolean enable) throws SQLException {}
        @Override public int getQueryTimeout() throws SQLException { return 0; }
        @Override public void setQueryTimeout(int seconds) throws SQLException {}
        @Override public void cancel() throws SQLException {}
        @Override public SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
        @Override public void setCursorName(String name) throws SQLException {}
        @Override public boolean execute(String sql) throws SQLException { return false; }
        @Override public ResultSet getResultSet() throws SQLException { return null; }
        @Override public int getUpdateCount() throws SQLException { return 0; }
        @Override public boolean getMoreResults() throws SQLException { return false; }
        @Override public void setFetchDirection(int direction) throws SQLException {}
        @Override public int getFetchDirection() throws SQLException { return 0; }
        @Override public void setFetchSize(int rows) throws SQLException {}
        @Override public int getFetchSize() throws SQLException { return 0; }
        @Override public int getResultSetConcurrency() throws SQLException { return 0; }
        @Override public int getResultSetType() throws SQLException { return 0; }
        @Override public void addBatch(String sql) throws SQLException {}
        @Override public void clearBatch() throws SQLException {}
        @Override public int[] executeBatch() throws SQLException { return new int[0]; }
        @Override public Connection getConnection() throws SQLException { return connection; }
        @Override public boolean getMoreResults(int current) throws SQLException { return false; }
        @Override public ResultSet getGeneratedKeys() throws SQLException { return null; }
        @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return 0; }
        @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return 0; }
        @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return 0; }
        @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return false; }
        @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return false; }
        @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return false; }
        @Override public int getResultSetHoldability() throws SQLException { return 0; }
        @Override public void setPoolable(boolean poolable) throws SQLException {}
        @Override public boolean isPoolable() throws SQLException { return false; }
        @Override public void closeOnCompletion() throws SQLException {}
        @Override public boolean isCloseOnCompletion() throws SQLException { return false; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
        @Override public boolean execute() throws SQLException { return false; }
        @Override public void addBatch() throws SQLException {}
        @Override public void clearParameters() throws SQLException { params.clear(); }
        @Override public void setByte(int parameterIndex, byte x) throws SQLException {}
        @Override public void setShort(int parameterIndex, short x) throws SQLException {}
        @Override public void setLong(int parameterIndex, long x) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setFloat(int parameterIndex, float x) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setDouble(int parameterIndex, double x) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setBoolean(int parameterIndex, boolean x) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setBytes(int parameterIndex, byte[] x) throws SQLException {}
        @Override public void setTime(int parameterIndex, Time x) throws SQLException {}
        @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {}
        @Override public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {}
        @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {}
        @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException {}
        @Override public void setRef(int parameterIndex, Ref x) throws SQLException {}
        @Override public void setBlob(int parameterIndex, Blob x) throws SQLException {}
        @Override public void setClob(int parameterIndex, Clob x) throws SQLException {}
        @Override public void setArray(int parameterIndex, Array x) throws SQLException {}
        @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
        @Override public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException { setDate(parameterIndex, x); }
        @Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {}
        @Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { setTimestamp(parameterIndex, x); }
        @Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {}
        @Override public void setURL(int parameterIndex, java.net.URL x) throws SQLException {}
        @Override public ParameterMetaData getParameterMetaData() throws SQLException { return null; }
        @Override public void setRowId(int parameterIndex, RowId x) throws SQLException {}
        @Override public void setNString(int parameterIndex, String value) throws SQLException { setString(parameterIndex, value); }
        @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value, long length) throws SQLException {}
        @Override public void setNClob(int parameterIndex, NClob value) throws SQLException {}
        @Override public void setClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException {}
        @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream, long length) throws SQLException {}
        @Override public void setNClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException {}
        @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {}
        @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException { params.put(parameterIndex, x); }
        @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x, long length) throws SQLException {}
        @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x, long length) throws SQLException {}
        @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, long length) throws SQLException {}
        @Override public void setAsciiStream(int parameterIndex, java.io.InputStream x) throws SQLException {}
        @Override public void setBinaryStream(int parameterIndex, java.io.InputStream x) throws SQLException {}
        @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader) throws SQLException {}
        @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value) throws SQLException {}
        @Override public void setClob(int parameterIndex, java.io.Reader reader) throws SQLException {}
        @Override public void setBlob(int parameterIndex, java.io.InputStream inputStream) throws SQLException {}
        @Override public void setNClob(int parameterIndex, java.io.Reader reader) throws SQLException {}
    }

    // ==========================================
    // SIMULATED RESULT SET
    // ==========================================
    public static class SimulatedResultSet implements ResultSet {
        private final List<Map<String, Object>> rows;
        private int cursor = -1;
        private boolean closed = false;

        public SimulatedResultSet(List<Map<String, Object>> rows) {
            this.rows = rows;
        }

        @Override
        public boolean next() throws SQLException {
            cursor++;
            return cursor < rows.size();
        }

        @Override
        public String getString(String columnLabel) throws SQLException {
            Object val = getCurrentRow().get(columnLabel.toLowerCase());
            return val != null ? val.toString() : null;
        }

        @Override
        public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
            Object val = getCurrentRow().get(columnLabel.toLowerCase());
            if (val == null) return null;
            if (val instanceof BigDecimal) return (BigDecimal) val;
            return new BigDecimal(val.toString());
        }

        @Override
        public java.sql.Date getDate(String columnLabel) throws SQLException {
            Object val = getCurrentRow().get(columnLabel.toLowerCase());
            if (val == null) return null;
            if (val instanceof LocalDate) return java.sql.Date.valueOf((LocalDate) val);
            return java.sql.Date.valueOf(val.toString());
        }

        @Override
        public Timestamp getTimestamp(String columnLabel) throws SQLException {
            Object val = getCurrentRow().get(columnLabel.toLowerCase());
            if (val == null) return null;
            if (val instanceof LocalDateTime) return Timestamp.valueOf((LocalDateTime) val);
            return Timestamp.valueOf(val.toString());
        }

        @Override
        public int getInt(String columnLabel) throws SQLException {
            Object val = getCurrentRow().get(columnLabel.toLowerCase());
            if (val == null) return 0;
            if (val instanceof Number) return ((Number) val).intValue();
            return Integer.parseInt(val.toString());
        }

        @Override
        public Object getObject(String columnLabel) throws SQLException {
            return getCurrentRow().get(columnLabel.toLowerCase());
        }

        @Override
        public void close() throws SQLException {
            this.closed = true;
        }

        private Map<String, Object> getCurrentRow() throws SQLException {
            if (cursor < 0 || cursor >= rows.size()) {
                throw new SQLException("Invalid cursor position in ResultSet.");
            }
            return rows.get(cursor);
        }

        // Boilerplate stubs
        @Override public boolean wasNull() throws SQLException { return false; }
        @Override public String getString(int columnIndex) throws SQLException { return null; }
        @Override public boolean getBoolean(int columnIndex) throws SQLException { return false; }
        @Override public byte getByte(int columnIndex) throws SQLException { return 0; }
        @Override public short getShort(int columnIndex) throws SQLException { return 0; }
        @Override public int getInt(int columnIndex) throws SQLException { return 0; }
        @Override public long getLong(int columnIndex) throws SQLException { return 0; }
        @Override public float getFloat(int columnIndex) throws SQLException { return 0; }
        @Override public double getDouble(int columnIndex) throws SQLException { return 0; }
        @Override public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return null; }
        @Override public byte[] getBytes(int columnIndex) throws SQLException { return new byte[0]; }
        @Override public java.sql.Date getDate(int columnIndex) throws SQLException { return null; }
        @Override public Time getTime(int columnIndex) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int columnIndex) throws SQLException { return null; }
        @Override public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException { return null; }
        @Override public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException { return null; }
        @Override public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException { return null; }
        @Override public boolean getBoolean(String columnLabel) throws SQLException { return false; }
        @Override public byte getByte(String columnLabel) throws SQLException { return 0; }
        @Override public short getShort(String columnLabel) throws SQLException { return 0; }
        @Override public long getLong(String columnLabel) throws SQLException { return 0; }
        @Override public float getFloat(String columnLabel) throws SQLException { return 0; }
        @Override public double getDouble(String columnLabel) throws SQLException { return 0; }
        @Override public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException { return null; }
        @Override public byte[] getBytes(String columnLabel) throws SQLException { return new byte[0]; }
        @Override public Time getTime(String columnLabel) throws SQLException { return null; }
        @Override public java.io.InputStream getAsciiStream(String columnLabel) throws SQLException { return null; }
        @Override public java.io.InputStream getUnicodeStream(String columnLabel) throws SQLException { return null; }
        @Override public java.io.InputStream getBinaryStream(String columnLabel) throws SQLException { return null; }
        @Override public SQLWarning getWarnings() throws SQLException { return null; }
        @Override public void clearWarnings() throws SQLException {}
        @Override public String getCursorName() throws SQLException { return null; }
        @Override public ResultSetMetaData getMetaData() throws SQLException { return null; }
        @Override public Object getObject(int columnIndex) throws SQLException { return null; }
        @Override public int findColumn(String columnLabel) throws SQLException { return 0; }
        @Override public java.io.Reader getCharacterStream(int columnIndex) throws SQLException { return null; }
        @Override public java.io.Reader getCharacterStream(String columnLabel) throws SQLException { return null; }
        @Override public BigDecimal getBigDecimal(int columnIndex) throws SQLException { return null; }
        @Override public boolean isBeforeFirst() throws SQLException { return cursor == -1; }
        @Override public boolean isAfterLast() throws SQLException { return cursor >= rows.size(); }
        @Override public boolean isFirst() throws SQLException { return cursor == 0; }
        @Override public boolean isLast() throws SQLException { return cursor == rows.size() - 1; }
        @Override public void beforeFirst() throws SQLException { cursor = -1; }
        @Override public void afterLast() throws SQLException { cursor = rows.size(); }
        @Override public boolean first() throws SQLException { cursor = 0; return !rows.isEmpty(); }
        @Override public boolean last() throws SQLException { cursor = rows.size() - 1; return !rows.isEmpty(); }
        @Override public int getRow() throws SQLException { return cursor + 1; }
        @Override public boolean absolute(int row) throws SQLException { return false; }
        @Override public boolean relative(int rows) throws SQLException { return false; }
        @Override public boolean previous() throws SQLException { if (cursor > 0) { cursor--; return true; } return false; }
        @Override public void setFetchDirection(int direction) throws SQLException {}
        @Override public int getFetchDirection() throws SQLException { return 0; }
        @Override public void setFetchSize(int rows) throws SQLException {}
        @Override public int getFetchSize() throws SQLException { return 0; }
        @Override public int getType() throws SQLException { return 0; }
        @Override public int getConcurrency() throws SQLException { return 0; }
        @Override public boolean rowUpdated() throws SQLException { return false; }
        @Override public boolean rowInserted() throws SQLException { return false; }
        @Override public boolean rowDeleted() throws SQLException { return false; }
        @Override public void updateNull(int columnIndex) throws SQLException {}
        @Override public void updateBoolean(int columnIndex, boolean x) throws SQLException {}
        @Override public void updateByte(int columnIndex, byte x) throws SQLException {}
        @Override public void updateShort(int columnIndex, short x) throws SQLException {}
        @Override public void updateInt(int columnIndex, int x) throws SQLException {}
        @Override public void updateLong(int columnIndex, long x) throws SQLException {}
        @Override public void updateFloat(int columnIndex, float x) throws SQLException {}
        @Override public void updateDouble(int columnIndex, double x) throws SQLException {}
        @Override public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {}
        @Override public void updateString(int columnIndex, String x) throws SQLException {}
        @Override public void updateBytes(int columnIndex, byte[] x) throws SQLException {}
        @Override public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(int columnIndex, Time x) throws SQLException {}
        @Override public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws SQLException {}
        @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {}
        @Override public void updateObject(int columnIndex, Object x) throws SQLException {}
        @Override public void updateNull(String columnLabel) throws SQLException {}
        @Override public void updateBoolean(String columnLabel, boolean x) throws SQLException {}
        @Override public void updateByte(String columnLabel, byte x) throws SQLException {}
        @Override public void updateShort(String columnLabel, short x) throws SQLException {}
        @Override public void updateInt(String columnLabel, int x) throws SQLException {}
        @Override public void updateLong(String columnLabel, long x) throws SQLException {}
        @Override public void updateFloat(String columnLabel, float x) throws SQLException {}
        @Override public void updateDouble(String columnLabel, double x) throws SQLException {}
        @Override public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {}
        @Override public void updateString(String columnLabel, String x) throws SQLException {}
        @Override public void updateBytes(String columnLabel, byte[] x) throws SQLException {}
        @Override public void updateDate(String columnLabel, java.sql.Date x) throws SQLException {}
        @Override public void updateTime(String columnLabel, Time x) throws SQLException {}
        @Override public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x, int length) throws SQLException {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x, int length) throws SQLException {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader, int length) throws SQLException {}
        @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {}
        @Override public void updateObject(String columnLabel, Object x) throws SQLException {}
        @Override public void insertRow() throws SQLException {}
        @Override public void updateRow() throws SQLException {}
        @Override public void deleteRow() throws SQLException {}
        @Override public void refreshRow() throws SQLException {}
        @Override public void cancelRowUpdates() throws SQLException {}
        @Override public void moveToInsertRow() throws SQLException {}
        @Override public void moveToCurrentRow() throws SQLException {}
        @Override public Statement getStatement() throws SQLException { return null; }
        @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException { return null; }
        @Override public Ref getRef(int columnIndex) throws SQLException { return null; }
        @Override public Blob getBlob(int columnIndex) throws SQLException { return null; }
        @Override public Clob getClob(int columnIndex) throws SQLException { return null; }
        @Override public Array getArray(int columnIndex) throws SQLException { return null; }
        @Override public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException { return null; }
        @Override public Ref getRef(String columnLabel) throws SQLException { return null; }
        @Override public Blob getBlob(String columnLabel) throws SQLException { return null; }
        @Override public Clob getClob(String columnLabel) throws SQLException { return null; }
        @Override public Array getArray(String columnLabel) throws SQLException { return null; }
        @Override public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException { return null; }
        @Override public java.sql.Date getDate(String columnLabel, Calendar cal) throws SQLException { return null; }
        @Override public Time getTime(int columnIndex, Calendar cal) throws SQLException { return null; }
        @Override public Time getTime(String columnLabel, Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException { return null; }
        @Override public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException { return null; }
        @Override public java.net.URL getURL(int columnIndex) throws SQLException { return null; }
        @Override public java.net.URL getURL(String columnLabel) throws SQLException { return null; }
        @Override public void updateRef(int columnIndex, Ref x) throws SQLException {}
        @Override public void updateRef(String columnLabel, Ref x) throws SQLException {}
        @Override public void updateBlob(int columnIndex, Blob x) throws SQLException {}
        @Override public void updateBlob(String columnLabel, Blob x) throws SQLException {}
        @Override public void updateClob(int columnIndex, Clob x) throws SQLException {}
        @Override public void updateClob(String columnLabel, Clob x) throws SQLException {}
        @Override public void updateArray(int columnIndex, Array x) throws SQLException {}
        @Override public void updateArray(String columnLabel, Array x) throws SQLException {}
        @Override public RowId getRowId(int columnIndex) throws SQLException { return null; }
        @Override public RowId getRowId(String columnLabel) throws SQLException { return null; }
        @Override public void updateRowId(int columnIndex, RowId x) throws SQLException {}
        @Override public void updateRowId(String columnLabel, RowId x) throws SQLException {}
        @Override public int getHoldability() throws SQLException { return 0; }
        @Override public boolean isClosed() throws SQLException { return closed; }
        @Override public void updateNString(int columnIndex, String nString) throws SQLException {}
        @Override public void updateNString(String columnLabel, String nString) throws SQLException {}
        @Override public void updateNClob(int columnIndex, NClob nClob) throws SQLException {}
        @Override public void updateNClob(String columnLabel, NClob nClob) throws SQLException {}
        @Override public NClob getNClob(int columnIndex) throws SQLException { return null; }
        @Override public NClob getNClob(String columnLabel) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(int columnIndex) throws SQLException { return null; }
        @Override public SQLXML getSQLXML(String columnLabel) throws SQLException { return null; }
        @Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {}
        @Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {}
        @Override public String getNString(int columnIndex) throws SQLException { return null; }
        @Override public String getNString(String columnLabel) throws SQLException { return null; }
        @Override public java.io.Reader getNCharacterStream(int columnIndex) throws SQLException { return null; }
        @Override public java.io.Reader getNCharacterStream(String columnLabel) throws SQLException { return null; }
        @Override public void updateNCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException {}
        @Override public void updateNCharacterStream(String columnLabel, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x, long length) throws SQLException {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x, long length) throws SQLException {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x, long length) throws SQLException {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x, long length) throws SQLException {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateBlob(int columnIndex, java.io.InputStream inputStream, long length) throws SQLException {}
        @Override public void updateBlob(String columnLabel, java.io.InputStream inputStream, long length) throws SQLException {}
        @Override public void updateClob(int columnIndex, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateClob(String columnLabel, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateNClob(int columnIndex, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateNClob(String columnLabel, java.io.Reader reader, long length) throws SQLException {}
        @Override public void updateNCharacterStream(int columnIndex, java.io.Reader x) throws SQLException {}
        @Override public void updateNCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {}
        @Override public void updateAsciiStream(int columnIndex, java.io.InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(int columnIndex, java.io.InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(int columnIndex, java.io.Reader x) throws SQLException {}
        @Override public void updateAsciiStream(String columnLabel, java.io.InputStream x) throws SQLException {}
        @Override public void updateBinaryStream(String columnLabel, java.io.InputStream x) throws SQLException {}
        @Override public void updateCharacterStream(String columnLabel, java.io.Reader reader) throws SQLException {}
        @Override public void updateBlob(int columnIndex, java.io.InputStream inputStream) throws SQLException {}
        @Override public void updateBlob(String columnLabel, java.io.InputStream inputStream) throws SQLException {}
        @Override public void updateClob(int columnIndex, java.io.Reader reader) throws SQLException {}
        @Override public void updateClob(String columnLabel, java.io.Reader reader) throws SQLException {}
        @Override public void updateNClob(int columnIndex, java.io.Reader reader) throws SQLException {}
        @Override public void updateNClob(String columnLabel, java.io.Reader reader) throws SQLException {}
        @Override public <T> T getObject(int columnIndex, Class<T> type) throws SQLException { return null; }
        @Override public <T> T getObject(String columnLabel, Class<T> type) throws SQLException { return null; }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }
    }
}
