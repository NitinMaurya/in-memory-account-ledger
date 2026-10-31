package ledger.core;

public record LoggedEvent(Event event, Outcome outcome) {}
