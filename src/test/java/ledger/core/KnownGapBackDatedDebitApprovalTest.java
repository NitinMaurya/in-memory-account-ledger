package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * KNOWN GAP: fails on purpose, against this ledger's own design. Excluded from the default run;
 * run with {@code mvn test -Dgroups=known-gap -DexcludedGroups=}
 */
@Tag("known-gap")
class KnownGapBackDatedDebitApprovalTest {

    @Test
    void aBackDatedDebitWithoutApproverOrReasonCodeIsRejected() {
        Ledger ledger = new Ledger(List.of(new Account("ACC-001", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", Money.of("1200.00", AED), 1, 1));

        // Booked Day 5, value-dated Day 2: rewrites days already reported. In the brief this is
        // E7, a mistake (E9 reverses it) that first caused three overdraft fees.
        Event backDatedDebit = new Event.Debit("D1", "ACC-001", Money.of("620.00", AED), 5, 2);

        Outcome outcome = ledger.apply(backDatedDebit);

        // WHAT THIS REVEALS: no maker-checker. One caller can post a back-dated debit; Event.Debit
        // has no approver or reason code, so the design can't even express a second check, which
        // would likely have stopped E7. Not built because the brief's events carry no approver:
        // requiring one would reject E7 and break the replay. The control is proposed in the
        // architecture document.
        assertInstanceOf(Outcome.Rejected.class, outcome,
                "KNOWN GAP: a back-dated debit with no approver and no reason code was accepted");
    }
}
