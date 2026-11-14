package model;

/**
 * Account status lifecycle enum.
 * Supported states: ACTIVE, FROZEN, CLOSED.
 */
public enum AccountStatus {
    ACTIVE("Account is active and ready for transactions"),
    FROZEN("Account is temporarily locked. Transactions are blocked."),
    CLOSED("Account is permanently closed. Status cannot be modified.");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
