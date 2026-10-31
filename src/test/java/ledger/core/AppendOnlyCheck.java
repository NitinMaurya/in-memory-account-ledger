package ledger.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

final class AppendOnlyCheck {

    private AppendOnlyCheck() {}

    /** What was there before must still be there, unchanged and in the same place. */
    static <T> void assertOnlyAppended(String what, List<T> before, List<T> after) {
        assertTrue(after.size() >= before.size(), () -> "BR-11: the " + what + " shrank");
        assertEquals(before, after.subList(0, before.size()),
                () -> "BR-11: an earlier record in the " + what + " was changed or removed");
    }
}
