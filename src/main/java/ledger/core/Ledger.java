package ledger.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Ledger {

    public static final int FIRST_DAY = 1;
    public static final int LAST_DAY = 6;

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<LoggedEvent> log = new ArrayList<>();

    public Ledger(List<Account> accounts) {
        for (Account account : accounts) {
            this.accounts.put(account.id(), account);
        }
    }

    public Outcome apply(Event event) {
        Outcome outcome = rejectionFor(event)
                .<Outcome>map(Outcome.Rejected::new)
                .orElseGet(() -> execute(event));
        log.add(new LoggedEvent(event, outcome));
        return outcome;
    }

    private Optional<Outcome.Reason> rejectionFor(Event event) {
        Account account = accounts.get(event.account());
        if (account == null) {
            return Optional.of(Outcome.Reason.UNKNOWN_ACCOUNT);
        }
        if (!inWindow(event.bookingDay()) || !inWindow(event.valueDate())) {
            return Optional.of(Outcome.Reason.OUTSIDE_WINDOW);
        }
        Optional<Money> amount = amountOf(event);
        if (amount.isPresent() && amount.get().currency() != account.currency()) {
            return Optional.of(Outcome.Reason.CURRENCY_MISMATCH);
        }
        return Optional.empty();
    }

    private Outcome execute(Event event) {
        return switch (event) {
            case Event.Credit credit -> post(credit, Entry.Type.CREDIT, credit.amount());
            case Event.Debit debit -> post(debit, Entry.Type.DEBIT, debit.amount().negate());
            case Event.Authorize authorize -> authorize(authorize);
            // Not built yet: logged, no money moves.
            default -> new Outcome.Accepted();
        };
    }

    private static Optional<Money> amountOf(Event event) {
        return switch (event) {
            case Event.Credit credit -> Optional.of(credit.amount());
            case Event.Debit debit -> Optional.of(debit.amount());
            case Event.Authorize authorize -> Optional.of(authorize.amount());
            case Event.Settle settle -> Optional.of(settle.amount());
            case Event.InstalmentCredit instalments -> Optional.of(instalments.total());
            case Event.Reverse reverse -> Optional.empty();
        };
    }

    public void closeDay(int day) {
        if (!inWindow(day)) {
            throw new IllegalArgumentException("Day " + day + " is outside the window");
        }
        // Not built yet: overdraft fees and interest.
    }

    /** Sum of entries with value date on or before {@code day}, whenever they were booked. */
    public Money closingBalance(String account, int day) {
        Money balance = Money.zero(account(account).currency());
        for (Entry entry : entries) {
            if (entry.account().equals(account) && entry.valueDate() <= day) {
                balance = balance.plus(entry.amount());
            }
        }
        return balance;
    }

    /** Every posted entry, whatever its value date, minus the holds of active authorizations. */
    public Money availableBalance(String account) {
        Money balance = Money.zero(account(account).currency());
        for (Entry entry : entries) {
            if (entry.account().equals(account)) {
                balance = balance.plus(entry.amount());
            }
        }
        for (Authorization authorization : authorizations()) {
            if (authorization.account().equals(account) && authorization.status() == Authorization.Status.ACTIVE) {
                balance = balance.plus(authorization.requested().negate());
            }
        }
        return balance;
    }

    // Read-only copies, so nothing outside the ledger can change a recorded line.
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public List<LoggedEvent> eventLog() {
        return List.copyOf(log);
    }

    // Derived from the log every time, never stored, so a status is never edited.
    public List<Authorization> authorizations() {
        List<Authorization> authorizations = new ArrayList<>();
        for (LoggedEvent logged : log) {
            if (logged.event() instanceof Event.Authorize authorize) {
                Authorization.Status status = logged.outcome() instanceof Outcome.Accepted
                        ? Authorization.Status.ACTIVE
                        : Authorization.Status.DECLINED;
                Money nothingSettled = Money.zero(authorize.amount().currency());
                authorizations.add(new Authorization(
                        authorize.authId(), authorize.account(), authorize.amount(), status, nothingSettled));
            }
        }
        return authorizations;
    }

    public List<Money> dailyInterest(String account) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private Outcome authorize(Event.Authorize authorize) {
        Money availableAfterHold = availableBalance(authorize.account()).plus(authorize.amount().negate());
        if (availableAfterHold.isNegative()) {
            return new Outcome.Rejected(Outcome.Reason.INSUFFICIENT_AVAILABLE);
        }
        return new Outcome.Accepted();
    }

    private static boolean inWindow(int day) {
        return day >= FIRST_DAY && day <= LAST_DAY;
    }

    private Account account(String id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new IllegalArgumentException("unknown account " + id);
        }
        return account;
    }

    private Outcome post(Event event, Entry.Type type, Money signedAmount) {
        String entryId = "L%04d".formatted(entries.size() + 1);
        entries.add(new Entry(
                entryId, event.id(), event.account(), type, signedAmount, event.bookingDay(), event.valueDate()));
        return new Outcome.Accepted();
    }
}
