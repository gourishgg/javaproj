package dao;

import database.DBConnection;
import model.Account;
import model.AccountStatus;
import model.AccountType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Account entity.
 * Supports transactional operations by accepting an existing Connection.
 */
public class AccountDAO {

    public void save(Account account) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, account.getCustomerId());
            stmt.setString(3, account.getAccountType().name());
            stmt.setBigDecimal(4, account.getBalance());
            stmt.setString(5, account.getStatus().name());
            stmt.setBigDecimal(6, account.getDailyLimit());
            stmt.setBigDecimal(7, account.getDailyWithdrawnToday());
            stmt.setDate(8, Date.valueOf(account.getLastWithdrawalDate()));
            stmt.executeUpdate();
        }
    }

    public Optional<Account> findByAccountNumber(String accountNumber) throws SQLException {
        String sql = "SELECT account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date " +
                     "FROM accounts WHERE account_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAccount(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Account> findByCustomerId(String customerId) throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date " +
                     "FROM accounts WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAccount(rs));
                }
            }
        }
        return list;
    }

    public List<Account> findAll() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date FROM accounts";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToAccount(rs));
            }
        }
        return list;
    }

    /**
     * Updates account balance and daily withdrawal tracking.
     * Accepts a Connection to participate in transactions with commit/rollback.
     */
    public boolean updateBalance(Account account, Connection conn) throws SQLException {
        String sql = "UPDATE accounts SET balance = ?, daily_withdrawn_today = ?, last_withdrawal_date = ? WHERE account_number = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, account.getBalance());
            stmt.setBigDecimal(2, account.getDailyWithdrawnToday());
            stmt.setDate(3, Date.valueOf(account.getLastWithdrawalDate()));
            stmt.setString(4, account.getAccountNumber());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(String accountNumber, AccountStatus status) throws SQLException {
        String sql = "UPDATE accounts SET status = ? WHERE account_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, accountNumber.trim().toUpperCase());
            return stmt.executeUpdate() > 0;
        }
    }

    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        String accNum = rs.getString("account_number");
        String custId = rs.getString("customer_id");
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        BigDecimal balance = rs.getBigDecimal("balance");
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        BigDecimal dailyLimit = rs.getBigDecimal("daily_limit");
        BigDecimal dailyWithdrawn = rs.getBigDecimal("daily_withdrawn_today");
        Date date = rs.getDate("last_withdrawal_date");
        LocalDate lastDate = date != null ? date.toLocalDate() : LocalDate.now();

        return new Account(accNum, custId, type, balance, status, dailyLimit, dailyWithdrawn, lastDate);
    }
}
