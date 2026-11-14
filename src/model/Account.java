package model;

import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Account domain model.
 * Demonstrates:
 * - Implementation of Withdrawable interface (Abstraction & Polymorphism)
 * - Strict financial calculations using BigDecimal (Milestone 1)
 * - Daily withdrawal limits and account status lifecycle enforcement (Milestone 1, 2)
 */
public class Account implements Withdrawable {
    private String accountNumber;
    private String customerId;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private BigDecimal dailyLimit;
    private BigDecimal dailyWithdrawnToday;
    private LocalDate lastWithdrawalDate;

    public Account(String accountNumber, String customerId, AccountType accountType, BigDecimal initialDeposit)
            throws InvalidAccountException {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new InvalidAccountException("Account number cannot be empty.");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new InvalidAccountException("Customer ID cannot be empty.");
        }
        if (accountType == null) {
            throw new InvalidAccountException("Account type must be specified.");
        }
        if (initialDeposit == null || initialDeposit.compareTo(accountType.getMinOpeningDeposit()) < 0) {
            throw new InvalidAccountException(String.format(
                    "Initial deposit ₹%.2f is below the minimum required for %s (Min: ₹%.2f).",
                    initialDeposit == null ? BigDecimal.ZERO : initialDeposit,
                    accountType,
                    accountType.getMinOpeningDeposit()));
        }

        this.accountNumber = accountNumber.trim().toUpperCase();
        this.customerId = customerId.trim().toUpperCase();
        this.accountType = accountType;
        this.balance = initialDeposit.setScale(2, RoundingMode.HALF_UP);
        this.status = AccountStatus.ACTIVE;
        this.dailyLimit = accountType.getDefaultDailyLimit().setScale(2, RoundingMode.HALF_UP);
        this.dailyWithdrawnToday = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.lastWithdrawalDate = LocalDate.now();
    }

    // Secondary constructor for persistence restoration
    public Account(String accountNumber, String customerId, AccountType accountType, BigDecimal balance,
                   AccountStatus status, BigDecimal dailyLimit, BigDecimal dailyWithdrawnToday, LocalDate lastWithdrawalDate) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
        this.status = status;
        this.dailyLimit = dailyLimit.setScale(2, RoundingMode.HALF_UP);
        this.dailyWithdrawnToday = dailyWithdrawnToday.setScale(2, RoundingMode.HALF_UP);
        this.lastWithdrawalDate = lastWithdrawalDate != null ? lastWithdrawalDate : LocalDate.now();
    }

    public synchronized void deposit(BigDecimal amount) throws InvalidAccountException {
        if (status != AccountStatus.ACTIVE) {
            throw new InvalidAccountException("Deposit failed: Account " + accountNumber + " is " + status + ".");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountException("Deposit amount must be strictly greater than zero.");
        }
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public synchronized boolean withdraw(BigDecimal amount) throws InsufficientFundsException, InvalidAccountException {
        if (status == AccountStatus.FROZEN) {
            throw new InvalidAccountException("Withdrawal rejected: Account " + accountNumber + " is FROZEN.");
        }
        if (status == AccountStatus.CLOSED) {
            throw new InvalidAccountException("Withdrawal rejected: Account " + accountNumber + " is CLOSED.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountException("Withdrawal amount must be strictly greater than zero.");
        }

        // Daily limit check
        resetDailyWithdrawalLimitIfNeeded();
        BigDecimal newDailyWithdrawn = this.dailyWithdrawnToday.add(amount);
        if (newDailyWithdrawn.compareTo(this.dailyLimit) > 0) {
            BigDecimal remainingLimit = this.dailyLimit.subtract(this.dailyWithdrawnToday);
            throw new InvalidAccountException(String.format(
                    "Daily withdrawal limit exceeded! Limit: ₹%.2f, Already withdrawn today: ₹%.2f, Available limit today: ₹%.2f, Requested: ₹%.2f",
                    this.dailyLimit, this.dailyWithdrawnToday, remainingLimit.max(BigDecimal.ZERO), amount));
        }

        // Balance check
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.balance, amount);
        }

        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        this.dailyWithdrawnToday = newDailyWithdrawn.setScale(2, RoundingMode.HALF_UP);
        return true;
    }

    public synchronized void updateStatus(AccountStatus newStatus) throws InvalidAccountException {
        if (this.status == AccountStatus.CLOSED) {
            throw new InvalidAccountException("Cannot modify status: Account " + accountNumber + " is permanently CLOSED.");
        }
        if (newStatus == AccountStatus.CLOSED && this.balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidAccountException("Cannot close account: Remaining balance is ₹" + this.balance +
                    ". Please withdraw all funds before closing.");
        }
        this.status = newStatus;
    }

    public synchronized void restoreBalance(BigDecimal originalBalance, BigDecimal originalDailyWithdrawn) {
        this.balance = originalBalance.setScale(2, RoundingMode.HALF_UP);
        if (originalDailyWithdrawn != null) {
            this.dailyWithdrawnToday = originalDailyWithdrawn.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public synchronized void resetDailyWithdrawalLimitIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(this.lastWithdrawalDate)) {
            this.dailyWithdrawnToday = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.lastWithdrawalDate = today;
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    @Override
    public synchronized BigDecimal getAvailableBalance() {
        return balance;
    }

    public synchronized AccountStatus getStatus() {
        return status;
    }

    public synchronized BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public synchronized void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit.setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized BigDecimal getDailyWithdrawnToday() {
        resetDailyWithdrawalLimitIfNeeded();
        return dailyWithdrawnToday;
    }

    public synchronized LocalDate getLastWithdrawalDate() {
        return lastWithdrawalDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public synchronized String toString() {
        return String.format("Account[No=%s, CustomerID=%s, Type=%s, Balance=₹%.2f, Status=%s, DailyLimit=₹%.2f]",
                accountNumber, customerId, accountType, balance, status, dailyLimit);
    }
}
