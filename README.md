# In-Memory Account Ledger

An in-memory, append-only account ledger core, written in plain Java. It handles value-dated
entries, card authorization holds, daily overdraft fees and daily interest, and replays a fixed
six-day event stream, printing the state of each account at the end of every day.

There is no web layer, no database and no UI. The ledger is exercised entirely through its test
suite and a replay program. The assessment brief is in [`brief.md`](brief.md).

> **Status:** the ledger, its tests, the replay program and the written analysis are complete. The
> architecture and trade-offs document (Part 2) is still to come. The commit history and
> [`WORKLOG.md`](WORKLOG.md) record the work as it happened.

---

## Contents

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
  re-checked after a back-dated entry.
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
  6                  390.93      390.93                  10.008      10.008   Auth-A SETTLED 185.00 of 200.00; Auth-B DECLINED 90.00

Day   Fees charged (AED 25.00 each)   Errors
---   -----------------------------   ---------------------------------
  1   none                            none
  2   none                            none
  3   none                            none
  4   none                            E6 Auth-Z: UNKNOWN_AUTHORIZATION
  5   for Day 2, Day 4, Day 5         E8 Auth-B: INSUFFICIENT_AVAILABLE
  6   none                            none

FINAL VIEW: each day by value date, after every event

Day   ACC-001 closing (AED)     fee   interest   ACC-002 closing (BHD)   fee   interest
---   ---------------------   -----   --------   ---------------------   ---   --------
  1                  250.00       -       0.10                   0.000     -      0.000
  2                  225.00   25.00       0.09                   0.000     -      0.000
  3                  625.00       -       0.25                   0.000     -      0.000
  4                  415.00   25.00       0.17                   0.000     -      0.000
  5                  390.00   25.00       0.16                  10.000     -      0.004
  6                  390.93       -       0.16                  10.008     -      0.004

Interest credited on Day 6 (included in Day 6): ACC-001 AED 0.93, ACC-002 BHD 0.008
Final balances: ACC-001 AED 390.93, ACC-002 BHD 10.008
```

## Reading the output

The report has **two views of the same replay**, because back-dated entries change a day's numbers
after it has closed.

**1. Day-end log: each day as it was when it closed.** What the bank knew that night.

- *closing*: the sum of every entry value-dated on or before that day.
- *available*: every posted entry minus the holds of active authorizations.
- *Authorizations*: every authorization's state that night.
- *Fees charged* and *Errors*: what was charged or refused since the previous close.

**2. Final view: each day by value date, after every event.** The corrected history. Each row
shows the day's closing balance, the overdraft fee dated that day, and that day's rounded interest.

What to look for:

| Where | What it shows |
|---|---|
| Day 2: closing 250.00, available 50.00 | Auth-A's 200.00 hold reduces *available*, never the ledger |
| Day 4: Auth-A SETTLED 185.00 of 200.00; error E6 | A partial capture is accepted and releases the whole hold (available back to 465.00); a settlement for unknown Auth-Z is refused and moves no money |
| Day 5: closing -230.00; fees for Day 2, Day 4, Day 5; error E8 | E7 (booked Day 5, dated Day 2) turns Days 2 and 4 negative after they closed, so both are charged at once, and Day 5 at its close; Auth-B is declined because available is already negative |
| Day 5: ACC-002 still 0.000 in the day-end log | E10 is booked Day 5 but arrives after Day 5 closed (it is listed after E9), so it wasn't there that night |
| Day 2 in the final view: 225.00, fee 25.00 | E9 reverses E7, but the Day 2 fee stays: 250.00 - 25.00 |
| Final balances: ACC-001 390.93, ACC-002 10.008 | 390.00 plus 0.93 interest (0.10 + 0.09 + 0.25 + 0.17 + 0.16 + 0.16); 10.000 plus 0.004 on each of Days 5 and 6 |

Every figure can be checked by hand from the rules. [`NUMBERS.md`](NUMBERS.md) shows the
arithmetic, and [`REJECTED.md`](REJECTED.md) explains why some of the brief's acceptance criteria
expect different numbers.

## Running the tests

```bash
mvn test
```

The suite is organised in layers:

| Layer | What it checks |
|---|---|
| **Business-rule invariants** (`BusinessRuleInvariantsTest`) | Rules that must hold at every moment, checked after **every step** of the replay: nothing recorded is ever changed, closing balance is the sum by value date, never two fees for one day, every settlement fits its hold, every split adds up, and more |
| **Rule tests** (`AccountsTest`, `AuthorizationTest`, `SettlementTest`, `ReversalTest`, `OverdraftFeeTest`, `InterestTest`, `InstalmentTest`, …) | Each rule with hand-worked numbers, including negative cases named after the mistake they rule out (for example `…CountsFromItsValueDateNotItsBookingDay`) |
| **Scenario and acceptance** (`BriefScenarioTest`, `AcceptanceCriteriaTest`) | The brief's replay end to end, and one test per acceptance criterion: four proven true, four proven wrong |

Test names carry the business-rule ID they enforce (`br8_…`), matching
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
├── src/main/java/ledger/core/      The ledger: knows nothing about the brief
├── src/main/java/ledger/scenario/  The brief's accounts and events, the replay and the report
├── src/test/java/ledger/core/      Business-rule invariants and rule tests
├── src/test/java/ledger/scenario/  The brief's replay, the acceptance criteria and the report
├── brief.md                        The assessment brief
├── README.md                       This file
├── BUSINESS-RULES.md               The business rules, each traced to the test that enforces it
├── AMBIGUITIES.md                  Every ambiguity found and how it was resolved
├── REJECTED.md                     The acceptance criteria refused, and approaches abandoned
├── NUMBERS.md                      Every constant and why it has that value
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
| Architecture & trade-offs (PDF) | Scale, value dating in production, authorization lifecycle, scope cuts | *to come* |

## Use of AI assistance

This project is built with the help of an AI assistant (Claude). Commits it contributed to carry
a `Co-Authored-By` trailer, and [`WORKLOG.md`](WORKLOG.md) notes where it was used. Every design
decision and every number in the output is mine to explain.
