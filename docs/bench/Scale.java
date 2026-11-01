import java.util.ArrayList;
import java.util.List;
import ledger.core.Account;
import ledger.core.Currency;
import ledger.core.Event;
import ledger.core.Ledger;
import ledger.core.Money;
import ledger.scenario.Replay;

/**
 * Times the replay as volume grows, for section 1 of ARCHITECTURE.pdf. Not part of the build.
 *
 * <p>A synthetic stream with the brief's proportions, scaled by k: 2k accounts and 2k + 1 requests
 * on each of the six days (about 12k requests), credits and debits, every second debit back-dated a
 * day. Run from the repository root:
 *
 * <pre>
 * mvn -q compile && javac -cp target/classes -d target/bench docs/bench/*.java
 * java -cp target/classes:target/bench Scale
 * </pre>
 */
public class Scale {

    public static void main(String[] args) {
        for (int k : new int[] {1, 10, 100, 300}) {
            List<Account> accounts = new ArrayList<>();
            for (int i = 0; i < 2 * k; i++) {
                accounts.add(new Account("ACC-" + i, Currency.AED));
            }
            List<Event> events = new ArrayList<>();
            int n = 0;
            for (int day = 1; day <= 6; day++) {
                for (int e = 0; e < 2 * k + 1; e++) {
                    String account = "ACC-" + (n % (2 * k));
                    events.add(n % 3 == 0
                            ? new Event.Credit("E" + n, account, Money.of("100.00", Currency.AED), day, day)
                            : new Event.Debit("E" + n, account, Money.of("40.00", Currency.AED), day,
                                    Math.max(1, day - (n % 2))));
                    n++;
                }
            }
            Ledger ledger = new Ledger(accounts);
            long start = System.nanoTime();
            Replay.run(ledger, events, () -> {});
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("%4dx  accounts %5d  requests %5d  entries %6d  %7d ms%n",
                    k, accounts.size(), events.size(), ledger.entries().size(), ms);
        }
    }
}
