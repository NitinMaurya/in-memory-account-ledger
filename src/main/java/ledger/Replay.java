package ledger;

import java.util.List;

/**
 * Replays events in the order given. Before applying an event booked on a later day, it closes
 * every day still open before that day. After the last event it closes the remaining days of the
 * window. Events are never re-sorted: one booked on a day that has already closed (E10) is simply
 * applied late.
 */
public final class Replay {

    private Replay() {}

    /** Replays the events, calling {@code afterEachStep} after every apply and every day close. */
    public static void run(Ledger ledger, List<Event> events, Runnable afterEachStep) {
        int lastClosed = Ledger.FIRST_DAY - 1;
        for (Event event : events) {
            while (lastClosed < event.bookingDay() - 1) {
                lastClosed++;
                ledger.closeDay(lastClosed);
                afterEachStep.run();
            }
            ledger.apply(event);
            afterEachStep.run();
        }
        while (lastClosed < Ledger.LAST_DAY) {
            lastClosed++;
            ledger.closeDay(lastClosed);
            afterEachStep.run();
        }
    }
}
