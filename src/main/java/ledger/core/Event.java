package ledger.core;

public sealed interface Event {
    String id();

    String account();

    /** The day the bank recorded it. */
    int bookingDay();

    /** The day it counts for in balances, fees and interest. Can be earlier than the booking day. */
    int valueDate();

    record Credit(String id, String account, Money amount, int bookingDay, int valueDate) implements Event {}

    record Debit(String id, String account, Money amount, int bookingDay, int valueDate) implements Event {}

    record Authorize(String id, String account, String authId, Money amount, int bookingDay, int valueDate)
            implements Event {}

    record Settle(String id, String account, String authId, Money amount, int bookingDay, int valueDate)
            implements Event {}

    /** No amount: it is taken from the event being reversed. */
    record Reverse(String id, String account, String reversesEventId, int bookingDay, int valueDate)
            implements Event {}

    record InstalmentCredit(String id, String account, Money total, int instalments, int bookingDay, int valueDate)
            implements Event {}
}
