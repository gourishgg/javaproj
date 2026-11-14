package dao;

import database.DBConnection;
import exception.InvalidCustomerException;
import model.Customer;
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
 * Data Access Object (DAO) for Customer entity.
 * Demonstrates:
 * - Separation of database access from business logic
 * - PreparedStatement for parameter binding (SQL injection prevention)
 * - ResultSet row mapping to domain objects
 * - try-with-resources for automatic resource closing
 */
public class CustomerDAO {

    public void save(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (customer_id, name, email, phone, dob) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getCustomerId());
            stmt.setString(2, customer.getName());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getPhone());
            stmt.setDate(5, Date.valueOf(customer.getDob()));
            stmt.executeUpdate();
        }
    }

    public Optional<Customer> findById(String customerId) throws SQLException {
        String sql = "SELECT customer_id, name, email, phone, dob FROM customers WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> findByEmail(String email) throws SQLException {
        String sql = "SELECT customer_id, name, email, phone, dob FROM customers WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCustomer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT customer_id, name, email, phone, dob FROM customers";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToCustomer(rs));
            }
        }
        return list;
    }

    public boolean update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET name = ?, email = ?, phone = ?, dob = ? WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getName());
            stmt.setString(2, customer.getEmail());
            stmt.setString(3, customer.getPhone());
            stmt.setDate(4, Date.valueOf(customer.getDob()));
            stmt.setString(5, customer.getCustomerId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(String customerId) throws SQLException {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customerId.trim().toUpperCase());
            return stmt.executeUpdate() > 0;
        }
    }

    private Customer mapRowToCustomer(ResultSet rs) throws SQLException {
        try {
            String id = rs.getString("customer_id");
            String name = rs.getString("name");
            String email = rs.getString("email");
            String phone = rs.getString("phone");
            Date date = rs.getDate("dob");
            LocalDate dob = date != null ? date.toLocalDate() : LocalDate.of(2000, 1, 1);
            return new Customer(id, name, email, phone, dob);
        } catch (InvalidCustomerException e) {
            throw new SQLException("Corrupt customer database record: " + e.getMessage(), e);
        }
    }
}
