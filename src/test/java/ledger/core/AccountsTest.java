package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountsTest {

    private Ledger ledger;

    @BeforeEach
    void openTheAccounts() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD)));
    }

    @Test
    void br2_bothAccountsOpenAtZeroOnEveryDay() {
        for (int day = Ledger.FIRST_DAY; day <= Ledger.LAST_DAY; day++) {
            assertEquals(Money.of("0.00", AED), ledger.closingBalance("ACC-001", day));
            assertEquals(Money.of("0.000", BHD), ledger.closingBalance("ACC-002", day));
        }
    }

    @Test
    void br2_thereIsNoOpeningEntry() {
        assertTrue(ledger.entries().isEmpty());
    }

    @Test
    void br1_anEventForAnAccountThatDoesNotExistIsRefusedAndMovesNoMoney() {
        Event credit = new Event.Credit("X1", "ACC-999", Money.of("100.00", AED), 1, 1);

        Outcome outcome = ledger.apply(credit);

        assertEquals(new Outcome.Rejected(Outcome.Reason.UNKNOWN_ACCOUNT), outcome);
        assertTrue(ledger.entries().isEmpty());
        assertEquals(List.of(new LoggedEvent(credit, outcome)), ledger.eventLog());
    }

    @Test
    void br3_anAmountInAnotherCurrencyIsRefusedAndMovesNoMoney() {
        Event bhdIntoAedAccount = new Event.Credit("X2", "ACC-001", Money.of("10.000", BHD), 1, 1);

        Outcome outcome = ledger.apply(bhdIntoAedAccount);

        assertEquals(new Outcome.Rejected(Outcome.Reason.CURRENCY_MISMATCH), outcome);
        assertTrue(ledger.entries().isEmpty());
        assertEquals(Money.of("0.00", AED), ledger.closingBalance("ACC-001", 1));
    }
}
