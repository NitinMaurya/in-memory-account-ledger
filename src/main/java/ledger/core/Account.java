package ledger.core;

/** An account. Every account opens with a zero balance. */
public record Account(String id, Currency currency) {}
