package concurrency;

import model.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Callable task simulating asynchronous batch settlement calculation.
 * Demonstrates:
 * - Milestone 4: Callable interface returning a value
 * - Future.get() for retrieving concurrent task computation results
 */
public class SettlementCalculatorTask implements Callable<BigDecimal> {
    private final String batchName;
    private final List<Account> accounts;

    public SettlementCalculatorTask(String batchName, List<Account> accounts) {
        this.batchName = batchName;
        this.accounts = accounts;
    }

    @Override
    public BigDecimal call() throws Exception {
        System.out.printf("   [%s] Worker calculating batch settlement across %d accounts...%n",
                Thread.currentThread().getName(), accounts.size());
        BigDecimal total = BigDecimal.ZERO;
        for (Account a : accounts) {
            total = total.add(a.getBalance());
            Thread.sleep(20); // Simulate analytical computation
        }
        System.out.printf("   [%s] Batch [%s] calculated total: ₹%,.2f%n",
                Thread.currentThread().getName(), batchName, total);
        return total;
    }
}
