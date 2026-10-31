package ledger.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Ledger {

    public static final int FIRST_DAY = 1;
    public static final int LAST_DAY = 6;

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<LoggedEvent> log = new ArrayList<>();

    public Ledger(List<Account> accounts) {
        for (Account account : accounts) {
            this.accounts.put(account.id(), account);
        }
    }

    public Outcome apply(Event event) {
        Outcome outcome = switch (event) {
            case Event.Credit credit -> post(credit, Entry.Type.CREDIT, credit.amount());
            case Event.Debit debit -> post(debit, Entry.Type.DEBIT, debit.amount().negate());
            // Not built yet: logged, no money moves.
            default -> new Outcome.Accepted();
        };
        log.add(new LoggedEvent(event, outcome));
        return outcome;
    }

    public void closeDay(int day) {
        // Not built yet: overdraft fees and interest.
    }

    /** Sum of entries with value date on or before {@code day}, whenever they were booked. */
    public Money closingBalance(String account, int day) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Money availableBalance(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    // Read-only copies, so nothing outside the ledger can change a recorded line.
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public List<LoggedEvent> eventLog() {
        return List.copyOf(log);
    }

    public List<Authorization> authorizations() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<Money> dailyInterest(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private Outcome post(Event event, Entry.Type type, Money signedAmount) {
        String entryId = "L%04d".formatted(entries.size() + 1);
        entries.add(new Entry(
                entryId, event.id(), event.account(), type, signedAmount, event.bookingDay(), event.valueDate()));
        return new Outcome.Accepted();
    }
}
