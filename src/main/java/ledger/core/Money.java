package ledger.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    /** Refuses to add different currencies: AED and BHD are never mixed. */
    public Money plus(Money other) {
        if (other.currency != currency) {
            throw new IllegalArgumentException("cannot add " + other.currency + " to " + currency);
        }
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    /** Equal floored shares; the last part absorbs the remainder, so the parts always add up exactly. */
    public List<Money> split(int parts) {
        long share = minorUnits / parts;
        long remainder = minorUnits % parts;
        List<Money> split = new ArrayList<>();
        for (int i = 1; i <= parts; i++) {
            split.add(new Money(i == parts ? share + remainder : share, currency));
        }
        return split;
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), currency);
    }

    @Override
    public String toString() {
        return currency + " " + BigDecimal.valueOf(minorUnits, currency.decimals()).toPlainString();
    }
}
