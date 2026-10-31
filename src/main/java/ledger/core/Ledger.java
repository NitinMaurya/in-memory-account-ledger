package ledger.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory, append-only account ledger.
 *
 * <p>Two commands change it: {@link #apply(Event)} and {@link #closeDay(int)}. Everything else
 * is a query, so every business rule can be checked from outside.
 */
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

    /** Applies one event. A business refusal comes back as {@link Outcome.Rejected} and is logged. */
    public Outcome apply(Event event) {
        Outcome outcome = switch (event) {
            case Event.Credit credit -> post(credit, Entry.Type.CREDIT, credit.amount());
            case Event.Debit debit -> post(debit, Entry.Type.DEBIT, debit.amount().negate());
            // Not built yet: logged, but no money moves. Each type gets its own rules and tests next.
            default -> new Outcome.Accepted();
        };
        log.add(new LoggedEvent(event, outcome));
        return outcome;
    }

    /** Ends a day: assesses overdraft fees; at the end of the window also capitalizes interest. */
    public void closeDay(int day) {
        // Nothing yet: overdraft fees and interest arrive with their own rules and tests.
    }

    /** Sum of all entries with value date on or before {@code day}, as known now. */
    public Money closingBalance(String account, int day) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Ledger balance minus the holds of active authorizations. */
    public Money availableBalance(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Every money movement, in the order recorded. Read-only. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** Every request received, accepted or rejected, in the order received. Read-only. */
    public List<LoggedEvent> eventLog() {
        return List.copyOf(log);
    }

    /** The current state of every authorization seen. */
    public List<Authorization> authorizations() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** The rounded interest accrual for each day of the window, Day 1 first. */
    public List<Money> dailyInterest(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Appends one entry for an event. The amount is signed: positive in, negative out. */
    private Outcome post(Event event, Entry.Type type, Money signedAmount) {
        String entryId = "L%04d".formatted(entries.size() + 1);
        entries.add(new Entry(
                entryId, event.id(), event.account(), type, signedAmount, event.bookingDay(), event.valueDate()));
        return new Outcome.Accepted();
    }
}
