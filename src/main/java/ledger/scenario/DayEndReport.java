package ledger.scenario;

import java.util.ArrayList;
import java.util.List;
import ledger.core.Account;
import ledger.core.Authorization;
import ledger.core.Entry;
import ledger.core.Event;
import ledger.core.Ledger;
import ledger.core.LoggedEvent;
import ledger.core.Money;
import ledger.core.Outcome;

/**
 * Two views of the same replay. The day-end log shows each day as the bank saw it that night. The
 * final view shows every day as it ends up once back-dated entries (E7, E9, E10) are counted. The
 * ledger section lists the entries behind both.
 */
public final class DayEndReport {

    private final List<Account> accounts;
    private final Ledger ledger;
    private final List<List<String>> balanceRows = new ArrayList<>();
    private final List<List<String>> activityRows = new ArrayList<>();
    private final List<String> authIds = new ArrayList<>();
    private int entriesSeen;
    private int requestsSeen;

    private DayEndReport(List<Account> accounts) {
        this.accounts = accounts;
        this.ledger = new Ledger(accounts);
    }

    public static String forTheBrief() {
        DayEndReport report = new DayEndReport(BriefScenario.accounts());
        Replay.run(report.ledger, BriefScenario.events(), () -> {}, report::dayClosed);
        return report.render();
    }

    private void dayClosed(int day) {
        for (Authorization a : ledger.authorizations()) {
            if (!authIds.contains(a.authId())) {
                authIds.add(a.authId());
            }
        }
        List<String> balances = new ArrayList<>(List.of(String.valueOf(day)));
        for (Account account : accounts) {
            balances.add(amount(ledger.closingBalance(account.id(), day)));
            balances.add(amount(ledger.availableBalance(account.id())));
        }
        balances.add(String.join("; ", authorizationStates()));
        balanceRows.add(balances);
        activityRows.add(List.of(String.valueOf(day), orNone(sinceLastClose(Entry.Type.OVERDRAFT_FEE)),
                orNone(sinceLastClose(Entry.Type.FEE_REFUND)), orNone(refusalsSinceLastClose())));
        entriesSeen = ledger.entries().size();
        requestsSeen = ledger.eventLog().size();
    }

    private String render() {
        StringBuilder out = new StringBuilder();
        out.append("DAY-END LOG: each day as it was when it closed\n\n");

        List<String> balanceHeaders = new ArrayList<>(List.of("Day"));
        StringBuilder balanceAlign = new StringBuilder("R");
        for (Account account : accounts) {
            balanceHeaders.add(account.id() + " closing (" + account.currency() + ")");
            balanceHeaders.add("available");
            balanceAlign.append("RR");
        }
        balanceHeaders.add("Authorizations");
        balanceAlign.append('L');
        out.append(table(balanceHeaders, balanceRows, balanceAlign.toString())).append('\n');
        out.append(table(List.of("Day", "Fees charged (AED 25.00 each)", "Fees refunded", "Errors"), activityRows, "RLLL"))
                .append('\n');

        out.append("FINAL VIEW: each day by value date, after every event\n\n");
        out.append(table(finalHeaders(), finalRows(), "R" + "RRRR".repeat(accounts.size()))).append('\n');

        List<String> credited = new ArrayList<>();
        List<String> finals = new ArrayList<>();
        for (Account account : accounts) {
            credited.add(account.id() + " " + sum(ledger.dailyInterest(account.id())));
            finals.add(account.id() + " " + ledger.closingBalance(account.id(), Ledger.LAST_DAY));
        }
        out.append("Interest credited on Day 6 (included in Day 6): ").append(String.join(", ", credited)).append('\n');
        out.append("Final balances: ").append(String.join(", ", finals)).append('\n');

        out.append("\nLEDGER: every entry in the order it was written; nothing is ever changed or deleted\n\n");
        out.append(table(List.of("Entry", "From", "Account", "Type", "Amount", "Booked", "Value"), ledgerRows(), "LLLLRRR"));
        return out.toString();
    }

    private List<List<String>> ledgerRows() {
        List<List<String>> rows = new ArrayList<>();
        for (Entry entry : ledger.entries()) {
            String amount = amount(entry.amount());
            rows.add(List.of(entry.entryId(), entry.eventId(), entry.account(), entry.type().name(),
                    entry.amount().isNegative() ? amount : "+" + amount,
                    String.valueOf(entry.bookingDay()), String.valueOf(entry.valueDate())));
        }
        return rows;
    }

