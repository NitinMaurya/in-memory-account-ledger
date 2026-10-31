package ledger.core;

import static ledger.core.AppendOnlyCheck.assertOnlyAppended;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void br14_authorizationsNeverCreateEntries() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            for (LoggedEvent logged : ledger.eventLog()) {
                if (logged.event() instanceof Event.Authorize authorize) {
                    for (Entry entry : ledger.entries()) {
                        assertNotEquals(authorize.id(), entry.eventId(), () -> "BR-14 violated: entry " + entry);
                    }
                }
            }
        });
    }

    @Test
    void br15_everyApprovedAuthorizationLeftAvailableAtOrAboveZero() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            List<LoggedEvent> log = ledger.eventLog();
            LoggedEvent last = log.get(log.size() - 1);
            if (last.event() instanceof Event.Authorize authorize && last.outcome() instanceof Outcome.Accepted) {
                assertTrue(ledger.availableBalance(authorize.account()).minorUnits() >= 0,
                        () -> "BR-15 violated: " + authorize.authId() + " approved with available "
                                + ledger.availableBalance(authorize.account()));
            }
        });
    }

    @Test
    void br18_br20_everySettlementEntryHasAnApprovedAuthorizationAndFitsItsHold() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            for (Entry entry : ledger.entries()) {
                if (entry.type() != Entry.Type.SETTLEMENT) {
                    continue;
                }
                Event.Settle settle = (Event.Settle) eventWithId(ledger, entry.eventId());
                Authorization authorization = ledger.authorizations().stream()
                        .filter(a -> a.authId().equals(settle.authId()) && a.account().equals(settle.account()))
                        .filter(a -> a.status() != Authorization.Status.DECLINED)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("BR-20 violated: money left for " + settle.authId()));
                assertTrue(settle.amount().minorUnits() <= authorization.requested().minorUnits(),
                        () -> "settlement " + settle.id() + " exceeds its hold");
            }
        });
    }

    @Test
    void br21_everyReversalMirrorsExactlyOneEarlierCreditOrDebitOnTheSameAccount() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            List<String> reversed = new ArrayList<>();
            for (Entry entry : ledger.entries()) {
                if (entry.type() != Entry.Type.REVERSAL) {
                    continue;
                }
                Event.Reverse reverse = (Event.Reverse) eventWithId(ledger, entry.eventId());
                Entry original = ledger.entries().stream()
                        .filter(e -> e.eventId().equals(reverse.reversesEventId()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("BR-21 violated: nothing to reverse for " + reverse.id()));
                assertTrue(original.type() == Entry.Type.CREDIT || original.type() == Entry.Type.DEBIT);
                assertEquals(original.account(), entry.account());
                assertEquals(original.amount().negate(), entry.amount(), () -> "BR-21: not the opposite amount");
                assertTrue(!reversed.contains(original.eventId()), () -> original.eventId() + " reversed twice");
                reversed.add(original.eventId());
            }
        });
    }

    @Test
    void br24_neverTwoFeesForTheSameAccountAndDay() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            List<String> charged = new ArrayList<>();
            for (Entry entry : ledger.entries()) {
                if (entry.type() == Entry.Type.OVERDRAFT_FEE) {
                    String accountDay = entry.account() + " Day " + entry.valueDate();
                    assertTrue(!charged.contains(accountDay), () -> "BR-24 violated: two fees for " + accountDay);
                    charged.add(accountDay);
                }
            }
        });
    }

    @Test
    void br25_noFeeIsEverForADayAfterTheDayItWasBooked() {
        Ledger ledger = new Ledger(BriefScenario.accounts());

        Replay.run(ledger, BriefScenario.events(), () -> {
            for (Entry entry : ledger.entries()) {
                if (entry.type() == Entry.Type.OVERDRAFT_FEE) {
                    assertTrue(entry.valueDate() <= entry.bookingDay(), () -> "fee for a future day: " + entry);
                }
            }
        });
    }

    private static Event eventWithId(Ledger ledger, String eventId) {
        return ledger.eventLog().stream()
                .map(LoggedEvent::event)
                .filter(e -> e.id().equals(eventId))
                .findFirst()
                .orElseThrow();
    }
}
