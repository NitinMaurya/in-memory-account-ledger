package ledger.core;

import static ledger.core.AppendOnlyCheck.assertOnlyAppended;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ledger.scenario.BriefScenario;
import ledger.scenario.Replay;
import org.junit.jupiter.api.Test;

/** Each rule is checked after every replay step. BR-n ids match BUSINESS-RULES.md. */
class BusinessRuleInvariantsTest {

    /** Money is whole minor units, so precision holds if each entry is in its account's currency. */
    @Test
    void br6_everyEntryIsKeptAtItsAccountsCurrencyPrecision() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        Map<String, Currency> currencyOf = BriefScenario.accounts().stream()
                .collect(Collectors.toMap(Account::id, Account::currency));

        Replay.run(ledger, BriefScenario.events(), () -> {
            for (Entry entry : ledger.entries()) {
                assertEquals(currencyOf.get(entry.account()), entry.amount().currency(),
                        () -> "BR-6 violated by " + entry);
            }
        });
    }

    @Test
    void br11_nothingRecordedIsEverChangedOrRemoved() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        List<Entry> entriesBefore = new ArrayList<>();
        List<LoggedEvent> logBefore = new ArrayList<>();

        Replay.run(ledger, BriefScenario.events(), () -> {
            assertOnlyAppended("entries", entriesBefore, ledger.entries());
            assertOnlyAppended("event log", logBefore, ledger.eventLog());
            entriesBefore.clear();
            entriesBefore.addAll(ledger.entries());
            logBefore.clear();
            logBefore.addAll(ledger.eventLog());
        });

        assertThrows(UnsupportedOperationException.class, () -> ledger.entries().clear(),
                "BR-11: entries can be changed from outside the ledger");
        assertThrows(UnsupportedOperationException.class, () -> ledger.eventLog().clear(),
                "BR-11: the event log can be changed from outside the ledger");
    }

    @Test
    void br8_closingBalanceIsTheSumOfEntriesValueDatedOnOrBeforeThatDay() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            for (Account account : BriefScenario.accounts()) {
                for (int day = Ledger.FIRST_DAY; day <= Ledger.LAST_DAY; day++) {
                    long expected = 0;
                    for (Entry entry : ledger.entries()) {
                        if (entry.account().equals(account.id()) && entry.valueDate() <= day) {
                            expected += entry.amount().minorUnits();
                        }
                    }
                    int d = day;
                    assertEquals(new Money(expected, account.currency()), ledger.closingBalance(account.id(), day),
                            () -> "BR-8 violated for " + account.id() + " on Day " + d);
                }
            }
        });
    }
}
