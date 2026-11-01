package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationTest {

    private static final Outcome APPROVED = new Outcome.Accepted();
    private static final Outcome DECLINED = new Outcome.Rejected(Outcome.Reason.INSUFFICIENT_AVAILABLE);

    private Ledger ledger;

    @BeforeEach
    void creditTwoHundredFifty() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("250.00"), 1, 1));
    }

    @Test
    void br14_br10_anAuthorizationHoldsMoneyWithoutMovingIt() {
        assertEquals(APPROVED, ledger.apply(authorize("A1", "Auth-A", "200.00")));

        assertEquals(1, ledger.entries().size());
        assertEquals(aed("250.00"), ledger.closingBalance("ACC-001", 2));
    }

    @Test
    void br9_availableIsTheLedgerBalanceMinusActiveHolds() {
        ledger.apply(authorize("A1", "Auth-A", "200.00"));

        assertEquals(aed("50.00"), ledger.availableBalance("ACC-001", 2));
    }

    @Test
    void br9_br15_moneyValueDatedLaterIsNotAvailableYet() {
        ledger.apply(new Event.Credit("C2", "ACC-001", aed("1000.00"), 1, 6));

        assertEquals(aed("250.00"), ledger.availableBalance("ACC-001", 2));
        assertEquals(DECLINED, ledger.apply(authorize("A1", "Auth-A", "300.00")));
        assertEquals(aed("1250.00"), ledger.availableBalance("ACC-001", 6));
    }

    @Test
    void br15_approvedWhenAvailableStaysExactlyAtZero() {
        assertEquals(APPROVED, ledger.apply(authorize("A1", "Auth-A", "250.00")));
        assertEquals(aed("0.00"), ledger.availableBalance("ACC-001", 2));
    }

    @Test
    void br15_br16_declinedWhenAvailableWouldGoBelowZero_andNoHoldIsPlaced() {
        assertEquals(DECLINED, ledger.apply(authorize("A1", "Auth-A", "250.01")));
        assertEquals(aed("250.00"), ledger.availableBalance("ACC-001", 2));
    }

    @Test
    void br15_earlierHoldsCountAgainstALaterAuthorization() {
        assertEquals(APPROVED, ledger.apply(authorize("A1", "Auth-A", "100.00")));
        assertEquals(APPROVED, ledger.apply(authorize("A2", "Auth-B", "100.00")));

        assertEquals(DECLINED, ledger.apply(authorize("A3", "Auth-C", "60.00")));
        assertEquals(aed("50.00"), ledger.availableBalance("ACC-001", 2));
    }

    @Test
    void br15_aBackDatedDebitAlreadyPostedCountsAgainstAvailable() {
        ledger.apply(new Event.Debit("D1", "ACC-001", aed("620.00"), 5, 2));

        assertEquals(DECLINED, ledger.apply(new Event.Authorize("A1", "ACC-001", "Auth-B", aed("90.00"), 5, 5)));
    }

    @Test
    void authorizationsShowWhichAreActiveAndWhichWereDeclined() {
        ledger.apply(authorize("A1", "Auth-A", "200.00"));
        ledger.apply(authorize("A2", "Auth-B", "90.00"));

        assertEquals(List.of(
                        new Authorization("Auth-A", "ACC-001", aed("200.00"), Authorization.Status.ACTIVE, aed("0.00")),
                        new Authorization("Auth-B", "ACC-001", aed("90.00"), Authorization.Status.DECLINED, aed("0.00"))),
                ledger.authorizations());
    }

    private static Event authorize(String id, String authId, String amount) {
        return new Event.Authorize(id, "ACC-001", authId, aed(amount), 2, 2);
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }
}
