import java.util.List;
import ledger.core.Account;
import ledger.core.Currency;
import ledger.core.Event;
import ledger.core.Ledger;
import ledger.core.Money;

/**
 * The behaviours ARCHITECTURE.pdf quotes that are limitations, not rules, so they have no test.
 * Not part of the build. Run from the repository root:
 *
 * <pre>
 * mvn -q compile && javac -cp target/classes -d target/bench docs/bench/*.java
 * java -cp target/classes:target/bench Probe
 * </pre>
 */
public class Probe {

    public static void main(String[] args) {
        Ledger ledger = withThousand();
        ledger.apply(new Event.Authorize("AU1", "A", "X", aed("300.00"), 1, 1));
        closeAllDays(ledger);
        System.out.println("A hold never settled, after Day 6: available " + ledger.availableBalance("A", 6)
                + ", ledger " + ledger.closingBalance("A", 6) + ", " + ledger.authorizations().get(0).status());

        ledger = withThousand();
        ledger.apply(new Event.Authorize("AU1", "A", "X", aed("100.00"), 1, 1));
        System.out.println("Voiding an authorization with a reversal: "
                + ledger.apply(new Event.Reverse("R1", "A", "AU1", 1, 1)));

        ledger = withThousand();
        ledger.apply(new Event.Authorize("AU1", "A", "X", aed("900.00"), 1, 1));
        ledger.apply(new Event.Debit("D1", "A", aed("800.00"), 1, 1));
        System.out.println("Settling with available at " + ledger.availableBalance("A", 1) + ": "
                + ledger.apply(new Event.Settle("S1", "A", "X", aed("900.00"), 1, 1)));

        ledger = withThousand();
        System.out.println("A plain debit of 1,000,000.00: "
                + ledger.apply(new Event.Debit("D1", "A", aed("1000000.00"), 1, 1))
                + ", ledger " + ledger.closingBalance("A", 1));
    }

    private static Ledger withThousand() {
        Ledger ledger = new Ledger(List.of(new Account("A", Currency.AED)));
        ledger.apply(new Event.Credit("C1", "A", aed("1000.00"), 1, 1));
        return ledger;
    }

    private static void closeAllDays(Ledger ledger) {
        for (int day = Ledger.FIRST_DAY; day <= Ledger.LAST_DAY; day++) {
            ledger.closeDay(day);
        }
    }

    private static Money aed(String amount) {
        return Money.of(amount, Currency.AED);
    }
}
