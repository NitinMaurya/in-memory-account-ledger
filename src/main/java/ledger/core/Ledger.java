package ledger.core;

import java.util.List;

/**
 * An in-memory, append-only account ledger.
 *
 * <p>Two commands change it: {@link #apply(Event)} and {@link #closeDay(int)}. Everything else
 * is a query, so every business rule can be checked from outside.
 */
public final class Ledger {

    public static final int FIRST_DAY = 1;
    public static final int LAST_DAY = 6;

    public Ledger(List<Account> accounts) {}

    /** Applies one event. A business refusal comes back as {@link Outcome.Rejected} and is logged. */
    public Outcome apply(Event event) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Ends a day: assesses overdraft fees; at the end of the window also capitalizes interest. */
    public void closeDay(int day) {
        throw new UnsupportedOperationException("not implemented yet");
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
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** Every request received, accepted or rejected, in the order received. Read-only. */
    public List<LoggedEvent> eventLog() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** The current state of every authorization seen. */
    public List<Authorization> authorizations() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /** The rounded interest accrual for each day of the window, Day 1 first. */
    public List<Money> dailyInterest(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
