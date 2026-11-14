package model;

public enum TransactionType {
    DEPOSIT("Deposit to account"),
    WITHDRAWAL("Withdrawal from account"),
    TRANSFER_IN("Funds transferred in"),
    TRANSFER_OUT("Funds transferred out");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
