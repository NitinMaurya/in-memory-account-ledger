package ledger.core;

/** The amount is signed: positive in, negative out. */
public record Entry(
        String entryId, String eventId, String account, Type type, Money amount, int bookingDay, int valueDate) {

    public enum Type {
        CREDIT,
        DEBIT,
        SETTLEMENT,
        REVERSAL,
        INSTALMENT,
        OVERDRAFT_FEE,
        FEE_REFUND,
        INTEREST
    }
}
