package ledger.core;

/** A refusal is a normal business result, not an exception. */
public sealed interface Outcome {

    record Accepted() implements Outcome {}

    record Rejected(Reason reason) implements Outcome {}

    enum Reason {
        INSUFFICIENT_AVAILABLE,
        UNKNOWN_AUTHORIZATION,
        ALREADY_SETTLED,
        SETTLEMENT_EXCEEDS_HOLD,
        DUPLICATE_AUTHORIZATION,
        UNKNOWN_ORIGINAL,
        ALREADY_REVERSED,
        NOT_REVERSIBLE,
        DUPLICATE_EVENT_ID,
        UNKNOWN_ACCOUNT,
        OUTSIDE_WINDOW,
        CURRENCY_MISMATCH
    }
}
