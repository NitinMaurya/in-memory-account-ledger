package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
