package service;

import dao.AccountDAO;
import dao.TransactionDAO;
import database.DBConnection;
import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import exception.TransactionException;
import exception.VertexException;
import model.*;
import repository.Repository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service responsible for atomic fund transfers between accounts.
 * Demonstrates:
 * - Milestone 5: Database Transactions, ACID properties, commit, and rollback
 * - Failure handling and state restoration
 */
public class TransferService {
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final Repository<Account> accountRepo;
    private final Repository<Transaction> transactionRepo;
    private final AccountService accountService;
    private final AtomicLong transferSequence = new AtomicLong(5000);

    public TransferService(AccountDAO accountDAO, TransactionDAO transactionDAO,
                           Repository<Account> accountRepo, Repository<Transaction> transactionRepo,
                           AccountService accountService) {
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
        this.accountService = accountService;
    }

    public synchronized TransferResult transferFunds(String fromAccountNum, String toAccountNum, BigDecimal amount)
            throws VertexException {
        return transferFundsInternal(fromAccountNum, toAccountNum, amount, false);
    }

    /**
     * Executes transfer with an optional simulated failure midway (after debit)
     * to prove database transaction rollback (Test T14).
     */
    public synchronized TransferResult transferFundsInternal(String fromAccountNum, String toAccountNum,
                                                             BigDecimal amount, boolean simulateFailureAfterDebit)
            throws VertexException {
        if (fromAccountNum == null || toAccountNum == null) {
            throw new InvalidAccountException("Source and destination account numbers must be provided.");
        }
        if (fromAccountNum.trim().equalsIgnoreCase(toAccountNum.trim())) {
            throw new InvalidAccountException("Cannot transfer funds to the exact same account (" + fromAccountNum + ").");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountException("Transfer amount must be strictly greater than zero.");
        }

        Account fromAccount = accountService.getAccount(fromAccountNum)
                .orElseThrow(() -> new InvalidAccountException("Source account '" + fromAccountNum + "' not found."));
        Account toAccount = accountService.getAccount(toAccountNum)
                .orElseThrow(() -> new InvalidAccountException("Destination account '" + toAccountNum + "' not found."));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Transfer blocked: Source account " + fromAccountNum + " is " + fromAccount.getStatus() + ".");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Transfer blocked: Destination account " + toAccountNum + " is " + toAccount.getStatus() + ".");
        }

        // Cache initial balances for in-memory rollback restoration if needed
        BigDecimal fromInitialBalance = fromAccount.getBalance();
        BigDecimal fromInitialDaily = fromAccount.getDailyWithdrawnToday();
        BigDecimal toInitialBalance = toAccount.getBalance();
        BigDecimal toInitialDaily = toAccount.getDailyWithdrawnToday();

        // 1. Perform domain model withdrawal check & balance update
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);

        long refId = transferSequence.incrementAndGet();
        long ts = System.currentTimeMillis();
        String debitTxnId = "TXN-OUT-" + ts + "-" + refId;
        String creditTxnId = "TXN-IN-" + ts + "-" + refId;

        Transaction debitTxn = new Transaction(debitTxnId, fromAccount.getAccountNumber(),
                TransactionType.TRANSFER_OUT, amount, TransactionStatus.SUCCESS,
                "Transfer to Acc " + toAccount.getAccountNumber());

        Transaction creditTxn = new Transaction(creditTxnId, toAccount.getAccountNumber(),
                TransactionType.TRANSFER_IN, amount, TransactionStatus.SUCCESS,
                "Transfer from Acc " + fromAccount.getAccountNumber());

        // 2. Perform Atomic Database Transaction (Milestone 5)
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step A: Update debit on source account
                accountDAO.updateBalance(fromAccount, conn);
                transactionDAO.save(debitTxn, conn);

                // SIMULATED FAILURE POINT (for demonstrating rollback)
                if (simulateFailureAfterDebit) {
                    throw new SQLException("CRITICAL FAILURE: Network link severed or power failure after debit step!");
                }

                // Step B: Update credit on destination account
                accountDAO.updateBalance(toAccount, conn);
                transactionDAO.save(creditTxn, conn);

                // Step C: Commit transaction
                conn.commit();

                // Update in-memory caches upon commit
                accountRepo.update(fromAccount);
                accountRepo.update(toAccount);
                transactionRepo.save(debitTxn);
                transactionRepo.save(creditTxn);

                return new TransferResult(true, "Transfer completed successfully.", fromAccount.getBalance(), toAccount.getBalance(), debitTxnId, creditTxnId);
            } catch (SQLException ex) {
                // Rollback database changes
                conn.rollback();

                // Rollback in-memory balances to preserve consistency
                fromAccount.restoreBalance(fromInitialBalance, fromInitialDaily);
                toAccount.restoreBalance(toInitialBalance, toInitialDaily);
                accountRepo.update(fromAccount);
                accountRepo.update(toAccount);

                throw new TransactionException("TRANSACTION ROLLED BACK: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            fromAccount.restoreBalance(fromInitialBalance, fromInitialDaily);
            toAccount.restoreBalance(toInitialBalance, toInitialDaily);
            accountRepo.update(fromAccount);
            accountRepo.update(toAccount);
            throw new TransactionException("Database connection failure during transfer: " + e.getMessage(), e);
        }
    }

    public static class TransferResult {
        private final boolean success;
        private final String message;
        private final BigDecimal fromBalance;
        private final BigDecimal toBalance;
        private final String debitTxnId;
        private final String creditTxnId;

        public TransferResult(boolean success, String message, BigDecimal fromBalance, BigDecimal toBalance,
                              String debitTxnId, String creditTxnId) {
            this.success = success;
            this.message = message;
            this.fromBalance = fromBalance;
            this.toBalance = toBalance;
            this.debitTxnId = debitTxnId;
            this.creditTxnId = creditTxnId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BigDecimal getFromBalance() { return fromBalance; }
        public BigDecimal getToBalance() { return toBalance; }
        public String getDebitTxnId() { return debitTxnId; }
        public String getCreditTxnId() { return creditTxnId; }
    }
}
