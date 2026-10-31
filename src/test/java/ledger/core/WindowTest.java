package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WindowTest {

    private Ledger ledger;

    @BeforeEach
    void openTheAccount() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED)));
    }

    @Test
    void br4_eventsOnTheFirstAndLastDayOfTheWindowAreAccepted() {
        assertEquals(new Outcome.Accepted(), ledger.apply(credit("C1", 1, 1)));
        assertEquals(new Outcome.Accepted(), ledger.apply(credit("C6", 6, 6)));
    }

    @Test
    void br4_anEventBookedAfterTheWindowIsRefusedAndMovesNoMoney() {
        Outcome outcome = ledger.apply(credit("C7", 7, 6));

        assertEquals(new Outcome.Rejected(Outcome.Reason.OUTSIDE_WINDOW), outcome);
        assertTrue(ledger.entries().isEmpty());
        assertEquals(1, ledger.eventLog().size());
    }

    @Test
    void br4_anEventValueDatedBeforeTheWindowIsRefusedAndMovesNoMoney() {
        Outcome outcome = ledger.apply(credit("C0", 1, 0));

        assertEquals(new Outcome.Rejected(Outcome.Reason.OUTSIDE_WINDOW), outcome);
        assertTrue(ledger.entries().isEmpty());
    }

    @Test
    void br4_closingADayOutsideTheWindowIsACallerMistake() {
        assertThrows(IllegalArgumentException.class, () -> ledger.closeDay(0));
        assertThrows(IllegalArgumentException.class, () -> ledger.closeDay(7));
    }

    private static Event credit(String id, int bookingDay, int valueDate) {
        return new Event.Credit(id, "ACC-001", Money.of("100.00", AED), bookingDay, valueDate);
    }
}
