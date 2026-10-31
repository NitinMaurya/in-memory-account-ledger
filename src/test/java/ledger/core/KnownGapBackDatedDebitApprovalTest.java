package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * KNOWN GAP: this test fails on purpose. It is written against this ledger's own design, to show
 * a control the design does not have.
 *
 * <p>It is tagged {@code known-gap}, so the default {@code mvn test} leaves it out and stays a
 * clear pass/fail signal for everything else. Run it on its own with:
 * {@code mvn test -Dgroups=known-gap -DexcludedGroups=}
 */
@Tag("known-gap")
class KnownGapBackDatedDebitApprovalTest {

    @Test
    void aBackDatedDebitWithoutApproverOrReasonCodeIsRejected() {
        Ledger ledger = new Ledger(List.of(new Account("ACC-001", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", Money.of("1200.00", AED), 1, 1));

        // Booked on Day 5 but value-dated Day 2: this one entry changes the closing balance of
        // Days 2, 3 and 4, which have already been reported. In the brief this is E7. It turned
        // out to be a mistake (E9 reverses it), and before the reversal it pushed three days
        // negative and triggered three overdraft fees.
        Event backDatedDebit = new Event.Debit("D1", "ACC-001", Money.of("620.00", AED), 5, 2);

        Outcome outcome = ledger.apply(backDatedDebit);

        // WHAT THIS REVEALS
        //
        // 1. The ledger accepts any back-dated debit from a single caller. Event.Debit has no
        //    field for an approver or a reason code, so the design cannot even express
        //    "a second person checked this".
        //
        // 2. In a bank, rewriting already-reported days is one of the riskiest postings there is.
        //    A maker-checker control (a different person approves it, with a reason code, within
        //    a limit on how far back) would most likely have caught E7 before it reached the
        //    customer and the fees.
        //
        // 3. Why it is not simply built: the brief's event stream carries no approver or reason
        //    data. Requiring them would reject E7 and break the replay the brief asks for. So the
        //    gap is left visible here, and the control is described as the first thing to add
        //    before going live in the architecture document.
        assertInstanceOf(Outcome.Rejected.class, outcome,
                "KNOWN GAP: a back-dated debit with no approver and no reason code was accepted");
    }
}
