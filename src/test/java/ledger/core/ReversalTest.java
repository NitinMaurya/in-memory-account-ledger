package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReversalTest {

    private Ledger ledger;

    @BeforeEach
    void bookTheBackDatedDebit() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED), new Account("ACC-003", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("1200.00"), 1, 1));
        ledger.apply(new Event.Debit("D1", "ACC-001", aed("950.00"), 1, 1));
        ledger.apply(new Event.Credit("C2", "ACC-001", aed("400.00"), 3, 3));
        ledger.apply(new Event.Debit("D2", "ACC-001", aed("620.00"), 5, 2));
    }

    @Test
    void br21_br22_reversingADebitPostsTheOppositeAmountOnTheGivenValueDate() {
        assertEquals(new Outcome.Accepted(), ledger.apply(reverse("R1", "ACC-001", "D2")));

        Entry reversal = ledger.entries().get(4);
        assertEquals(Entry.Type.REVERSAL, reversal.type());
        assertEquals(aed("620.00"), reversal.amount());
        assertEquals(6, reversal.bookingDay());
        assertEquals(2, reversal.valueDate());
        assertEquals(aed("250.00"), ledger.closingBalance("ACC-001", 2));
        assertEquals(aed("650.00"), ledger.closingBalance("ACC-001", 3));
    }

    @Test
    void br12_theOriginalEntryStaysAfterItIsReversed() {
        ledger.apply(reverse("R1", "ACC-001", "D2"));

        assertEquals(5, ledger.entries().size());
        assertEquals("D2", ledger.entries().get(3).eventId());
        assertEquals(aed("-620.00"), ledger.entries().get(3).amount());
    }

    @Test
    void br21_reversingACreditPostsANegativeAmount() {
        ledger.apply(reverse("R1", "ACC-001", "C2"));

        assertEquals(aed("-400.00"), ledger.entries().get(4).amount());
    }

    @Test
    void reversingSomethingThatDoesNotExistIsRefused() {
        assertEquals(rejected(Outcome.Reason.UNKNOWN_ORIGINAL), ledger.apply(reverse("R1", "ACC-001", "NOPE")));
        assertEquals(4, ledger.entries().size());
    }

    @Test
    void reversingTheSameTransactionTwiceIsRefused() {
        ledger.apply(reverse("R1", "ACC-001", "D2"));

        assertEquals(rejected(Outcome.Reason.ALREADY_REVERSED), ledger.apply(reverse("R2", "ACC-001", "D2")));
        assertEquals(aed("250.00"), ledger.closingBalance("ACC-001", 2));
    }

    @Test
    void reversingARefusedRequestIsRefusedBecauseItNeverMovedMoney() {
        ledger.apply(new Event.Credit("C9", "ACC-001", aed("100.00"), 7, 7));

        assertEquals(rejected(Outcome.Reason.UNKNOWN_ORIGINAL), ledger.apply(reverse("R1", "ACC-001", "C9")));
    }

    @Test
    void reversingAReversalIsRefused() {
        ledger.apply(reverse("R1", "ACC-001", "D2"));

        assertEquals(rejected(Outcome.Reason.NOT_REVERSIBLE), ledger.apply(reverse("R2", "ACC-001", "R1")));
    }

    @Test
    void reversingACardSettlementIsRefused() {
        ledger.apply(new Event.Authorize("A1", "ACC-001", "Auth-A", aed("20.00"), 5, 5));
        ledger.apply(new Event.Settle("S1", "ACC-001", "Auth-A", aed("15.00"), 5, 5));

        assertEquals(rejected(Outcome.Reason.NOT_REVERSIBLE), ledger.apply(reverse("R1", "ACC-001", "S1")));
    }

    @Test
    void reversingAnotherAccountsTransactionIsRefused() {
        assertEquals(rejected(Outcome.Reason.UNKNOWN_ORIGINAL), ledger.apply(reverse("R1", "ACC-003", "D2")));
        assertEquals(aed("-370.00"), ledger.closingBalance("ACC-001", 2));
    }

    private static Event reverse(String id, String account, String reversesEventId) {
        return new Event.Reverse(id, account, reversesEventId, 6, 2);
    }

    private static Outcome rejected(Outcome.Reason reason) {
        return new Outcome.Rejected(reason);
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }
}
