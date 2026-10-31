package ledger;

/**
 * One line in the ledger: money that actually moved. The amount is signed: positive in,
 * negative out. Entries are only ever appended, never changed.
 */
public record Entry(
        String entryId, String eventId, String account, Type type, Money amount, int bookingDay, int valueDate) {

    public enum Type {
        CREDIT,
        DEBIT,
        SETTLEMENT,
        REVERSAL,
        OVERDRAFT_FEE,
        INTEREST
    }
}
