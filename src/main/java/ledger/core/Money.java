package ledger.core;

import java.math.BigDecimal;

/**
 * An exact amount of money: a whole number of minor units (fils) in one currency.
 * There is no floating-point arithmetic on money anywhere in the ledger.
 */
public record Money(long minorUnits, Currency currency) {

    /** Parses "1200.00" as 120000 fils of AED. Rejects more decimals than the currency allows. */
    public static Money of(String amount, Currency currency) {
        BigDecimal value = new BigDecimal(amount);
        if (value.scale() > currency.decimals()) {
            throw new IllegalArgumentException(
                    amount + " has more decimal places than " + currency + " allows (" + currency.decimals() + ")");
        }
        long minor = value.movePointRight(currency.decimals()).longValueExact();
        return new Money(minor, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    @Override
    public String toString() {
        return currency + " " + BigDecimal.valueOf(minorUnits, currency.decimals()).toPlainString();
    }
}
