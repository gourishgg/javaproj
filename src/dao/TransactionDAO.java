package dao;

import database.DBConnection;
import model.Transaction;
import model.TransactionStatus;
import model.TransactionType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Transaction audit records.
 */
public class TransactionDAO {

    public void save(Transaction txn, Connection conn) throws SQLException {
        String sql = "INSERT INTO transactions (txn_id, account_number, type, amount, txn_date, status, remarks) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        boolean closeConn = false;
        Connection activeConn = conn;
        if (activeConn == null) {
            activeConn = DBConnection.getConnection();
            closeConn = true;
        }

        try (PreparedStatement stmt = activeConn.prepareStatement(sql)) {
            stmt.setString(1, txn.getTxnId());
            stmt.setString(2, txn.getAccountNumber());
            stmt.setString(3, txn.getType().name());
            stmt.setBigDecimal(4, txn.getAmount());
            stmt.setTimestamp(5, Timestamp.valueOf(txn.getTimestamp()));
            stmt.setString(6, txn.getStatus().name());
            stmt.setString(7, txn.getRemarks());
            stmt.executeUpdate();
        } finally {
            if (closeConn && activeConn != null) {
                activeConn.close();
            }
        }
    }

    public Optional<Transaction> findById(String txnId) throws SQLException {
        String sql = "SELECT txn_id, account_number, type, amount, txn_date, status, remarks FROM transactions WHERE txn_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, txnId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTransaction(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Transaction> findByAccountNumber(String accountNumber) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT txn_id, account_number, type, amount, txn_date, status, remarks FROM transactions WHERE account_number = ? ORDER BY txn_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTransaction(rs));
                }
            }
        }
        return list;
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT txn_id, account_number, type, amount, txn_date, status, remarks FROM transactions ORDER BY txn_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToTransaction(rs));
            }
        }
        return list;
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        String id = rs.getString("txn_id");
        String acc = rs.getString("account_number");
        TransactionType type = TransactionType.valueOf(rs.getString("type"));
        BigDecimal amount = rs.getBigDecimal("amount");
        Timestamp ts = rs.getTimestamp("txn_date");
        LocalDateTime dt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
        TransactionStatus status = TransactionStatus.valueOf(rs.getString("status"));
        String remarks = rs.getString("remarks");

        return new Transaction(id, acc, type, amount, dt, status, remarks);
    }
}
