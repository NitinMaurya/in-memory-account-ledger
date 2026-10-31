# Business rules

The banking rules this ledger follows, written as a business analyst would state them, with no
code. Each rule has an ID (`BR-n`) used in test names, so any rule can be traced to the test that
enforces it.

- **Source.** *Stated*: the brief says it. *Implied*: the brief relies on it but never says it.
  Implied rules are interpretations, and the reasoning for each is in `AMBIGUITIES.md`.
- **Kind.**
  - *Invariant*: true at every moment, for any sequence of events. Checked after every step of the
    replay.
  - *Scenario*: true for the brief's event stream specifically.
- **Enforced by.** The test that proves it. *Planned* means the test hasn't been written yet. This
  file is updated as each one lands.

---

## 1. Accounts

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-1 | There are two accounts: ACC-001 in UAE dirhams (AED) and ACC-002 in Bahraini dinars (BHD) | Stated | Scenario | `BriefScenarioTest.br1_…`; refusal of unknown accounts: `AccountsTest.br1_…` |
| BR-2 | Both accounts open with a zero balance | Stated | Scenario | `AccountsTest.br2_…` |
| BR-3 | Each account operates only in its own currency; no currency conversion takes place | Implied | Invariant | `AccountsTest.br3_…`, `MoneyTest.refusesToAddDifferentCurrencies` |
| BR-4 | The business period is six days, Day 1 to Day 6; all fees and interest relate to it | Stated | Invariant | `WindowTest.br4_…` |

## 2. Money and precision

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-5 | AED amounts have 2 decimal places; BHD amounts have 3 | Stated | Invariant | `MoneyTest` |
| BR-6 | Every amount is kept and rounded at its own currency's precision; no fraction of a fils exists on an account | Stated | Invariant | `BusinessRuleInvariantsTest.br6_…` |

## 3. Balances

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-7 | Every transaction has a booking day (when it was recorded) and a value date (the day it counts for), which can differ | Stated | Invariant | `ClosingBalanceTest.br8_…` (a back-dated entry counts from its value date) |
| BR-8 | A day's closing balance is the sum of all transactions whose value date is on or before that day, whenever they were booked | Stated | Invariant | `BusinessRuleInvariantsTest.br8_…`, `ClosingBalanceTest.br8_…` |
| BR-9 | Available balance is the ledger balance minus money held for authorizations not yet settled | Stated | Invariant | `AuthorizationTest.br9_…` |
| BR-10 | A hold reduces the available balance but never the ledger balance | Stated | Invariant | `AuthorizationTest.br14_br10_…` |

## 4. Record keeping

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-11 | Nothing recorded is ever changed or deleted | Stated | Invariant | `BusinessRuleInvariantsTest.br11_…` |
| BR-12 | Mistakes are corrected only by recording a new, opposite transaction (a reversal) | Implied | Invariant | `ReversalTest.br12_…` |
| BR-13 | Refused requests are recorded with the reason, but move no money | Implied | Invariant | `AccountsTest.br1_…`, `AccountsTest.br3_…` (more refusals to come) |

## 5. Card authorizations

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-14 | An authorization reserves (holds) money; it does not take it | Implied | Invariant | `BusinessRuleInvariantsTest.br14_…`, `AuthorizationTest.br14_br10_…` |
| BR-15 | An authorization is approved only if the available balance stays at zero or above once the hold is placed | Stated | Invariant | `BusinessRuleInvariantsTest.br15_…`, `AuthorizationTest.br15_…` (boundary at exactly zero) |
| BR-16 | Otherwise it is declined, and no hold is placed | Implied | Invariant | `AuthorizationTest.br15_br16_…` |
| BR-17 | A hold stays in place until it is settled; holds do not expire within the six days | Implied | Invariant | `SettlementTest.aSettlementForMoreThanTheHoldIsRefusedAndTheHoldStays` |

