package exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends VertexException {
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String message) {
        super(message);
        this.currentBalance = BigDecimal.ZERO;
        this.requestedAmount = BigDecimal.ZERO;
    }

    public InsufficientFundsException(BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient funds. Current Balance: ₹%.2f, Requested Amount: ₹%.2f",
                currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
