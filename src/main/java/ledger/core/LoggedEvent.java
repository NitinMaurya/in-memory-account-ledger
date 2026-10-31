package ledger.core;

/** A request as it was received, with what the ledger decided. Rejected requests are kept too. */
public record LoggedEvent(Event event, Outcome outcome) {}
