package ledger.core;

/** A currency and the number of decimal places its amounts are kept at. */
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

    /** Minor units per major unit: 100 fils per dirham, 1000 fils per dinar. */
    public long minorUnitsPerUnit() {
        long factor = 1;
        for (int i = 0; i < decimals; i++) {
            factor *= 10;
        }
        return factor;
    }
}
