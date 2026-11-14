package model;

import exception.InsufficientFundsException;
import exception.InvalidAccountException;
import java.math.BigDecimal;

/**
 * Interface demonstrating Java abstraction and contract-based design.
 * Any account or wallet capable of disbursing funds implements Withdrawable.
 */
public interface Withdrawable {
    boolean withdraw(BigDecimal amount) throws InsufficientFundsException, InvalidAccountException;
    BigDecimal getAvailableBalance();
}
