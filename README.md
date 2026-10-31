# In-Memory Account Ledger

An in-memory, append-only account ledger core, written in plain Java. It handles value-dated
entries, card authorization holds, daily overdraft fees and daily interest, and replays a fixed
six-day event stream, printing the state of each account at the end of every day.

There is no web layer, no database and no UI. The ledger is exercised entirely through its test
suite and a replay entry point.

> **Status: work in progress.** This repository is being built test-first, and the commit history
> and [`WORKLOG.md`](WORKLOG.md) record the work as it happens. Sections marked *(planned)*
> describe pieces that don't exist yet; they're updated as each one lands.

---

## Contents

- [What it does](#what-it-does)
- [Design principles](#design-principles)
- [Getting started](#getting-started)
- [Running the tests](#running-the-tests)
- [Running the replay](#running-the-replay) *(planned)*
- [Reading the output](#reading-the-output) *(planned)*
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
  re-evaluated after a back-dated entry.
- **Daily interest** on positive balances, rounded per day and capitalized once at the end of the
  window. The capitalized amount always equals the sum of the rounded daily amounts.
- **Instalment splits** that conserve money exactly at the currency's precision.

For every day the replay reports the closing ledger balance, fee assessments, authorization
states and errors.

## Design principles

- **Append-only.** Records are only ever added. Corrections are new, linked records, so any past
  balance can be reconstructed and every change can be explained.
- **Store facts, derive state.** Balances, hold totals and authorization states are computed from
  the recorded facts rather than stored and edited. A back-dated entry is one new record, not a
  rewrite of history.
- **Exact money.** Amounts are held at their currency's precision and rounded explicitly. There
  is no floating-point arithmetic on money.
- **Refusals are recorded.** A rejected request is kept, with its reason, so the system can always
  answer "why was this declined?". It just never moves money.
- **Small and explainable.** The goal is a core where every number can be derived by hand, not a
  feature-rich framework.

The reasoning behind each judgment call is in [`AMBIGUITIES.md`](AMBIGUITIES.md) *(planned)*.

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
mvn -q package
```

## Running the tests

```bash
mvn test
```

The suite will contain **one test that fails on purpose**. It documents a known gap in the
design, and its inline comment explains what it reveals. Instructions for running the passing
suite on its own will be added here when that test lands. *(planned)*

## Running the replay

*(planned)* A single command will replay the six-day event stream and print each day's closing
balances, fees, authorization states and errors.

## Reading the output

*(planned)* A walkthrough of the printed report: what each line means, and how to check every
figure by hand.

## Project layout

```
.
├── pom.xml              Maven build (Java 21, JUnit 5)
├── src/main/java/       Ledger core          (planned)
├── src/test/java/       Test suite           (planned)
├── README.md            This file
└── WORKLOG.md           Timestamped log of the work, written as it happens
```

## Documentation

| Document | Purpose | Status |
|---|---|---|
| [`README.md`](README.md) | Overview, build and run instructions | ✅ |
| [`WORKLOG.md`](WORKLOG.md) | Timestamped record of the work as it happened | ✅ |
| `AMBIGUITIES.md` | Every ambiguity found and how it was resolved | *planned* |
| `REJECTED.md` | Incorrect acceptance criteria refused, with reasons, and approaches abandoned | *planned* |
| `NUMBERS.md` | Every constant used, and why that value | *planned* |
| Architecture & trade-offs (PDF) | Scale, value dating in production, authorization lifecycle, scope cuts | *planned* |

## Use of AI assistance

This project is built with the help of an AI assistant (Claude). Commits it contributed to carry
a `Co-Authored-By` trailer, and [`WORKLOG.md`](WORKLOG.md) notes where it was used. Every design
decision and every number in the output is mine to explain.
