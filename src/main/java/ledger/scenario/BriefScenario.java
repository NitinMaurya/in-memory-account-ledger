package ledger.scenario;

import static ledger.core.Currency.AED;
import static ledger.core.Currency.BHD;

import java.util.List;
import ledger.core.Account;
import ledger.core.Event;
import ledger.core.Money;

/** The accounts and the event stream from the brief, in the order they must be replayed. */
public final class BriefScenario {

    private BriefScenario() {}

    public static List<Account> accounts() {
        return List.of(new Account("ACC-001", AED), new Account("ACC-002", BHD));
    }

    public static List<Event> events() {
        return List.of(
                new Event.Credit("E1", "ACC-001", Money.of("1200.00", AED), 1, 1),
                new Event.Debit("E2", "ACC-001", Money.of("950.00", AED), 1, 1),
                new Event.Authorize("E3", "ACC-001", "Auth-A", Money.of("200.00", AED), 2, 2),
                new Event.Credit("E4", "ACC-001", Money.of("400.00", AED), 3, 3),
                new Event.Settle("E5", "ACC-001", "Auth-A", Money.of("185.00", AED), 4, 4),
                new Event.Settle("E6", "ACC-001", "Auth-Z", Money.of("180.00", AED), 4, 4),
                new Event.Debit("E7", "ACC-001", Money.of("620.00", AED), 5, 2),
                new Event.Authorize("E8", "ACC-001", "Auth-B", Money.of("90.00", AED), 5, 5),
                new Event.Reverse("E9", "ACC-001", "E7", 6, 2),
                new Event.InstalmentCredit("E10", "ACC-002", Money.of("10.000", BHD), 3, 5, 5));
    }
}
