package concurrency;

import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import model.Account;
import java.math.BigDecimal;

/**
 * Runnable task simulating an individual concurrent withdrawal transaction.
 * Demonstrates Thread/Runnable lifecycle and status tracking.
 */
public class TransactionTask implements Runnable {
    private final String taskId;
    private final Account account;
    private final BigDecimal amount;
    private final BalanceManager balanceManager;

    private boolean completed = false;
    private boolean successful = false;
    private String failureReason = null;

    public TransactionTask(String taskId, Account account, BigDecimal amount, BalanceManager balanceManager) {
        this.taskId = taskId;
        this.account = account;
        this.amount = amount;
        this.balanceManager = balanceManager;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        try {
            successful = balanceManager.processWithdrawal(account, amount, taskId);
        } catch (InsufficientFundsException | InvalidAccountException ex) {
            successful = false;
            failureReason = ex.getMessage();
            System.out.printf("   [%s] %s REJECTED: %s%n", threadName, taskId, ex.getMessage());
        } catch (Exception ex) {
            successful = false;
            failureReason = "Unexpected error: " + ex.getMessage();
            System.out.printf("   [%s] %s ERROR: %s%n", threadName, taskId, ex.getMessage());
        } finally {
            completed = true;
        }
    }

    public String getTaskId() {
        return taskId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
