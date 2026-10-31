package ledger.scenario;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import ledger.core.Account;
import ledger.core.Entry;
import ledger.core.Ledger;
import ledger.core.Money;
import org.junit.jupiter.api.Test;

class BriefScenarioTest {

    @Test
    void br1_theBriefHasTwoAccounts_ACC001InAed_ACC002InBhd() {
        assertEquals(List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD)), BriefScenario.accounts());
    }

    @Test
    void replayingTheBriefChargesThreeFees_forDays2_4And5_allFoundOnDay5() {
        Ledger ledger = replayTheBrief();

        List<Entry> fees = ledger.entries().stream().filter(e -> e.type() == Entry.Type.OVERDRAFT_FEE).toList();
        assertEquals(List.of(2, 4, 5), fees.stream().map(Entry::valueDate).toList());
        assertEquals(List.of(5, 5, 5), fees.stream().map(Entry::bookingDay).toList());
    }

    @Test
    void replayingTheBriefClosesDays1To5At250_225_625_415_390() {
        Ledger ledger = replayTheBrief();

        List<String> expected = List.of("250.00", "225.00", "625.00", "415.00", "390.00");
        for (int day = 1; day <= 5; day++) {
            assertEquals(Money.of(expected.get(day - 1), AED), ledger.closingBalance("ACC-001", day), "Day " + day);
        }
    }

    @Test
    void replayingTheBriefEarnsAcc001DailyInterestOf_010_009_025_017_016_016_andEndsAt390_93() {
        Ledger ledger = replayTheBrief();

        List<Money> expected = List.of("0.10", "0.09", "0.25", "0.17", "0.16", "0.16").stream()
                .map(amount -> Money.of(amount, AED))
                .toList();
        assertEquals(expected, ledger.dailyInterest("ACC-001"));
        assertEquals(Money.of("390.93", AED), ledger.closingBalance("ACC-001", 6));
    }

    private static Ledger replayTheBrief() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        Replay.run(ledger, BriefScenario.events(), () -> {});
        return ledger;
    }
}
