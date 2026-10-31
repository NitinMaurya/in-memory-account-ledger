package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BR-8 with hand-worked numbers. A debit booked on Day 5 but value-dated Day 2:
 * closing balances are Day 1: 250.00, Day 2: -370.00, Day 3 onwards: 30.00.
 */
class ClosingBalanceTest {

    private Ledger ledger;

    @BeforeEach
    void bookTheExample() {
        ledger = new Ledger(List.of(
                new Account("ACC-001", AED), new Account("ACC-002", BHD), new Account("ACC-003", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", Money.of("1200.00", AED), 1, 1));
        ledger.apply(new Event.Debit("D1", "ACC-001", Money.of("950.00", AED), 1, 1));
        ledger.apply(new Event.Credit("C2", "ACC-001", Money.of("400.00", AED), 3, 3));
        ledger.apply(new Event.Debit("D2", "ACC-001", Money.of("620.00", AED), 5, 2));
    }

    @Test
    void br8_aBackDatedEntryCountsFromItsValueDateNotItsBookingDay() {
        assertEquals(Money.of("-370.00", AED), ledger.closingBalance("ACC-001", 2));
    }

    @Test
    void br8_anEntryValueDatedAfterTheDayIsNotCounted() {
        assertEquals(Money.of("250.00", AED), ledger.closingBalance("ACC-001", 1));
    }

    @Test
    void br8_anEntryValueDatedExactlyOnTheDayIsCounted() {
        assertEquals(Money.of("30.00", AED), ledger.closingBalance("ACC-001", 3));
    }

    @Test
    void br8_everyLaterDayIncludesAllEarlierEntries() {
        assertEquals(Money.of("30.00", AED), ledger.closingBalance("ACC-001", 6));
    }

    @Test
    void br8_anotherAccountsEntriesAreNotCounted() {
        ledger.apply(new Event.Credit("C3", "ACC-002", Money.of("10.000", BHD), 1, 1));

        assertEquals(Money.of("250.00", AED), ledger.closingBalance("ACC-001", 1));
        assertEquals(Money.of("10.000", BHD), ledger.closingBalance("ACC-002", 1));
    }

    @Test
    void br8_anotherAccountInTheSameCurrencyIsNotCounted() {
        ledger.apply(new Event.Credit("C4", "ACC-003", Money.of("75.00", AED), 1, 1));

        assertEquals(Money.of("250.00", AED), ledger.closingBalance("ACC-001", 1));
        assertEquals(Money.of("75.00", AED), ledger.closingBalance("ACC-003", 1));
    }

    @Test
    void askingAboutAnUnknownAccountIsACallerMistake() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> ledger.closingBalance("ACC-999", 1));
        assertEquals("unknown account ACC-999", error.getMessage());
    }
}
