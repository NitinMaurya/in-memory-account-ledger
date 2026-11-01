# Rejected

The acceptance criteria I refused and why, and the approaches I abandoned while building. Every
verdict below is backed by a test in `AcceptanceCriteriaTest`, run against the brief's replay.

**Refused: criteria 2, 7 and 8.** Accepted: 1, 3, 4, 5 and 6.

## Refused acceptance criteria

### Criterion 2: "E7 causes exactly one overdraft fee to be assessed, on Day 2." Refused

E7 is booked on Day 5 but dated Day 2. A day's closing balance counts every entry dated on or before
that day, so E7 lowers Day 2 **and every later day** by 620.00:

| Day | Before E7 | With E7, before fees | Fee? |
|---|---|---|---|
| 2 | 250.00 | −370.00 | **Yes** |
| 3 | 650.00 | −370.00 + 400.00 − 25.00 (the Day 2 fee) = **+5.00** | No |
| 4 | 465.00 | 5.00 − 185.00 = **−180.00** | **Yes** |
| 5 | 465.00 | −180.00 − 25.00 (the Day 4 fee) = **−205.00** | **Yes** |

Three separate days close negative, and the fee is charged once per day per account, so E7 causes
**three** fees, dated Days 2, 4 and 5. A single Day 2 fee would leave Days 4 and 5 negative with
no fee, which breaks the fee rule.

*Test:* `criterion2_refused_E7CausesThreeFeesNotOne_forDays2_4And5`

### Criterion 7: "The three BHD instalments in E10 must each be BHD 3.334." Refused

3 × 3.334 = **10.002**. That would credit 0.002 BHD the customer never paid in. BHD has three decimal
places, so 10.000 can't be split into three identical parts: 10,000 fils ÷ 3 = 3,333 with 1 fils left
over. The ledger posts **3.333 + 3.333 + 3.334 = 10.000**, giving the leftover fils to the last part.

*Test:* `criterion7_refused_theInstalmentsAre3333_3333_3334_not3334Each`

### Criterion 8: "If the rounded daily interest accruals do not sum to the capitalized total, the remainder is discarded." Refused

The brief's own rule is that the rounded daily accruals **must** sum exactly to the capitalized
total. Discarding a remainder would break that rule and quietly underpay the customer. The ledger
credits exactly the sum of the rounded days, so there is never a remainder: ACC-001 is credited
1.03 (0.10 + 0.10 + 0.26 + 0.19 + 0.19 + 0.19) and ACC-002 0.008.

*Test:* `criterion8_refused_nothingIsDiscarded_theCreditEqualsTheSumOfTheRoundedDays`

## Accepted acceptance criteria

| # | Criterion | Why it's true | Test |
|---|---|---|---|
| 1 | Day 2 at end of Day 5, before any fee, is AED −370.00 | 250.00 − 620.00. The Day 2 fee is already on the book by then, so this is the balance excluding fees | `criterion1_accepted_…` |
| 3 | The Day 4 settlement of Auth-A must be accepted | A capture for less than the hold (185.00 of 200.00) is valid; the whole hold is released | `criterion3_accepted_…` |
| 4 | A settlement for an unknown authorization is rejected and no funds leave | E6 (Auth-Z) is refused, recorded, and creates no entry | `criterion4_accepted_…` |
| 5 | If Auth-B is approved, its hold reduces available, not ledger | True as a rule, shown with Auth-A on Day 2 (ledger 250.00, available 50.00). Its condition never happens: Auth-B is **declined** | `criterion5_accepted_…` |
| 6 | After E9, all balances and fees return to their pre-E7 values | True under my refund policy (AMBIGUITIES.md, A4). See below | `criterion6_accepted_…` |

### Criterion 6: accepted, and how

E9 reverses E7 by adding a new +620.00 entry dated Day 2. The three fees E7 caused are then
**refunded**, each by a new AED 25.00 entry dated the fee's own day. Nothing is deleted: the books show
three fees and three refunds, which net to zero.

| | Before E7 | After E9 |
|---|---|---|
| Days 2–5 closing | 250.00 / 650.00 / 465.00 / 465.00 | **250.00 / 650.00 / 465.00 / 465.00** |
| Fees, net | none | **3 fees + 3 refunds = 0.00** |

Two things this criterion does **not** mean, and that I want to be explicit about:

- **The records don't disappear.** "Return to pre-E7 values" is met by the *values*: the fee entries
  stay, with refunds beside them. The ledger is append-only.
- **Auth-B's decline is not undone.** It was declined on Day 5 because E7 had pushed available
  negative. An authorization decision isn't a balance or a fee, so the criterion doesn't cover it, but
  history still shows the customer was refused.

The brief has no refund rule, so accepting this criterion rests on my own policy decision. Keeping
the fees (390.93) would have been the other defensible reading.

## Approaches abandoned mid-build

| Approach | Why I dropped it | What replaced it |
|---|---|---|
| **A global fee policy switch** (keep or refund fees for every reversal) | A real bank decides per reversal, by why it happened; one switch for the whole ledger can't express that | A reason-code design (bank error or fraud → refund; customer request → keep) |
| **Reason codes on reversals** | The brief gives E9 no reason, so the codes would add code with nothing in the brief to drive them | A single refund rule (next row); reason codes are described as the production addition in AMBIGUITIES.md (A4) |
| **Keeping the fees after the reversal** (built first, and criterion 6 refused) | A customer shouldn't pay for a debit the bank undid, and a UAE bank must refund charges caused by its own error. The fees were charged correctly; leaving them in place was the part I changed my mind on | Fees are refunded by new, dated entries when a reversal clears their day: three fees plus three refunds, net zero. Criterion 6 is now accepted |
| **A failing test that asserts a refused criterion** (e.g. "each instalment is 3.334") | That tests the brief's mistake, not my design, and only repeats what this file already proves | The failing test exposes a gap in my own design: no approval for back-dated debits |
| **A flat, single package** | It mixed the general ledger with the brief's data and runner | `ledger.core` (knows nothing about the brief) and `ledger.scenario` |
| **A temporary "break the code and watch the test fail" check** for the append-only rule | Nobody else could re-run it | Permanent tests of the check itself (`AppendOnlyCheckTest`) |
| **An other-account test using the BHD account** | Planting a bug that matched entries by currency instead of by account showed the test passed by coincidence | A second AED account, and `anotherAccountInTheSameCurrencyIsNotCounted` |
| **A mutation-testing plugin (PIT)** | It adds a dependency, and the project keeps JUnit as its only one | Failure tests named after the mistake each one rules out |
| **A separate testing document** | More process than a small assessment needs | The test names, and the test layers described in the README |
| **An unused helper** (`Currency.minorUnitsPerUnit`) | Never called | Removed |

Two representation choices were weighed and rejected before code was written: `BigDecimal` for money
(it can hold a fraction of a fils unless every step rounds; see NUMBERS.md) and `LocalDate` for days
(the brief has no calendar; see AMBIGUITIES.md).
