package service;

import model.Account;
import model.AccountStatus;
import model.AccountType;
import model.Transaction;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service generating analytical insights using the Java Stream API and Lambdas.
 * Demonstrates:
 * - Milestone 6: Functional interfaces, intermediate operations (filter, map, sorted, distinct, limit)
 * - Terminal operations: collect, counting, averagingDouble, summarizingDouble, reduce
 * - Collectors.groupingBy() for multi-level aggregation
 */
public class ReportService {
    private final AccountService accountService;

    public ReportService(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Total bank deposit assets across all accounts.
     * Demonstrates: map + reduce intermediate & terminal operations.
     */
    public BigDecimal getTotalBankAssets() {
        return accountService.getAllAccounts().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Distinct account types currently utilized in the bank.
     * Demonstrates: map + distinct.
     */
    public List<String> getDistinctAccountTypesInUse() {
        return accountService.getAllAccounts().stream()
                .map(a -> a.getAccountType().name())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Filter accounts by a specific status.
     * Demonstrates: filter + collect.
     */
    public List<Account> getAccountsByStatus(AccountStatus status) {
        return accountService.getAllAccounts().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Top accounts sorted by balance descending.
     * Demonstrates: sorted with Comparator + limit.
     */
    public List<Account> getTopAccountsByBalance(int limit) {
        return accountService.getAllAccounts().stream()
                .sorted(Comparator.comparing(Account::getBalance).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Account count grouped by status.
     * Demonstrates: Collectors.groupingBy + Collectors.counting.
     */
    public Map<AccountStatus, Long> getAccountCountByStatus() {
        return accountService.getAllAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getStatus,
                        Collectors.counting()
                ));
    }

    /**
     * Account count grouped by account type.
     * Demonstrates: Collectors.groupingBy + Collectors.counting.
     */
    public Map<AccountType, Long> getAccountCountByType() {
        return accountService.getAllAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getAccountType,
                        Collectors.counting()
                ));
    }

    /**
     * Average balance grouped by account type.
     * Demonstrates: Collectors.groupingBy + Collectors.averagingDouble.
     */
    public Map<AccountType, Double> getAverageBalanceByType() {
        return accountService.getAllAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getAccountType,
                        Collectors.averagingDouble(a -> a.getBalance().doubleValue())
                ));
    }

    /**
     * Comprehensive summary statistics by account type (count, sum, min, max, average).
     * Demonstrates: Collectors.groupingBy + Collectors.summarizingDouble.
     */
    public Map<AccountType, DoubleSummaryStatistics> getBalanceStatisticsByType() {
        return accountService.getAllAccounts().stream()
                .collect(Collectors.groupingBy(
                        Account::getAccountType,
                        Collectors.summarizingDouble(a -> a.getBalance().doubleValue())
                ));
    }

    /**
     * Formats a comprehensive textual report using StringBuilder.
     */
    public String generateSystemReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                  VERTEX BANKING - ANALYTICS & REPORTS                 \n");
        sb.append("========================================================================\n\n");

        sb.append(String.format(">> TOTAL BANK ASSETS UNDER MANAGEMENT: ₹%,.2f\n", getTotalBankAssets()));
        sb.append(String.format(">> TOTAL ACTIVE ACCOUNTS: %d\n\n", accountService.getAllAccounts().size()));

        sb.append("------------------------------------------------------------------------\n");
        sb.append("1. ACCOUNT DISTRIBUTION BY STATUS (Collectors.groupingBy + counting):\n");
        getAccountCountByStatus().forEach((status, count) ->
                sb.append(String.format("   - %-10s : %d account(s)\n", status, count)));

        sb.append("\n------------------------------------------------------------------------\n");
        sb.append("2. ACCOUNT METRICS BY TYPE (groupingBy + summarizingDouble):\n");
        getBalanceStatisticsByType().forEach((type, stats) -> {
            sb.append(String.format("   [%s]\n", type));
            sb.append(String.format("     Total Accounts : %d\n", stats.getCount()));
            sb.append(String.format("     Total Balance  : ₹%,.2f\n", stats.getSum()));
            sb.append(String.format("     Average Balance: ₹%,.2f\n", stats.getAverage()));
            sb.append(String.format("     Min Balance    : ₹%,.2f\n", stats.getMin()));
            sb.append(String.format("     Max Balance    : ₹%,.2f\n", stats.getMax()));
        });

        sb.append("\n------------------------------------------------------------------------\n");
        sb.append("3. TOP 5 HIGH-VALUE ACCOUNTS (sorted + limit):\n");
        List<Account> top5 = getTopAccountsByBalance(5);
        if (top5.isEmpty()) {
            sb.append("   (No accounts available)\n");
        } else {
            for (int i = 0; i < top5.size(); i++) {
                Account a = top5.get(i);
                sb.append(String.format("   %d. Acc: %-8s | CustID: %-8s | Type: %-8s | Balance: ₹%,10.2f\n",
                        i + 1, a.getAccountNumber(), a.getCustomerId(), a.getAccountType(), a.getBalance()));
            }
        }

        sb.append("\n------------------------------------------------------------------------\n");
        sb.append("4. DISTINCT ACCOUNT TYPES IN ACTIVE USE (map + distinct):\n   ");
        sb.append(String.join(", ", getDistinctAccountTypesInUse())).append("\n");
        sb.append("========================================================================\n");

        return sb.toString();
    }
}
