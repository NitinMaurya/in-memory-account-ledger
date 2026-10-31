package ledger.scenario;

import java.util.List;
import java.util.function.IntConsumer;
import ledger.core.Event;
import ledger.core.Ledger;

/**
 * Replays events in the order given, the way a bank's days actually pass.
 *
 * <ul>
 *   <li>A day is closed once the stream moves past it: before an event booked on Day 3 is applied,
 *       Days 1 and 2 are closed (if still open).
 *   <li>After the last event, the remaining days up to the end of the window are closed.
 *   <li>Events are never re-sorted. An event booked on a day that has already closed (E10, booked
 *       Day 5 but listed after E9 on Day 6) is simply applied late, as a back-dated arrival.
 * </ul>
 */
public final class Replay {

    private Replay() {}

    /** {@code afterEachStep} runs after every event applied and every day closed, for invariant checks. */
    public static void run(Ledger ledger, List<Event> events, Runnable afterEachStep) {
        run(ledger, events, afterEachStep, day -> {});
    }

    /** As above, and {@code afterDayClosed} also runs with the day number each time a day closes. */
    public static void run(Ledger ledger, List<Event> events, Runnable afterEachStep, IntConsumer afterDayClosed) {
        int lastClosed = Ledger.FIRST_DAY - 1;
        for (Event event : events) {
            while (lastClosed < event.bookingDay() - 1) {
                lastClosed++;
                ledger.closeDay(lastClosed);
                afterEachStep.run();
                afterDayClosed.accept(lastClosed);
            }
            ledger.apply(event);
            afterEachStep.run();
        }
        while (lastClosed < Ledger.LAST_DAY) {
            lastClosed++;
            ledger.closeDay(lastClosed);
            afterEachStep.run();
            afterDayClosed.accept(lastClosed);
        }
    }
}
