package ledger.core;

import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SettlementTest {

    private static final Outcome ACCEPTED = new Outcome.Accepted();

    private Ledger ledger;

    @BeforeEach
    void creditAndHoldTwoHundredForAuthA() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED), new Account("ACC-003", AED)));
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("250.00"), 1, 1));
        ledger.apply(new Event.Authorize("A1", "ACC-001", "Auth-A", aed("200.00"), 2, 2));
    }

    @Test
    void br18_br19_aSettlementForLessThanTheHoldTakesTheMoneyAndReleasesTheWholeHold() {
        assertEquals(ACCEPTED, ledger.apply(settle("S1", "ACC-001", "Auth-A", "185.00")));

        Entry settlement = ledger.entries().get(1);
        assertEquals(Entry.Type.SETTLEMENT, settlement.type());
        assertEquals(aed("-185.00"), settlement.amount());
        assertEquals(aed("65.00"), ledger.closingBalance("ACC-001", 4));
        assertEquals(aed("65.00"), ledger.availableBalance("ACC-001", 4));
        assertEquals(
                new Authorization("Auth-A", "ACC-001", aed("200.00"), Authorization.Status.SETTLED, aed("185.00")),
                ledger.authorizations().get(0));
    }

    @Test
    void br18_aSettlementForTheFullHoldIsAccepted() {
        assertEquals(ACCEPTED, ledger.apply(settle("S1", "ACC-001", "Auth-A", "200.00")));
        assertEquals(aed("50.00"), ledger.availableBalance("ACC-001", 4));
    }

    @Test
    void br20_aSettlementForAnAuthorizationThatDoesNotExistIsRefusedAndMovesNoMoney() {
        Outcome outcome = ledger.apply(settle("S1", "ACC-001", "Auth-Z", "180.00"));

        assertEquals(rejected(Outcome.Reason.UNKNOWN_AUTHORIZATION), outcome);
        assertEquals(1, ledger.entries().size());
        assertEquals(aed("250.00"), ledger.closingBalance("ACC-001", 4));
    }

    @Test
    void aSettlementForMoreThanTheHoldIsRefusedAndTheHoldStays() {
        assertEquals(rejected(Outcome.Reason.SETTLEMENT_EXCEEDS_HOLD),
                ledger.apply(settle("S1", "ACC-001", "Auth-A", "200.01")));

        assertEquals(1, ledger.entries().size());
        assertEquals(aed("50.00"), ledger.availableBalance("ACC-001", 4));
    }

    @Test
    void aSecondSettlementOfTheSameAuthorizationIsRefused() {
        ledger.apply(settle("S1", "ACC-001", "Auth-A", "185.00"));

        assertEquals(rejected(Outcome.Reason.ALREADY_SETTLED),
                ledger.apply(settle("S2", "ACC-001", "Auth-A", "10.00")));
        assertEquals(aed("65.00"), ledger.closingBalance("ACC-001", 4));
    }

    @Test
    void aSettlementForADeclinedAuthorizationIsRefusedAsUnknown() {
        ledger.apply(new Event.Authorize("A2", "ACC-001", "Auth-B", aed("90.00"), 2, 2));

        assertEquals(rejected(Outcome.Reason.UNKNOWN_AUTHORIZATION),
                ledger.apply(settle("S1", "ACC-001", "Auth-B", "90.00")));
    }

    @Test
    void aSettlementAgainstAnotherAccountsAuthorizationIsRefusedAsUnknown() {
        ledger.apply(new Event.Credit("C2", "ACC-003", aed("500.00"), 1, 1));

        assertEquals(rejected(Outcome.Reason.UNKNOWN_AUTHORIZATION),
                ledger.apply(settle("S1", "ACC-003", "Auth-A", "185.00")));
        assertEquals(aed("500.00"), ledger.closingBalance("ACC-003", 4));
    }

    @Test
    void anAuthorizationReusingAnExistingIdIsRefusedAndPlacesNoHold() {
        assertEquals(rejected(Outcome.Reason.DUPLICATE_AUTHORIZATION),
                ledger.apply(new Event.Authorize("A2", "ACC-001", "Auth-A", aed("10.00"), 2, 2)));
        assertEquals(aed("50.00"), ledger.availableBalance("ACC-001", 4));
    }

    private static Event settle(String id, String account, String authId, String amount) {
        return new Event.Settle(id, account, authId, aed(amount), 4, 4);
    }

    private static Outcome rejected(Outcome.Reason reason) {
        return new Outcome.Rejected(reason);
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }
}
