package ledger;

/** An account. Every account opens with a zero balance. */
public record Account(String id, Currency currency) {}
