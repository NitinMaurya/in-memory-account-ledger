package ledger.core;

/** The result of applying an event. A refusal is a normal result, not an exception. */
public sealed interface Outcome {

    record Accepted() implements Outcome {}

    record Rejected(Reason reason) implements Outcome {}

    enum Reason {
        INSUFFICIENT_AVAILABLE,
        UNKNOWN_AUTHORIZATION,
        ALREADY_SETTLED,
        SETTLEMENT_EXCEEDS_HOLD,
        UNKNOWN_ORIGINAL,
        ALREADY_REVERSED,
        DUPLICATE_EVENT_ID,
        UNKNOWN_ACCOUNT,
        CURRENCY_MISMATCH
    }
}
