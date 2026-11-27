package service;

import dao.AccountDAO;
import dao.TransactionDAO;
import database.DBConnection;
import exception.DuplicateRecordException;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import exception.VertexException;
import model.*;
import repository.Repository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service layer for Account operations and business rules.
 * Enforces deposit, withdrawal, and status transitions.
 */
public class AccountService {
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final Repository<Account> accountRepo;
    private final Repository<Transaction> transactionRepo;
    private final CustomerService customerService;
    private final AtomicLong txnSequence = new AtomicLong(1000);

    public AccountService(AccountDAO accountDAO, TransactionDAO transactionDAO,
                          Repository<Account> accountRepo, Repository<Transaction> transactionRepo,
                          CustomerService customerService) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
        this.customerService = customerService;
        loadExistingAccounts();
    }

    private void loadExistingAccounts() {
        try {
            List<Account> all = accountDAO.findAll();
            for (Account acc : all) {
                try { accountRepo.save(acc); } catch (Exception ignored) {}
            }
            List<Transaction> txns = transactionDAO.findAll();
            for (Transaction t : txns) {
                try { transactionRepo.save(t); } catch (Exception ignored) {}
            }
        } catch (SQLException ignored) {}
    }

    public synchronized Account openAccount(String accountNumber, String customerId, AccountType accountType, BigDecimal initialDeposit)
            throws VertexException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidAccountException("Account number cannot be empty.");
        }
        String cleanAcc = accountNumber.trim().toUpperCase();

        // 1. Verify customer exists
        Customer customer = customerService.findCustomerById(customerId)
                .orElseThrow(() -> new InvalidAccountException("Customer with ID '" + customerId + "' does not exist."));

        // 2. Check duplicate account number
        if (accountRepo.findById(cleanAcc).isPresent()) {
            throw new DuplicateRecordException("Account number '" + cleanAcc + "' already exists.");
        }
        try {
            if (accountDAO.findByAccountNumber(cleanAcc).isPresent()) {
                throw new DuplicateRecordException("Account number '" + cleanAcc + "' already exists in database.");
            }
        } catch (SQLException e) {
            throw new VertexException("Database error checking account: " + e.getMessage(), e);
        }

        // 3. Create Account (validates initial deposit against AccountType minimum)
        Account account = new Account(cleanAcc, customer.getCustomerId(), accountType, initialDeposit);

        // 4. Save account and opening transaction
        String txnId = "TXN-INIT-" + cleanAcc;
        Transaction initTxn = new Transaction(txnId, cleanAcc, TransactionType.DEPOSIT, initialDeposit,
                TransactionStatus.SUCCESS, "Initial Opening Deposit");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                accountDAO.save(account, conn);
                transactionDAO.save(initTxn, conn);
                conn.commit();

                accountRepo.save(account);
                transactionRepo.save(initTxn);
                return account;
            } catch (SQLException ex) {
                conn.rollback();
                throw new VertexException("Transaction rolled back. Failed to open account: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new VertexException("Database connection error: " + e.getMessage(), e);
        }
    }

    public synchronized Transaction deposit(String accountNumber, BigDecimal amount, String remarks)
            throws VertexException {
        Account account = getAccount(accountNumber)
                .orElseThrow(() -> new InvalidAccountException("Account '" + accountNumber + "' not found."));

        account.deposit(amount);
        String txnId = "TXN-" + System.currentTimeMillis() + "-" + txnSequence.incrementAndGet();
        Transaction txn = new Transaction(txnId, account.getAccountNumber(), TransactionType.DEPOSIT, amount,
                TransactionStatus.SUCCESS, remarks != null ? remarks : "Account Deposit");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                accountDAO.updateBalance(account, conn);
                transactionDAO.save(txn, conn);
                conn.commit();

                accountRepo.update(account);
                transactionRepo.save(txn);
                return txn;
            } catch (SQLException ex) {
                conn.rollback();
                throw new VertexException("Deposit failed and was rolled back: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new VertexException("Database error: " + e.getMessage(), e);
        }
    }

    public synchronized Transaction withdraw(String accountNumber, BigDecimal amount, String remarks)
            throws VertexException {
        Account account = getAccount(accountNumber)
                .orElseThrow(() -> new InvalidAccountException("Account '" + accountNumber + "' not found."));

        // Executes withdrawal checks: status, daily limit, and balance
        account.withdraw(amount);
        String txnId = "TXN-" + System.currentTimeMillis() + "-" + txnSequence.incrementAndGet();
        Transaction txn = new Transaction(txnId, account.getAccountNumber(), TransactionType.WITHDRAWAL, amount,
                TransactionStatus.SUCCESS, remarks != null ? remarks : "Account Withdrawal");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                accountDAO.updateBalance(account, conn);
                transactionDAO.save(txn, conn);
                conn.commit();

                accountRepo.update(account);
                transactionRepo.save(txn);
                return txn;
            } catch (SQLException ex) {
                conn.rollback();
                throw new VertexException("Withdrawal failed and was rolled back: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new VertexException("Database error: " + e.getMessage(), e);
        }
    }

    public synchronized void updateStatus(String accountNumber, AccountStatus newStatus) throws VertexException {
        Account account = getAccount(accountNumber)
                .orElseThrow(() -> new InvalidAccountException("Account '" + accountNumber + "' not found."));

        account.updateStatus(newStatus);
        try {
            accountDAO.updateStatus(accountNumber, newStatus);
            accountRepo.update(account);
        } catch (SQLException e) {
            throw new VertexException("Failed to update account status in database: " + e.getMessage(), e);
        }
    }

    public Optional<Account> getAccount(String accountNumber) {
        if (accountNumber == null) return Optional.empty();
        String cleanAcc = accountNumber.trim().toUpperCase();
        Optional<Account> cached = accountRepo.findById(cleanAcc);
        if (cached.isPresent()) return cached;

        try {
            Optional<Account> dbAcc = accountDAO.findByAccountNumber(cleanAcc);
            dbAcc.ifPresent(a -> {
                try { accountRepo.save(a); } catch (Exception ignored) {}
            });
            return dbAcc;
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public List<Account> getAccountsByCustomer(String customerId) {
        try {
            return accountDAO.findByCustomerId(customerId);
        } catch (SQLException e) {
            return accountRepo.filter(a -> a.getCustomerId().equalsIgnoreCase(customerId));
        }
    }

    public List<Account> getAllAccounts() {
        try {
            List<Account> list = accountDAO.findAll();
            if (!list.isEmpty()) return list;
        } catch (SQLException ignored) {}
        return accountRepo.findAll();
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        try {
            return transactionDAO.findByAccountNumber(accountNumber);
        } catch (SQLException e) {
            return transactionRepo.filter(t -> t.getAccountNumber().equalsIgnoreCase(accountNumber));
        }
    }

    public List<Transaction> getAllTransactions() {
        try {
            return transactionDAO.findAll();
        } catch (SQLException e) {
            return transactionRepo.findAll();
        }
    }

    /**
     * Milestone 2: Demonstrates sorting accounts using Comparator.
     */
    public List<Account> getAccountsSortedByBalanceDescending() {
        return getAllAccounts().stream()
                .sorted(Comparator.comparing(Account::getBalance).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Milestone 2: Returns top N accounts by balance.
     */
    public List<Account> getTopAccounts(int limit) {
        return getAllAccounts().stream()
                .sorted(Comparator.comparing(Account::getBalance).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
