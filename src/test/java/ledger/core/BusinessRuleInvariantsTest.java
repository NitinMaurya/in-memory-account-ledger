package ledger.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;
import ledger.scenario.BriefScenario;
import ledger.scenario.Replay;
import org.junit.jupiter.api.Test;

/**
 * Business rules from the brief that must hold at every moment, whatever the implementation.
 * Each one is checked after every single step of the replay: every event applied and every
 * day closed. Test names carry the business-rule id (BR-n) they enforce.
 */
class BusinessRuleInvariantsTest {

    /**
     * BR-6: amounts are kept at their own currency's precision. Money is held as whole minor
     * units, so this holds as long as every entry is in its account's currency: AED entries in
     * hundredths, BHD entries in thousandths.
     */
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
