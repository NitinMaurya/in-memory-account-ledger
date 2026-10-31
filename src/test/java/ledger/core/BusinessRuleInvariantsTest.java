package ledger.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
