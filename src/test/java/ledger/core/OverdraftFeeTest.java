package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverdraftFeeTest {

    private Ledger ledger;

    @BeforeEach
    void openTheAccounts() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD)));
    }

    @Test
    void br23_br25_aDayClosingNegativeIsChargedOneFeeDatedThatDay() {
        ledger.apply(credit("C1", "100.00", 1, 1));
        ledger.apply(debit("D1", "150.00", 1, 1));
        ledger.closeDay(1);

        assertEquals(List.of(new Entry("L0003", "FEE-ACC-001-D1", "ACC-001", Entry.Type.OVERDRAFT_FEE,
                aed("-25.00"), 1, 1)), fees());
        assertEquals(aed("-75.00"), ledger.closingBalance("ACC-001", 1));
    }

    @Test
    void br23_aDayClosingAtExactlyZeroIsNotCharged() {
        ledger.apply(credit("C1", "100.00", 1, 1));
        ledger.apply(debit("D1", "100.00", 1, 1));
        ledger.closeDay(1);

        assertTrue(fees().isEmpty());
    }

    @Test
    void br24_aDayIsChargedAtMostOnce_evenWhenItIsCheckedAgain() {
        ledger.apply(debit("D1", "50.00", 1, 1));
        ledger.closeDay(1);
        ledger.apply(debit("D2", "30.00", 2, 1));

        assertEquals(1, fees().size());
    }

    @Test
    void br25_aBackDatedDebitCausesAFeeForEveryClosedDayItTurnsNegative_notJustOne() {
        bookUpToTheEndOfDay4();

        ledger.apply(debit("E7", "620.00", 5, 2));

        assertEquals(List.of(2, 4), feeValueDates());
        assertTrue(fees().stream().allMatch(fee -> fee.bookingDay() == 5));

        ledger.closeDay(5);
        assertEquals(List.of(2, 4, 5), feeValueDates());
    }

    @Test
    void br23_day3EscapesBecauseItsDepositKeepsItPositive() {
        bookUpToTheEndOfDay4();
        ledger.apply(debit("E7", "620.00", 5, 2));

        assertEquals(aed("5.00"), ledger.closingBalance("ACC-001", 3));
        assertTrue(fees().stream().noneMatch(fee -> fee.valueDate() == 3));
    }

    @Test
    void p1_aReversalRefundsEachFeeItsOriginalCaused_datedTheSameDayAsTheFee() {
        bookUpToTheEndOfDay4();
        ledger.apply(debit("E7", "620.00", 5, 2));
        ledger.closeDay(5);

        ledger.apply(new Event.Reverse("E9", "ACC-001", "E7", 6, 2));

        assertEquals(List.of(2, 4, 5), feeValueDates());
        List<Entry> refunds = refunds();
        assertEquals(List.of(2, 4, 5), refunds.stream().map(Entry::valueDate).toList());
        assertTrue(refunds.stream().allMatch(r -> r.amount().equals(aed("25.00")) && r.bookingDay() == 6));
        assertEquals(aed("250.00"), ledger.closingBalance("ACC-001", 2));
        assertEquals(aed("465.00"), ledger.closingBalance("ACC-001", 5));
    }

    @Test
    void p1_aFeeStaysWhenTheReversalDoesNotMakeItsDayPositiveAgain() {
        ledger.apply(debit("D1", "50.00", 1, 1));
        ledger.apply(debit("D2", "30.00", 1, 1));
        ledger.closeDay(1);

        ledger.apply(new Event.Reverse("R1", "ACC-001", "D2", 2, 1));

        assertTrue(refunds().isEmpty());
        assertEquals(aed("-75.00"), ledger.closingBalance("ACC-001", 1));
    }

    @Test
    void p1_onlyDaysFromTheReversalsValueDateAreRefunded() {
        ledger.apply(debit("D1", "10.00", 1, 1));
        ledger.closeDay(1);
        ledger.apply(credit("C2", "20.00", 2, 2));
        ledger.closeDay(2);
        ledger.apply(debit("D3", "5.00", 3, 3));
        ledger.closeDay(3);

        ledger.apply(new Event.Reverse("R1", "ACC-001", "D3", 4, 3));

        // Day 2 is +10.00 without fees but was charged because of Day 1's fee. The reversal is dated
        // Day 3, so it didn't change Day 2, and Day 2's fee stays.
        assertEquals(List.of(1, 2, 3), feeValueDates());
        assertEquals(List.of(3), refunds().stream().map(Entry::valueDate).toList());
    }

    @Test
    void p1_br24_aRefundedDayIsNeverChargedAgain() {
        bookUpToTheEndOfDay4();
        ledger.apply(debit("E7", "620.00", 5, 2));
        ledger.closeDay(5);
        ledger.apply(new Event.Reverse("E9", "ACC-001", "E7", 6, 2));

        ledger.apply(debit("D9", "1000.00", 6, 2));

        for (int day : List.of(2, 4, 5)) {
            assertEquals(1, feeValueDates().stream().filter(d -> d == day).count(), "Day " + day);
        }
    }

    @Test
    void br26_theBhdAccountIsNeverCharged() {
        ledger.apply(new Event.Debit("D1", "ACC-002", Money.of("1.000", BHD), 1, 1));
        ledger.closeDay(1);

        assertTrue(fees().isEmpty());
        assertEquals(Money.of("-1.000", BHD), ledger.closingBalance("ACC-002", 1));
    }

    @Test
    void daysMustCloseInOrderAndOnlyOnce() {
        ledger.closeDay(1);

        assertThrows(IllegalStateException.class, () -> ledger.closeDay(3));
        assertThrows(IllegalStateException.class, () -> ledger.closeDay(1));
    }

    /** The brief's E1-E5: closing 250, 250, 650, 465. Days 1-4 closed, all positive. */
    private void bookUpToTheEndOfDay4() {
        ledger.apply(credit("E1", "1200.00", 1, 1));
        ledger.apply(debit("E2", "950.00", 1, 1));
        ledger.closeDay(1);
        ledger.apply(new Event.Authorize("E3", "ACC-001", "Auth-A", aed("200.00"), 2, 2));
        ledger.closeDay(2);
        ledger.apply(credit("E4", "400.00", 3, 3));
        ledger.closeDay(3);
        ledger.apply(new Event.Settle("E5", "ACC-001", "Auth-A", aed("185.00"), 4, 4));
        ledger.closeDay(4);
    }

    private List<Entry> fees() {
        return ledger.entries().stream().filter(e -> e.type() == Entry.Type.OVERDRAFT_FEE).toList();
    }

    private List<Entry> refunds() {
        return ledger.entries().stream().filter(e -> e.type() == Entry.Type.FEE_REFUND).toList();
    }

    private List<Integer> feeValueDates() {
        return fees().stream().map(Entry::valueDate).toList();
    }

    private static Event credit(String id, String amount, int bookingDay, int valueDate) {
        return new Event.Credit(id, "ACC-001", aed(amount), bookingDay, valueDate);
    }

    private static Event debit(String id, String amount, int bookingDay, int valueDate) {
        return new Event.Debit(id, "ACC-001", aed(amount), bookingDay, valueDate);
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }
}
