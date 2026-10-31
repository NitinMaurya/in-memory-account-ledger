package ledger.core;

public enum Currency {
    AED(2),
    BHD(3);

    private final int decimals;

    Currency(int decimals) {
        this.decimals = decimals;
    }

    public int decimals() {
        return decimals;
    }
}
