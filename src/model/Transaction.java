package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable audit record representing one balance movement in Vertex Banking.
 */
public class Transaction {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String txnId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final TransactionStatus status;
    private final String remarks;

    public Transaction(String txnId, String accountNumber, TransactionType type, BigDecimal amount,
                       TransactionStatus status, String remarks) {
        this(txnId, accountNumber, type, amount, LocalDateTime.now(), status, remarks);
    }

    public Transaction(String txnId, String accountNumber, TransactionType type, BigDecimal amount,
                       LocalDateTime timestamp, TransactionStatus status, String remarks) {
        this.txnId = Objects.requireNonNull(txnId, "Transaction ID cannot be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null").setScale(2, RoundingMode.HALF_UP);
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.status = status != null ? status : TransactionStatus.SUCCESS;
        this.remarks = remarks != null ? remarks : "";
    }

    public String getTxnId() {
        return txnId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getRemarks() {
        return remarks;
    }

    @Override
    public String toString() {
        return String.format("[%s] ID:%s | Acc:%s | Type:%-12s | Amt:₹%10.2f | Status:%-8s | Note:%s",
                getFormattedTimestamp(), txnId, accountNumber, type, amount, status, remarks);
    }
}
