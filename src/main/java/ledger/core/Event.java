package ledger.core;

/**
 * Something that is asked of the ledger. Every event carries a unique id, the account it
 * concerns, the day it was booked and the day it counts for (its value date).
 */
public sealed interface Event {
    String id();

    String account();

    int bookingDay();

    int valueDate();

    /** Money paid into the account. */
    record Credit(String id, String account, Money amount, int bookingDay, int valueDate) implements Event {}

    /** Money paid out of the account. */
    record Debit(String id, String account, Money amount, int bookingDay, int valueDate) implements Event {}

    /** A card authorization: asks to hold an amount without moving it. */
    record Authorize(String id, String account, String authId, Money amount, int bookingDay, int valueDate)
            implements Event {}

    /** A card settlement: takes money for an earlier authorization and ends its hold. */
    record Settle(String id, String account, String authId, Money amount, int bookingDay, int valueDate)
            implements Event {}

    /** Cancels an earlier event by recording the opposite amount. The amount comes from the original. */
    record Reverse(String id, String account, String reversesEventId, int bookingDay, int valueDate)
            implements Event {}

    /** A credit paid in as several instalments that together equal the total. */
    record InstalmentCredit(String id, String account, Money total, int instalments, int bookingDay, int valueDate)
            implements Event {}
}
