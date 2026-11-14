package service;

import dao.CustomerDAO;
import exception.DuplicateRecordException;
import exception.InvalidCustomerException;
import exception.VertexException;
import model.Customer;
import repository.Repository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Service layer for customer profile management.
 * Demonstrates:
 * - Milestone 1: Input validation and custom exception propagation
 * - Milestone 2: HashSet for O(1) duplicate email prevention and HashMap/Repository lookup
 * - Milestone 5: JDBC DAO persistence coordination
 */
public class CustomerService {
    private final CustomerDAO customerDAO;
    private final Repository<Customer> customerRepo;
    private final Set<String> registeredEmails = new HashSet<>();

    public CustomerService(CustomerDAO customerDAO, Repository<Customer> customerRepo) {
        this.customerDAO = customerDAO;
        this.customerRepo = customerRepo;
        loadExistingEmails();
    }

    private void loadExistingEmails() {
        try {
            List<Customer> all = customerDAO.findAll();
            for (Customer c : all) {
                registeredEmails.add(c.getEmail().toLowerCase());
                try {
                    customerRepo.save(c);
                } catch (Exception ignored) {}
            }
        } catch (SQLException ignored) {}
    }

    public synchronized Customer registerCustomer(String customerId, String name, String email, String phone, LocalDate dob)
            throws VertexException {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidCustomerException("Customer ID cannot be empty.");
        }
        String cleanId = customerId.trim().toUpperCase();

        // 1. Check duplicate customer ID
        if (customerRepo.findById(cleanId).isPresent()) {
            throw new DuplicateRecordException("Customer with ID '" + cleanId + "' already exists.");
        }
        try {
            if (customerDAO.findById(cleanId).isPresent()) {
                throw new DuplicateRecordException("Customer with ID '" + cleanId + "' already exists in database.");
            }
        } catch (SQLException e) {
            throw new VertexException("Database error verifying customer ID: " + e.getMessage(), e);
        }

        // 2. Check duplicate email using HashSet (Milestone 2)
        if (email != null && registeredEmails.contains(email.trim().toLowerCase())) {
            throw new DuplicateRecordException("Email address '" + email.trim().toLowerCase() + "' is already registered.");
        }

        // 3. Create domain object (triggers Milestone 1 OOP & String validations)
        Customer customer = new Customer(cleanId, name, email, phone, dob);

        // 4. Save to Repository (Collections/Generics) and JDBC DAO (Persistence)
        try {
            customerDAO.save(customer);
            customerRepo.save(customer);
            registeredEmails.add(customer.getEmail().toLowerCase());
            return customer;
        } catch (SQLException e) {
            throw new VertexException("Failed to persist customer to database: " + e.getMessage(), e);
        }
    }

    public Optional<Customer> findCustomerById(String customerId) {
        if (customerId == null) return Optional.empty();
        String cleanId = customerId.trim().toUpperCase();
        // Check cache/repo first, then DAO
        Optional<Customer> opt = customerRepo.findById(cleanId);
        if (opt.isPresent()) return opt;

        try {
            Optional<Customer> dbOpt = customerDAO.findById(cleanId);
            dbOpt.ifPresent(c -> {
                try { customerRepo.save(c); } catch (Exception ignored) {}
            });
            return dbOpt;
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<Customer> getAllCustomers() {
        try {
            List<Customer> fromDb = customerDAO.findAll();
            if (!fromDb.isEmpty()) return fromDb;
        } catch (SQLException ignored) {}
        return customerRepo.findAll();
    }

    public synchronized boolean updateCustomer(String customerId, String name, String email, String phone, LocalDate dob)
            throws VertexException {
        Customer existing = findCustomerById(customerId)
                .orElseThrow(() -> new InvalidCustomerException("Customer with ID '" + customerId + "' not found."));

        String oldEmail = existing.getEmail();
        String newEmailNorm = email != null ? email.trim().toLowerCase() : oldEmail;

        if (!oldEmail.equalsIgnoreCase(newEmailNorm) && registeredEmails.contains(newEmailNorm)) {
            throw new DuplicateRecordException("Email address '" + newEmailNorm + "' is already registered to another user.");
        }

        existing.setName(name);
        existing.setEmail(newEmailNorm);
        existing.setPhone(phone);
        existing.setDob(dob);

        try {
            boolean updated = customerDAO.update(existing);
            if (updated) {
                registeredEmails.remove(oldEmail);
                registeredEmails.add(newEmailNorm);
                customerRepo.update(existing);
            }
            return updated;
        } catch (SQLException e) {
            throw new VertexException("Failed to update customer in database: " + e.getMessage(), e);
        }
    }
}
