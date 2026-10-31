package ledger.core;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterestTest {

    private Ledger ledger;

    @BeforeEach
    void openTheAccounts() {
        ledger = new Ledger(List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD)));
    }

    @Test
    void br27_br29_dailyInterestIs004PercentOfTheClosingBalanceRoundedToTheFils() {
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("250.00"), 1, 1));

        assertEquals(aed("0.10"), ledger.dailyInterest("ACC-001").get(0));
    }

    @Test
    void br29_anExactHalfFilsRoundsUp() {
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("12.50"), 1, 1));
        ledger.apply(new Event.Credit("C2", "ACC-002", Money.of("1.250", BHD), 1, 1));

        assertEquals(aed("0.01"), ledger.dailyInterest("ACC-001").get(0));
        assertEquals(Money.of("0.001", BHD), ledger.dailyInterest("ACC-002").get(0));
    }

    @Test
    void br28_zeroAndNegativeBalancesEarnNothing() {
        ledger.apply(new Event.Debit("D1", "ACC-001", aed("50.00"), 2, 2));

        assertEquals(aed("0.00"), ledger.dailyInterest("ACC-001").get(0));
        assertEquals(aed("0.00"), ledger.dailyInterest("ACC-001").get(1));
    }

    @Test
    void br30_br31_interestIsCreditedOnceAtTheEndOfDay6_asTheExactSumOfTheRoundedDays() {
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("250.00"), 1, 1));
        closeAllSixDays();

        assertEquals(List.of(new Entry("L0002", "INT-ACC-001-D6", "ACC-001", Entry.Type.INTEREST,
                aed("0.60"), 6, 6)), interestEntries());
        assertEquals(aed("250.60"), ledger.closingBalance("ACC-001", 6));
    }

    @Test
    void theInterestCreditDoesNotEarnInterestItself() {
        ledger.apply(new Event.Credit("C1", "ACC-001", aed("250.00"), 1, 1));
        closeAllSixDays();

        assertEquals(aed("0.10"), ledger.dailyInterest("ACC-001").get(5));
    }

    @Test
    void nothingIsCreditedWhenNoInterestWasEarned() {
        closeAllSixDays();

        assertTrue(interestEntries().isEmpty());
    }

    private void closeAllSixDays() {
        for (int day = Ledger.FIRST_DAY; day <= Ledger.LAST_DAY; day++) {
            ledger.closeDay(day);
        }
    }

    private List<Entry> interestEntries() {
        return ledger.entries().stream().filter(e -> e.type() == Entry.Type.INTEREST).toList();
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }
}
