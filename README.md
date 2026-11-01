# In-Memory Account Ledger

An in-memory, append-only account ledger core, written in plain Java. It handles value-dated
entries, card authorization holds, daily overdraft fees and daily interest, and replays a fixed
six-day event stream, printing the state of each account at the end of every day.

There is no web layer, no database and no UI. The ledger is exercised entirely through its test
suite and a replay program. The assessment brief is in [`brief.md`](brief.md).

> **Status:** complete. The ledger, its tests, the replay program, the written analysis and the
> architecture and trade-offs document ([`ARCHITECTURE.pdf`](ARCHITECTURE.pdf)) are done. The commit history and
> [`WORKLOG.md`](WORKLOG.md) record the work as it happened.

## Quick start

Needs JDK 21+ and Maven 3.9+ (`java -version`, `mvn -v`). From a terminal:

```bash
git clone https://github.com/NitinMaurya/in-memory-account-ledger.git
cd in-memory-account-ledger

# 1. Run the test suite: expect "Tests run: 111, Failures: 0" and BUILD SUCCESS
mvn test

# 2. Replay the brief's event stream and print the report
mvn -q compile
java -cp target/classes ledger.scenario.Main

# 3. Run the one deliberately failing test: expect "Tests run: 1, Failures: 1" and BUILD FAILURE
mvn test -Dgroups=known-gap -DexcludedGroups=
```

