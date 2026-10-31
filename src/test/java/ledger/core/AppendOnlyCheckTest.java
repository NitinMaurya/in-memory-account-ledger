package ledger.core;

import static ledger.core.AppendOnlyCheck.assertOnlyAppended;
import static ledger.core.Currency.AED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Proves the BR-11 check catches every way a recorded line could be altered. */
class AppendOnlyCheckTest {

    private static final Entry L1 = entry("L0001", "1200.00");
    private static final Entry L2 = entry("L0002", "-950.00");
    private static final Entry L3 = entry("L0003", "400.00");

    @Test
    void passesWhenRecordsAreOnlyAddedAtTheEnd() {
        assertDoesNotThrow(() -> assertOnlyAppended("entries", List.of(L1), List.of(L1, L2)));
    }

    @Test
    void failsWhenARecordIsRemoved() {
        assertThrows(AssertionError.class, () -> assertOnlyAppended("entries", List.of(L1, L2), List.of(L1)));
    }

    @Test
    void failsWhenARecordIsChanged() {
        Entry editedL1 = entry("L0001", "1100.00");
        assertThrows(AssertionError.class, () -> assertOnlyAppended("entries", List.of(L1), List.of(editedL1)));
    }

    @Test
    void failsWhenRecordsAreReordered() {
        assertThrows(AssertionError.class, () -> assertOnlyAppended("entries", List.of(L1, L2), List.of(L2, L1)));
    }

    @Test
    void failsWhenARecordIsInsertedInTheMiddle() {
        assertThrows(AssertionError.class,
                () -> assertOnlyAppended("entries", List.of(L1, L2), List.of(L1, L3, L2)));
    }

    private static Entry entry(String id, String amount) {
        return new Entry(id, "E", "ACC-001", Entry.Type.CREDIT, Money.of(amount, AED), 1, 1);
    }
}
