package ledger.core;

import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstalmentTest {

    private Ledger ledger;

    @BeforeEach
    void openTheBhdAccount() {
        ledger = new Ledger(List.of(new Account("ACC-002", BHD)));
    }

    @Test
    void br32_br33_e10PostsThreeInstalmentsThatAddUpExactly_not3x3334() {
        assertEquals(new Outcome.Accepted(), ledger.apply(instalments("E10", "10.000", 3)));

        assertEquals(List.of(bhd("3.333"), bhd("3.333"), bhd("3.334")),
                ledger.entries().stream().map(Entry::amount).toList());
        assertTrue(ledger.entries().stream().allMatch(e -> e.type() == Entry.Type.INSTALMENT && e.valueDate() == 5));
        assertEquals(bhd("10.000"), ledger.closingBalance("ACC-002", 5));
    }

    @Test
    void zeroInstalmentsAreRefused() {
        assertEquals(rejected(Outcome.Reason.INVALID_INSTALMENTS), ledger.apply(instalments("X1", "10.000", 0)));
        assertTrue(ledger.entries().isEmpty());
    }

    @Test
    void moreInstalmentsThanFilsAreRefused_noPartMayBeZero() {
        assertEquals(rejected(Outcome.Reason.INVALID_INSTALMENTS), ledger.apply(instalments("X1", "0.002", 3)));
        assertTrue(ledger.entries().isEmpty());
    }

    @Test
    void anInstalmentCreditCannotBeReversed() {
        ledger.apply(instalments("E10", "10.000", 3));

        assertEquals(rejected(Outcome.Reason.NOT_REVERSIBLE),
                ledger.apply(new Event.Reverse("R1", "ACC-002", "E10", 6, 5)));
    }

    private static Event instalments(String id, String total, int parts) {
        return new Event.InstalmentCredit(id, "ACC-002", bhd(total), parts, 5, 5);
    }

    private static Outcome rejected(Outcome.Reason reason) {
        return new Outcome.Rejected(reason);
    }

    private static Money bhd(String amount) {
        return Money.of(amount, BHD);
    }
}
