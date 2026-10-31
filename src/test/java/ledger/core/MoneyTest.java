package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void parsesAnAmountIntoWholeMinorUnits() {
        assertEquals(120000, Money.of("1200.00", AED).minorUnits());
        assertEquals(10000, Money.of("10.000", BHD).minorUnits());
    }

    @Test
    void refusesMoreDecimalPlacesThanTheCurrencyHas() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("3.334", AED));
        assertThrows(IllegalArgumentException.class, () -> Money.of("3.3334", BHD));
    }

    @Test
    void printsAtTheCurrencysPrecision() {
        assertEquals("AED 25.00", Money.of("25", AED).toString());
        assertEquals("BHD 3.334", Money.of("3.334", BHD).toString());
    }

    @Test
    void addsAmountsOfTheSameCurrency() {
        assertEquals(Money.of("250.00", AED), Money.of("1200.00", AED).plus(Money.of("-950.00", AED)));
    }

    @Test
    void refusesToAddDifferentCurrencies() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1.00", AED).plus(Money.of("1.000", BHD)));
    }

    @Test
    void failsLoudlyInsteadOfOverflowing() {
        Money max = new Money(Long.MAX_VALUE, AED);
        assertThrows(ArithmeticException.class, () -> max.plus(Money.of("0.01", AED)));
    }

    @Test
    void splitsIntoPartsThatAddUpExactly_theLastAbsorbingTheRemainder() {
        assertEquals(List.of(Money.of("3.333", BHD), Money.of("3.333", BHD), Money.of("3.334", BHD)),
                Money.of("10.000", BHD).split(3));
    }

    @Test
    void splitsEvenlyWhenThereIsNoRemainder() {
        assertEquals(List.of(Money.of("3.000", BHD), Money.of("3.000", BHD), Money.of("3.000", BHD)),
                Money.of("9.000", BHD).split(3));
        assertEquals(List.of(Money.of("10.000", BHD)), Money.of("10.000", BHD).split(1));
    }
}