The same commands work on Windows (PowerShell or cmd). Details: [running the replay](#running-the-replay),
[reading the output](#reading-the-output), [running the tests](#running-the-tests).

---

## Contents

- [Quick start](#quick-start)
- [What it does](#what-it-does)
- [Design principles](#design-principles)
- [Getting started](#getting-started)
- [Running the replay](#running-the-replay)
- [Reading the output](#reading-the-output)
- [Running the tests](#running-the-tests)
- [Project layout](#project-layout)
- [Documentation](#documentation)
- [Use of AI assistance](#use-of-ai-assistance)

---

## What it does

The ledger models two accounts in different currencies, AED (2 decimal places) and BHD
(3 decimal places), over a six-day window, and supports:

- **Credits and debits.** Each carries a *booking day* (when it was recorded) and a *value date*
  (the day it counts for). The two can differ, so an entry booked today can change balances for
  days that have already closed.
- **Card authorizations and settlements.** An authorization places a hold that reduces the
  *available* balance without moving money. A settlement moves the money and releases the hold.
- **Reversals.** A mistaken entry is corrected by appending a compensating entry. Nothing is ever
  deleted.
- **Overdraft fees,** assessed per account per day from that day's closing balance, including days
  re-checked after a back-dated entry. When a reversal makes a charged day positive again, its fee is
  **refunded** with a new entry, so the books show the fee and the refund side by side.
- **Daily interest** on positive balances, rounded per day and capitalized once at the end of the
  window. The capitalized amount always equals the sum of the rounded daily amounts.
- **Instalment splits** that conserve money exactly at the currency's precision.

Every request is either applied or **refused with a reason** (for example an unknown authorization
or insufficient available balance), and refusals are recorded too.

## Design principles

- **Append-only.** Records are only ever added. Corrections are new, linked records, so any past
  balance can be reconstructed and every change can be explained.
- **Store facts, derive state.** Balances, hold totals and authorization states are computed from
  the recorded facts rather than stored and edited. A back-dated entry is one new record, not a
  rewrite of history.
- **Exact money.** Amounts are whole minor units (fils) in a `long`, rounded explicitly. There is
  no floating-point arithmetic on money.
- **Refusals are recorded.** A rejected request is kept, with its reason, so the system can always
  answer "why was this declined?". It just never moves money.
- **Small and explainable.** The goal is a core where every number can be derived by hand, not a
  feature-rich framework.

The judgment calls behind each of these are in [`AMBIGUITIES.md`](AMBIGUITIES.md).

## Getting started

### Requirements

| Tool | Version |
|---|---|
| JDK | 21 or newer |
| Maven | 3.9 or newer |

There are no runtime dependencies. JUnit 5 is the only test dependency.

### Clone and build

```bash
git clone https://github.com/NitinMaurya/in-memory-account-ledger.git
cd in-memory-account-ledger
mvn -q compile
```

## Running the replay

```bash
mvn -q compile
java -cp target/classes ledger.scenario.Main
```

In an IDE (IntelliJ IDEA, or VS Code with the Java extension), open the folder as a Maven project
and run `ledger.scenario.Main`.

This replays the brief's event stream (E1 to E10, in the order given) and prints:

```
DAY-END LOG: each day as it was when it closed

Day   ACC-001 closing (AED)   available   ACC-002 closing (BHD)   available   Authorizations
---   ---------------------   ---------   ---------------------   ---------   ------------------------------------------------------
  1                  250.00      250.00                   0.000       0.000   none
  2                  250.00       50.00                   0.000       0.000   Auth-A ACTIVE 200.00
  3                  650.00      450.00                   0.000       0.000   Auth-A ACTIVE 200.00
  4                  465.00      465.00                   0.000       0.000   Auth-A SETTLED 185.00 of 200.00
  5                 -230.00     -230.00                   0.000       0.000   Auth-A SETTLED 185.00 of 200.00; Auth-B DECLINED 90.00
  6                  466.03      466.03                  10.008      10.008   Auth-A SETTLED 185.00 of 200.00; Auth-B DECLINED 90.00

Day   Fees charged (AED 25.00 each)   Fees refunded             Errors
---   -----------------------------   -----------------------   ---------------------------------
  1   none                            none                      none
  2   none                            none                      none
  3   none                            none                      none
  4   none                            none                      E6 Auth-Z: UNKNOWN_AUTHORIZATION
  5   for Day 2, Day 4, Day 5         none                      E8 Auth-B: INSUFFICIENT_AVAILABLE
  6   none                            for Day 2, Day 4, Day 5   none

FINAL VIEW: each day by value date, after every event

Day   ACC-001 closing (AED)     fee   refund   interest   ACC-002 closing (BHD)   fee   refund   interest
---   ---------------------   -----   ------   --------   ---------------------   ---   ------   --------
  1                  250.00       -        -       0.10                   0.000     -        -      0.000
  2                  250.00   25.00    25.00       0.10                   0.000     -        -      0.000
  3                  650.00       -        -       0.26                   0.000     -        -      0.000
  4                  465.00   25.00    25.00       0.19                   0.000     -        -      0.000
  5                  465.00   25.00    25.00       0.19                  10.000     -        -      0.004
  6                  466.03       -        -       0.19                  10.008     -        -      0.004

Interest credited on Day 6 (included in Day 6): ACC-001 AED 1.03, ACC-002 BHD 0.008
Final balances: ACC-001 AED 466.03, ACC-002 BHD 10.008

LEDGER: every entry in the order it was written; nothing is ever changed or deleted

Entry   From                Account   Type              Amount   Booked   Value
-----   -----------------   -------   -------------   --------   ------   -----
L0001   E1                  ACC-001   CREDIT          +1200.00        1       1
L0002   E2                  ACC-001   DEBIT            -950.00        1       1
L0003   E4                  ACC-001   CREDIT           +400.00        3       3
L0004   E5                  ACC-001   SETTLEMENT       -185.00        4       4
L0005   E7                  ACC-001   DEBIT            -620.00        5       2
L0006   FEE-ACC-001-D2      ACC-001   OVERDRAFT_FEE     -25.00        5       2
L0007   FEE-ACC-001-D4      ACC-001   OVERDRAFT_FEE     -25.00        5       4
L0008   FEE-ACC-001-D5      ACC-001   OVERDRAFT_FEE     -25.00        5       5
L0009   E9                  ACC-001   REVERSAL         +620.00        6       2
L0010   REFUND-ACC-001-D2   ACC-001   FEE_REFUND        +25.00        6       2
L0011   REFUND-ACC-001-D4   ACC-001   FEE_REFUND        +25.00        6       4
L0012   REFUND-ACC-001-D5   ACC-001   FEE_REFUND        +25.00        6       5
L0013   E10                 ACC-002   INSTALMENT        +3.333        5       5
L0014   E10                 ACC-002   INSTALMENT        +3.333        5       5
L0015   E10                 ACC-002   INSTALMENT        +3.334        5       5
L0016   INT-ACC-001-D6      ACC-001   INTEREST           +1.03        6       6
L0017   INT-ACC-002-D6      ACC-002   INTEREST          +0.008        6       6
```

## Reading the output

The report has **two views of the same replay**, because back-dated entries change a day's numbers
after it has closed, followed by **the ledger itself**.

**1. Day-end log: each day as it was when it closed.** What the bank knew that night.

- *closing*: the sum of every entry value-dated on or before that day.
- *available*: that day's ledger balance minus the holds of active authorizations. A back-dated
  entry already posted counts; money dated later doesn't.
- *Authorizations*: every authorization's state that night.
- *Fees charged*, *Fees refunded* and *Errors*: what was charged, refunded or refused since the
  previous close.

**2. Final view: each day by value date, after every event.** The corrected history. Each row
shows the day's closing balance, the overdraft fee and any refund dated that day, and that day's
rounded interest.

**3. Ledger: every entry in the order it was written.** *From* is the event that caused the entry,
or the fee, refund or interest the ledger generated itself. *Booked* is the booking day the request stated (for
fees, refunds and interest, the day the ledger created them), *Value* the day it counts for. E3, E6 and E8 have no entries: E3 only placed a hold, and E6 and E8
were refused.

What to look for:

| Where | What it shows |
|---|---|
| Day 2: closing 250.00, available 50.00 | Auth-A's 200.00 hold reduces *available*, never the ledger |
| Day 4: Auth-A SETTLED 185.00 of 200.00; error E6 | A partial capture is accepted and releases the whole hold (available back to 465.00); a settlement for unknown Auth-Z is refused and moves no money |
| Day 5: closing -230.00; fees for Day 2, Day 4, Day 5; error E8 | E7 (booked Day 5, dated Day 2) turns Days 2 and 4 negative after they closed, so both are charged at once, and Day 5 at its close; Auth-B is declined because available is already negative |
| Day 5: ACC-002 still 0.000 in the day-end log | E10 is booked Day 5 but arrives after Day 5 closed (it is listed after E9), so it wasn't there that night |
| Day 6: fees refunded for Day 2, Day 4, Day 5 | E9 reverses E7, so those days are no longer negative; each fee is refunded by a new entry dated the fee's own day |
| Final view, Days 2, 4 and 5: fee 25.00, refund 25.00 | Three fees and three refunds, net zero: the balances are back to their pre-E7 values (250.00, 650.00, 465.00, 465.00) |
| Ledger: L0005 to L0012 | The back-dated debit, its three fees (booked Day 5), the reversal and three refunds (booked Day 6) all stay on the books; the reversal and refunds carry the value dates of what they undo |
| Final balances: ACC-001 466.03, ACC-002 10.008 | 465.00 plus 1.03 interest (0.10 + 0.10 + 0.26 + 0.19 + 0.19 + 0.19); 10.000 plus 0.004 on each of Days 5 and 6 |

Every figure can be checked by hand from the rules. [`NUMBERS.md`](NUMBERS.md) shows the
arithmetic, and [`REJECTED.md`](REJECTED.md) explains why some of the brief's acceptance criteria
expect different numbers.

## Running the tests

```bash
mvn test                                    # every test except the known gap: 111 tests, all pass
mvn test -Dtest=OverdraftFeeTest            # one test class
mvn test -Dtest='OverdraftFeeTest#p1_*'     # matching methods in one class
```

The suite is organised in layers:

| Layer | What it checks |
|---|---|
| **Business-rule invariants** (`BusinessRuleInvariantsTest`) | Rules that must hold at every moment, checked after **every step** of the replay: nothing recorded is ever changed, closing balance is the sum by value date, never two fees for one day, every settlement fits its hold, every split adds up, and more |
| **Rule tests** (`AccountsTest`, `AuthorizationTest`, `SettlementTest`, `ReversalTest`, `OverdraftFeeTest`, `InterestTest`, `InstalmentTest`, …) | Each rule with hand-worked numbers, including negative cases named after the mistake they rule out (for example `…CountsFromItsValueDateNotItsBookingDay`) |
| **Scenario and acceptance** (`BriefScenarioTest`, `AcceptanceCriteriaTest`) | The brief's replay end to end, and one test per acceptance criterion: five proven true, three proven wrong |

Most test names carry the business-rule ID they enforce (`br8_…`), matching
[`BUSINESS-RULES.md`](BUSINESS-RULES.md).

`mvn test` runs every test **except one**: the suite has a single test that **fails on purpose**.
It is tagged `known-gap` and left out of the default run, so `mvn test` stays a clear pass/fail
signal. It is not disabled. Run it on its own with:

```bash
mvn test -Dgroups=known-gap -DexcludedGroups=
```

It shows that the ledger accepts a back-dated debit with no approver and no reason code: a
maker-checker control the design does not have. The inline comments in
[`KnownGapBackDatedDebitApprovalTest`](src/test/java/ledger/core/KnownGapBackDatedDebitApprovalTest.java)
explain what it reveals and why the control is not built.

## Project layout

```
.
├── pom.xml                         Maven build (Java 21, JUnit 5)
├── src/main/java/ledger/core/      The ledger: general mechanics, plus the brief's constants
├── src/main/java/ledger/scenario/  The brief's accounts and events, the replay and the report
├── src/test/java/ledger/core/      Business-rule invariants and rule tests
├── src/test/java/ledger/scenario/  The brief's replay, the acceptance criteria and the report
├── brief.md                        The assessment brief
├── README.md                       This file
├── BUSINESS-RULES.md               The business rules, each traced to the test that enforces it
├── AMBIGUITIES.md                  Every ambiguity found and how it was resolved
├── REJECTED.md                     The acceptance criteria refused, and approaches abandoned
├── NUMBERS.md                      Every constant and why it has that value
├── ARCHITECTURE.pdf                Part 2: architecture and trade-offs (source: docs/architecture.html)
├── docs/bench/                     Programs behind the PDF's measurements (not part of the build)
└── WORKLOG.md                      Timestamped log of the work, written as it happened
```

## Documentation

| Document | Purpose | Status |
|---|---|---|
| [`brief.md`](brief.md) | The assessment brief as received | ✅ |
| [`README.md`](README.md) | Overview, how to run it and how to read the output | ✅ |
| [`BUSINESS-RULES.md`](BUSINESS-RULES.md) | The 33 business rules, each traced to the test that enforces it | ✅ |
| [`AMBIGUITIES.md`](AMBIGUITIES.md) | Every ambiguity found and how it was resolved | ✅ |
| [`REJECTED.md`](REJECTED.md) | Incorrect acceptance criteria refused, with reasons, and approaches abandoned | ✅ |
| [`NUMBERS.md`](NUMBERS.md) | Every constant used, and why that value and not half it | ✅ |
| [`WORKLOG.md`](WORKLOG.md) | Timestamped record of the work as it happened | ✅ |
| Architecture & trade-offs (PDF) | Scale, value dating in production, authorization lifecycle, scope cuts | [`ARCHITECTURE.pdf`](ARCHITECTURE.pdf) (source: [`docs/architecture.html`](docs/architecture.html)) |

## Use of AI assistance

This project is built with the help of an AI assistant (Claude). Commits it contributed to carry
a `Co-Authored-By` trailer, and [`WORKLOG.md`](WORKLOG.md) notes where it was used. Every design
decision and every number in the output is mine to explain.
