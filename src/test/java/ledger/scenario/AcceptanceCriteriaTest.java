package ledger.scenario;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import ledger.core.Authorization;
import ledger.core.Entry;
import ledger.core.Ledger;
import ledger.core.LoggedEvent;
import ledger.core.Money;
import ledger.core.Outcome;
import org.junit.jupiter.api.Test;

/**
 * The brief's eight acceptance criteria, against the brief's replay. Accepted criteria are proven
 * true; refused criteria are proven wrong by testing what actually happens (see REJECTED.md).
 */
class AcceptanceCriteriaTest {

    private static final String ACC_001 = "ACC-001";

    @Test
    void criterion1_accepted_day2ClosingAtEndOfDay5BeforeAnyFeeIsMinus370() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        List<Money> day2BeforeFees = new ArrayList<>();

        Replay.run(ledger, BriefScenario.events(), () -> {}, day -> {
            if (day == 5) {
                day2BeforeFees.add(sumOf(ledger, e -> e.valueDate() <= 2 && e.type() != Entry.Type.OVERDRAFT_FEE));
            }
        });

        assertEquals(List.of(aed("-370.00")), day2BeforeFees);
    }

    @Test
    void criterion2_refused_E7CausesThreeFeesNotOne_forDays2_4And5() {
        Ledger ledger = replayTheBrief();

        assertEquals(List.of(2, 4, 5), fees(ledger).stream().map(Entry::valueDate).toList());
    }

    @Test
    void criterion3_accepted_theDay4SettlementOfAuthAIsAccepted() {
        assertEquals(new Outcome.Accepted(), outcomeOf(replayTheBrief(), "E5"));
    }

    @Test
    void criterion4_accepted_theSettlementForUnknownAuthZIsRefusedAndNoMoneyLeaves() {
        Ledger ledger = replayTheBrief();

        assertEquals(new Outcome.Rejected(Outcome.Reason.UNKNOWN_AUTHORIZATION), outcomeOf(ledger, "E6"));
        assertTrue(ledger.entries().stream().noneMatch(e -> e.eventId().equals("E6")));
    }

    @Test
    void criterion5_accepted_aHoldReducesAvailableNotLedger_butAuthBIsNeverApproved() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        List<Money> day2 = new ArrayList<>();

        Replay.run(ledger, BriefScenario.events(), () -> {}, day -> {
            if (day == 2) {
                day2.add(ledger.closingBalance(ACC_001, 2));
                day2.add(ledger.availableBalance(ACC_001));
            }
        });

        assertEquals(List.of(aed("250.00"), aed("50.00")), day2);
        assertEquals(Authorization.Status.DECLINED, statusOf(ledger, "Auth-B"));
    }

    @Test
    void criterion6_refused_afterE9TheFeesStay_soDay2Is225Not250() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        List<String> before = new ArrayList<>();
        List<String> after = new ArrayList<>();

        Replay.run(ledger, BriefScenario.events(), () -> {
            String lastEvent = lastLogged(ledger);
            if (lastEvent.equals("E6") && before.isEmpty()) {
                before.add(ledger.closingBalance(ACC_001, 2) + " with " + fees(ledger).size() + " fees");
            }
            if (lastEvent.equals("E9") && after.isEmpty()) {
                after.add(ledger.closingBalance(ACC_001, 2) + " with " + fees(ledger).size() + " fees");
            }
        });

        assertEquals(List.of("AED 250.00 with 0 fees"), before);
        assertEquals(List.of("AED 225.00 with 3 fees"), after);
        assertEquals(Authorization.Status.DECLINED, statusOf(ledger, "Auth-B"));
    }

    @Test
    void criterion7_refused_theInstalmentsAre3333_3333_3334_not3334Each() {
        Ledger ledger = replayTheBrief();

        List<Money> instalments = ledger.entries().stream()
                .filter(e -> e.type() == Entry.Type.INSTALMENT)
                .map(Entry::amount)
                .toList();
        assertEquals(List.of(bhd("3.333"), bhd("3.333"), bhd("3.334")), instalments);
        assertEquals(bhd("10.000"), instalments.stream().reduce(Money::plus).orElseThrow());
    }

    @Test
    void criterion8_refused_nothingIsDiscarded_theCreditEqualsTheSumOfTheRoundedDays() {
        Ledger ledger = replayTheBrief();

        assertEquals(aed("0.93"), interestCredited(ledger, ACC_001));
        assertEquals(bhd("0.008"), interestCredited(ledger, "ACC-002"));
        for (String account : List.of(ACC_001, "ACC-002")) {
            Money sumOfDays = ledger.dailyInterest(account).stream().reduce(Money::plus).orElseThrow();
            assertEquals(sumOfDays, interestCredited(ledger, account));
        }
    }

    private static Ledger replayTheBrief() {
        Ledger ledger = new Ledger(BriefScenario.accounts());
        Replay.run(ledger, BriefScenario.events(), () -> {});
        return ledger;
    }

    private static Money sumOf(Ledger ledger, java.util.function.Predicate<Entry> which) {
        return ledger.entries().stream()
                .filter(e -> e.account().equals(ACC_001))
                .filter(which)
                .map(Entry::amount)
                .reduce(Money.zero(AED), Money::plus);
    }

    private static List<Entry> fees(Ledger ledger) {
        return ledger.entries().stream().filter(e -> e.type() == Entry.Type.OVERDRAFT_FEE).toList();
    }

    private static Outcome outcomeOf(Ledger ledger, String eventId) {
        return ledger.eventLog().stream()
                .filter(logged -> logged.event().id().equals(eventId))
                .map(LoggedEvent::outcome)
                .findFirst()
                .orElseThrow();
    }

    private static Authorization.Status statusOf(Ledger ledger, String authId) {
        return ledger.authorizations().stream()
                .filter(a -> a.authId().equals(authId))
                .map(Authorization::status)
                .findFirst()
                .orElseThrow();
    }

    private static String lastLogged(Ledger ledger) {
        List<LoggedEvent> log = ledger.eventLog();
        return log.isEmpty() ? "" : log.get(log.size() - 1).event().id();
    }

    private static Money interestCredited(Ledger ledger, String account) {
        return ledger.entries().stream()
                .filter(e -> e.type() == Entry.Type.INTEREST && e.account().equals(account))
                .map(Entry::amount)
                .findFirst()
                .orElseThrow();
    }

    private static Money aed(String amount) {
        return Money.of(amount, AED);
    }

    private static Money bhd(String amount) {
        return Money.of(amount, BHD);
    }
}