    private List<String> finalHeaders() {
        List<String> headers = new ArrayList<>(List.of("Day"));
        for (Account account : accounts) {
            headers.add(account.id() + " closing (" + account.currency() + ")");
            headers.add("fee");
            headers.add("refund");
            headers.add("interest");
        }
        return headers;
    }

    private List<List<String>> finalRows() {
        List<List<String>> rows = new ArrayList<>();
        for (int day = Ledger.FIRST_DAY; day <= Ledger.LAST_DAY; day++) {
            List<String> row = new ArrayList<>(List.of(String.valueOf(day)));
            for (Account account : accounts) {
                row.add(amount(ledger.closingBalance(account.id(), day)));
                row.add(amountOf(Entry.Type.OVERDRAFT_FEE, account.id(), day));
                row.add(amountOf(Entry.Type.FEE_REFUND, account.id(), day));
                row.add(amount(ledger.dailyInterest(account.id()).get(day - 1)));
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> authorizationStates() {
        List<String> states = new ArrayList<>();
        for (String authId : authIds) {
            ledger.authorizations().stream().filter(a -> a.authId().equals(authId)).findFirst().ifPresent(a -> {
                String amounts = a.status() == Authorization.Status.SETTLED
                        ? amount(a.settled()) + " of " + amount(a.requested())
                        : amount(a.requested());
                states.add(a.authId() + " " + a.status() + " " + amounts);
            });
        }
        return states.isEmpty() ? List.of("none") : states;
    }

    private List<String> sinceLastClose(Entry.Type type) {
        List<String> days = new ArrayList<>();
        for (Entry entry : ledger.entries().subList(entriesSeen, ledger.entries().size())) {
            if (entry.type() == type) {
                days.add("Day " + entry.valueDate());
            }
        }
        return days.isEmpty() ? days : List.of("for " + String.join(", ", days));
    }

    private List<String> refusalsSinceLastClose() {
        List<String> refusals = new ArrayList<>();
        for (LoggedEvent logged : ledger.eventLog().subList(requestsSeen, ledger.eventLog().size())) {
            if (logged.outcome() instanceof Outcome.Rejected rejected) {
                refusals.add(logged.event().id() + " " + subject(logged.event()) + ": " + rejected.reason());
            }
        }
        return refusals;
    }

    private String amountOf(Entry.Type type, String account, int day) {
        return ledger.entries().stream()
                .filter(e -> e.type() == type && e.account().equals(account) && e.valueDate() == day)
                .map(e -> amount(e.amount().minorUnits() < 0 ? e.amount().negate() : e.amount()))
                .findFirst()
                .orElse("-");
    }

    private static String subject(Event event) {
        return switch (event) {
            case Event.Authorize authorize -> authorize.authId();
            case Event.Settle settle -> settle.authId();
            default -> event.account();
        };
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money::plus).orElseThrow();
    }

    private static String amount(Money money) {
        return money.toString().substring(money.currency().name().length() + 1);
    }

    private static String orNone(List<String> items) {
        return items.isEmpty() ? "none" : String.join(", ", items);
    }

    /** Pads every column to its widest cell; {@code align} has one letter per column, L or R. */
    private static String table(List<String> headers, List<List<String>> rows, String align) {
        int[] widths = new int[headers.size()];
        for (int c = 0; c < headers.size(); c++) {
            widths[c] = headers.get(c).length();
            for (List<String> row : rows) {
                widths[c] = Math.max(widths[c], row.get(c).length());
            }
        }
        StringBuilder out = new StringBuilder();
        appendRow(out, headers, widths, align);
        List<String> rule = new ArrayList<>();
        for (int width : widths) {
            rule.add("-".repeat(width));
        }
        appendRow(out, rule, widths, align);
        for (List<String> row : rows) {
            appendRow(out, row, widths, align);
        }
        return out.toString();
    }

    private static void appendRow(StringBuilder out, List<String> cells, int[] widths, String align) {
        List<String> padded = new ArrayList<>();
        for (int c = 0; c < cells.size(); c++) {
            String format = align.charAt(c) == 'R' ? "%" + widths[c] + "s" : "%-" + widths[c] + "s";
            padded.add(format.formatted(cells.get(c)));
        }
        out.append(String.join("   ", padded).stripTrailing()).append('\n');
    }
}
