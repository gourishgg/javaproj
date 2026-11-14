package concurrency;

import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import model.Account;
import java.math.BigDecimal;

/**
 * Thread-safe balance manager protecting shared account balance state.
 * Demonstrates:
 * - Milestone 3: Multithreading and Synchronization
 * - Intrinsic locks and synchronized critical section
 * - Atomic check-and-debit preventing race conditions and negative balances
 */
public class BalanceManager {

    /**
     * Atomically checks balance and debits the requested amount.
     * The critical section is synchronized on the account object's intrinsic monitor lock,
     * ensuring that competing concurrent threads execute one by one without interleaving.
     */
    public boolean processWithdrawal(Account account, BigDecimal amount, String taskName)
            throws InsufficientFundsException, InvalidAccountException {
        if (account == null) {
            throw new InvalidAccountException("Account cannot be null.");
        }

        synchronized (account) {
            String threadName = Thread.currentThread().getName();
            System.out.printf("   [%s] %s entered critical section. Current balance: ₹%.2f, Requested: ₹%.2f%n",
                    threadName, taskName, account.getBalance(), amount);

            // Simulate microscopic processing latency to provoke race condition if unsynchronized
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Safe atomic withdrawal
            boolean result = account.withdraw(amount);

            System.out.printf("   [%s] %s SUCCESSFUL withdrawal! New balance: ₹%.2f%n",
                    threadName, taskName, account.getBalance());
            return result;
        }
    }
}