## 6. Card settlements

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-18 | A settlement takes the money for an earlier authorization and ends that hold | Implied | Invariant | `SettlementTest.br18_…`, `BusinessRuleInvariantsTest.br18_br20_…` |
| BR-19 | A settlement for less than the authorized amount is accepted (Auth-A: 185.00 against 200.00) | Stated | Scenario | `SettlementTest.br18_br19_…` |
| BR-20 | A settlement for an authorization that doesn't exist is refused, and no money leaves the account | Stated | Invariant | `SettlementTest.br20_…`, `BusinessRuleInvariantsTest.br18_br20_…` |

## 7. Reversals

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-21 | A reversal cancels one specific earlier transaction by recording the opposite amount | Implied | Invariant | `ReversalTest.br21_…`, `BusinessRuleInvariantsTest.br21_…` |
| BR-22 | A reversal carries the value date it is given (E9: Day 2, the same as the transaction it reverses) | Stated | Scenario | `ReversalTest.br21_br22_…` |

## 8. Overdraft fee

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-23 | When a day's closing balance is negative, an overdraft fee of AED 25.00 is charged | Stated | Invariant | `OverdraftFeeTest.br23_…` (boundary: zero is not charged), `BriefScenarioTest` |
| BR-24 | At most one overdraft fee per account per day | Stated | Invariant | `OverdraftFeeTest.br24_…`, `BusinessRuleInvariantsTest.br24_…` |
| BR-25 | An overdraft fee is dated to the day it is charged for, even when it is charged later | Stated | Invariant | `OverdraftFeeTest.br25_…`, `BusinessRuleInvariantsTest.br25_…` |
| BR-26 | The fee is defined only in AED; no BHD fee exists | Stated | Invariant | `OverdraftFeeTest.br26_…` |

## 9. Interest

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-27 | Interest of 0.04% per day is paid on each day's closing balance | Stated | Invariant | `InterestTest.br27_br29_…`, `BriefScenarioTest` |
| BR-28 | Only positive balances earn interest; zero and negative balances earn nothing | Stated | Invariant | `InterestTest.br28_…` |
| BR-29 | Each day's interest is rounded to the currency's precision | Stated | Invariant | `InterestTest.br29_…` (an exact half rounds up) |
| BR-30 | Interest is credited once, as a single amount, at the end of Day 6 | Stated | Invariant | `InterestTest.br30_br31_…`, `BusinessRuleInvariantsTest.br30_br31_…` |
| BR-31 | The credited interest equals exactly the sum of the rounded daily amounts; nothing is discarded | Stated | Invariant | `InterestTest.br30_br31_…`, `BusinessRuleInvariantsTest.br30_br31_…` |

## 10. Instalments

| ID | Rule | Source | Kind | Enforced by |
|---|---|---|---|---|
| BR-32 | A credit can be paid in as several instalments (E10: BHD 10.000 in three) | Stated | Scenario | `InstalmentTest.br32_br33_…`, `BriefScenarioTest` |
| BR-33 | Instalments add up exactly to the original amount; no money is created or lost | Implied | Invariant | `InstalmentTest.br32_br33_…`, `MoneyTest.splits…`, `BusinessRuleInvariantsTest.br33_…` |

**Counts:** 33 rules: 24 stated, 9 implied. 28 invariants, 5 scenario.

---

## Where the rules are silent

These are not rules. They are gaps where the brief gives no answer, so a decision had to be made.
Each is recorded, with its alternatives and reasoning, in `AMBIGUITIES.md`. Tests that pin these
decisions are labelled as *policy*, kept separate from the business-rule tests above.

1. Does a back-dated transaction trigger fees for days already closed?
2. Are fees refunded when the transaction that caused them is reversed?
3. Is interest paid on each day's balance as it stood that night, or as later corrected?
4. What happens to a settlement for more than its hold, or a second settlement of the same
   authorization?
5. When instalments cannot be exactly equal, which one takes the leftover fils?
6. What happens if the BHD account goes negative, when the fee exists only in AED?
