package ledger.core;

import java.math.BigDecimal;

/** Whole minor units (fils) in a {@code long}: no floating point, no fraction of a fils. See NUMBERS.md. */
public record Money(long minorUnits, Currency currency) {

    /** Rejects more decimal places than the currency allows. */
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

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    @Override
    public String toString() {
        return currency + " " + BigDecimal.valueOf(minorUnits, currency.decimals()).toPlainString();
    }
}
