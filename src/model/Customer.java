package model;

import exception.InvalidCustomerException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Customer domain model.
 * Demonstrates encapsulation with private fields, constructor validation,
 * getters/setters, and domain business rules (age >= 18, email format).
 */
public class Customer {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");

    private String customerId;
    private String name;
    private String email;
    private String phone;
    private LocalDate dob;

    public Customer(String customerId, String name, String email, String phone, LocalDate dob)
            throws InvalidCustomerException {
        setCustomerId(customerId);
        setName(name);
        setEmail(email);
        setPhone(phone);
        setDob(dob);
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) throws InvalidCustomerException {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidCustomerException("Customer ID cannot be null or empty.");
        }
        this.customerId = customerId.trim().toUpperCase();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) throws InvalidCustomerException {
        if (name == null || name.trim().length() < 2) {
            throw new InvalidCustomerException("Customer name must be at least 2 characters long.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) throws InvalidCustomerException {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidCustomerException("Email address cannot be empty.");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidCustomerException("Email format is invalid: '" + email + "'. Must be user@example.com");
        }
        this.email = normalized;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) throws InvalidCustomerException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new InvalidCustomerException("Phone number cannot be empty.");
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new InvalidCustomerException("Phone number must contain exactly 10 digits.");
        }
        this.phone = cleanPhone;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) throws InvalidCustomerException {
        if (dob == null) {
            throw new InvalidCustomerException("Date of birth cannot be null.");
        }
        LocalDate today = LocalDate.now();
        if (dob.isAfter(today)) {
            throw new InvalidCustomerException("Date of birth cannot be in the future: " + dob);
        }
        int age = Period.between(dob, today).getYears();
        if (age < 18) {
            throw new InvalidCustomerException("Customer must be at least 18 years old. Calculated age: " + age);
        }
        this.dob = dob;
    }

    public int getAge() {
        return Period.between(dob, LocalDate.now()).getYears();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(customerId, customer.customerId) ||
               Objects.equals(email, customer.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }

    @Override
    public String toString() {
        return String.format("Customer[ID=%s, Name=%s, Email=%s, Phone=%s, DOB=%s, Age=%d]",
                customerId, name, email, phone, dob, getAge());
    }
}
