package ledger.core;

public record Authorization(String authId, String account, Money requested, Status status, Money settled) {

    public enum Status {
        ACTIVE,
        SETTLED,
        DECLINED
    }
}
