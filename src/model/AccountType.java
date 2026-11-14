package model;

import java.math.BigDecimal;

/**
 * Account types supported by Vertex Banking.
 * Contains business rules for minimum opening deposit and daily withdrawal limits.
 */
public enum AccountType {
    SAVINGS(new BigDecimal("1000.00"), new BigDecimal("25000.00"), "Savings Account (Min Deposit: ₹1,000)"),
    CURRENT(new BigDecimal("5000.00"), new BigDecimal("100000.00"), "Current Account (Min Deposit: ₹5,000)");

    private final BigDecimal minOpeningDeposit;
    private final BigDecimal defaultDailyLimit;
    private final String description;

    AccountType(BigDecimal minOpeningDeposit, BigDecimal defaultDailyLimit, String description) {
        this.minOpeningDeposit = minOpeningDeposit;
        this.defaultDailyLimit = defaultDailyLimit;
        this.description = description;
    }

    public BigDecimal getMinOpeningDeposit() {
        return minOpeningDeposit;
    }

    public BigDecimal getDefaultDailyLimit() {
        return defaultDailyLimit;
    }

    public String getDescription() {
        return description;
    }
}
