package ledger.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DayEndReportTest {

    private final String report = DayEndReport.forTheBrief();

    @Test
    void showsBothFinalBalances() {
        assertTrue(report.contains("Final balances: ACC-001 AED 466.03, ACC-002 BHD 10.008"), report);
    }

    @Test
    void showsBothRefusalsAsErrorsOnTheDayTheyHappened() {
        assertTrue(rowFor("DAY-END LOG", "4", "UNKNOWN_AUTHORIZATION").contains("E6 Auth-Z"), report);
        assertTrue(rowFor("DAY-END LOG", "5", "INSUFFICIENT_AVAILABLE").contains("E8 Auth-B"), report);
    }

    @Test
    void showsTheThreeFeesFoundOnDay5() {
        assertTrue(rowFor("DAY-END LOG", "5", "for Day 2, Day 4, Day 5").startsWith("  5"), report);
    }

    @Test
    void showsTheThreeRefundsMadeOnDay6() {
        assertTrue(rowFor("DAY-END LOG", "6", "for Day 2, Day 4, Day 5").startsWith("  6"), report);
    }

    @Test
    void showsDay2AsItWasThatNightAndAsItEndedUp() {
        assertTrue(rowFor("DAY-END LOG", "2", "Auth-A ACTIVE").contains("250.00"), report);
        assertTrue(rowFor("DAY-END LOG", "5", "INSUFFICIENT").contains("for Day 2, Day 4, Day 5"), report);
        assertTrue(rowFor("FINAL VIEW", "2", "250.00").contains("25.00"), report);
    }

    @Test
    void printsTheLedgerEveryEntryInTheOrderItWasWritten() {
        String ledger = report.substring(report.indexOf("LEDGER"));
        long rows = Arrays.stream(ledger.split("\n")).filter(line -> line.startsWith("L0")).count();
        assertEquals(17, rows, ledger);
        assertTrue(ledger.contains("L0001   E1"), ledger);
        assertTrue(ledgerRow("L0005").matches("L0005 +E7 +ACC-001 +DEBIT +-620\\.00 +5 +2"), ledger);
        assertTrue(ledgerRow("L0006").matches("L0006 +FEE-ACC-001-D2 +ACC-001 +OVERDRAFT_FEE +-25\\.00 +5 +2"), ledger);
        assertTrue(ledgerRow("L0010").matches("L0010 +REFUND-ACC-001-D2 +ACC-001 +FEE_REFUND +\\+25\\.00 +6 +2"), ledger);
        assertTrue(ledgerRow("L0015").matches("L0015 +E10 +ACC-002 +INSTALMENT +\\+3\\.334 +5 +5"), ledger);
        assertTrue(ledgerRow("L0017").matches("L0017 +INT-ACC-002-D6 +ACC-002 +INTEREST +\\+0\\.008 +6 +6"), ledger);
    }

    private String ledgerRow(String entryId) {
        return Arrays.stream(report.substring(report.indexOf("LEDGER")).split("\n"))
                .filter(line -> line.startsWith(entryId))
                .findFirst()
                .orElse("");
    }

    /** The first row in a section that starts with the day number and contains the given text. */
    private String rowFor(String section, String day, String text) {
        String from = report.substring(report.indexOf(section));
        return Arrays.stream(from.split("\n"))
                .filter(line -> line.strip().startsWith(day + " ") && line.contains(text))
                .findFirst()
                .orElse("");
    }
}
