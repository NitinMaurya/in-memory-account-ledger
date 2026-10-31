package ledger;

/** The current state of a card authorization, derived from the events received. */
public record Authorization(String authId, String account, Money requested, Status status, Money settled) {

    public enum Status {
        ACTIVE,
        SETTLED,
        DECLINED
    }
}
