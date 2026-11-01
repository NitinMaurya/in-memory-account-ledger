# Ambiguities

Where the brief could be read more than one way, what I chose, and what the other choice would have
changed. When it was unclear I went, in order, by: the written rule, the criteria that are true, the
non-negotiables, what a real bank would do, and the simplest option to explain.

## The decisions that shaped the result

| # | Question | My choice | The alternative | What it changes |
|---|---|---|---|---|
| A1 | Does a back-dated debit cause fees for days already closed? | Yes: re-check closed days | Freeze closed days | **3 fees**, not 1 (criterion 2 refused) |
| A4 | Are E7's fees refunded when E9 reverses it? | Yes, policy P-1 | Keep the fees | ACC-001 ends at **466.03**, not 390.93 (criterion 6 accepted) |
| A5 | Interest on each night's balance, or the corrected one? | Corrected | Frozen each night | Interest **1.03**, not 0.84 |
| A15 | Which ledger balance does available use? | The one on the request's day | All posted entries | Auth-B **declined**; money dated later can't be spent early |
| A10 | "Three equal instalments" of BHD 10.000 | 3.333 + 3.333 + 3.334 | 3 × 3.334 | No money created (criterion 7 refused) |
| A9 | What is credited on Day 6? | The exact sum of the rounded days | Discard a remainder | Nothing discarded (criterion 8 refused) |
| A16 | E10 is booked Day 5 but listed after E9 | Apply it late, in the order given | Re-sort by booking day | ACC-002 is 0.000 on the night of Day 5 |
| A36 | Does a fee count in the next day's fee check? | Yes, as the rule's wording says | Leave fees out | Nothing in the brief; matters in general |

**A1. Fees for days already closed.** E7 lands on Day 5 dated Day 2, turning closed Days 2 and 4
negative. A day's balance is defined by value date and the fee by that balance, so both are charged
when E7 lands, and Day 5 at its own close. Freezing closed days would leave Days 2 and 4 negative with
no fee. Checking fees only at the end would see E9 first and charge nothing, as if the bank knew on
Day 5 about a reversal on Day 6. *Test:* `OverdraftFeeTest.br25_aBackDatedDebitCausesAFeeForEveryClosedDay…`

**A4. Refund after the reversal.** The brief has no refund rule, so this is my judgment. E9 says E7
should never have happened, so a customer shouldn't pay for it, and the CBUAE Consumer Protection
Standards require charges caused by a bank's error to be refunded immediately, with no benefit to the
bank. Each refund is a new +25.00 entry booked on Day 6 and dated the fee's own day, so the interest
the fee cost comes back too: three fees and three refunds, nothing deleted. Only days from the
reversal's value date onwards are refunded, and only if they're non-negative without fees. In
production a reversal would carry a reason code: a bank error is refunded, a customer's own
cancellation may not be. *Test:* `OverdraftFeeTest.p1_…`

**A5. Corrected balances for interest.** Interest is paid only on Day 6, when the true balances are
known. Freezing each night's figure would pay nothing for Day 5 (−230.00 that night, 465.00 in the
end), so interest would be paid on balances later shown to be wrong.

**A15. Available by value date.** The brief defines available as "ledger balance minus active holds",
and its ledger balance is by value date. E7 is posted and dated Day 2 when Auth-B asks on Day 5, so it
counts and Auth-B is declined at −205.00. A credit dated Day 6 isn't in the account on Day 2, so it
can't be spent then. *Tests:* `AuthorizationTest.br15_aBackDatedDebit…`, `AuthorizationTest.br9_br15_…`

**A10. Unequal instalments.** 10,000 fils don't split into three equal parts at any precision. Floor
the shares and give the leftover fils to the last part; three times 3.334 would credit 0.002 BHD
nobody paid.

**A9. The Day 6 credit.** The brief requires the rounded days to sum to the credit, so the credit *is*
their sum and there is nothing to discard.

**A16. Replay order.** The brief says "replayed in this order", so E10 arrives after Day 5 has closed
and keeps its stated booking day. The day-end log shows ACC-002 at 0.000 that night; the final view
counts it for Day 5.

**A36. A fee in the next day's fee check.** The closing balance is "all entries", and a fee is an
entry, so a −1.00 day becomes −26.00 and can make the next day negative too. In the brief it changes
nothing (three fees either way). A bank might cap fees or ignore a balance pushed negative only by its
own fee; that's a tariff choice the brief doesn't make. The refund check (A4) leaves fees out on
purpose: it asks whether the day would be negative without the mistake.

## Every smaller decision

None of these changes the brief's figures, or the brief's wording settles it.

| # | Question | Choice |
|---|---|---|
| A2 | When is a fee charged? | Closed days: as soon as the entry lands. The current day: at its close |
| A3 | What dates does a fee carry? | Value date = the day it's for; booking day = the day it was found |
| A6 | Do fees and refunds count for interest? | Yes, both are in the closing balance |
| A7 | Does the interest credit earn interest? | No, that would be interest on interest |
| A8 | Rounding | Half-up per day to the fils; no brief balance lands on a tie |
| A11 | Instalments on one day or spread? | All on Day 5; spreading needs a Day 7 |
| A12 | Settlement for less than the hold (185 of 200) | Accept it and release the whole hold (criterion 3) |
| A13 | Settlement over the hold, or a second one | Refused |
| A14 | Settlement with no authorization (Auth-Z) | Refused, no money moves: no hold and no proof the customer agreed (criterion 4). In a real bank it goes to a suspense account |
| A17 | E9 gives no amount | Copy the original's amount, opposite sign |
| A18 | A fee on the BHD account? | None; the fee exists only in AED |
| A19 | Does a zero balance earn interest? | No |
| A20 | "Once per day" | At most one fee per account per day, ever, even after a refund |
| A21 | Are refused requests kept? | Yes, with their reason; they move no money |
| A22 | Opening balance | A property of the account, not an entry |
| A23 | Order on Day 6 | Fee check, then the interest credit |
| A24 | What is an "error" in the output? | A refused request with its reason (E6, E8) |
| A25 | An event outside Days 1–6 | Refused |
| A26–A28 | Settling a declined or another account's authorization; reusing an authorization ID | Refused |
| A29–A31 | Reversing a refused request, a non-credit/debit, or another account's entry | Refused |
| A32 | Days closing out of order or twice | Not allowed |
| A33 | Instalment entry type | Its own type, so parts can't be reversed one at a time |
| A34 | Zero instalments, or more parts than fils | Refused |
| A35 | Which "per day" to print | Both: each day as it closed, and as it ended up |
| A37 | Does a back-dated credit refund a fee? | No, only a reversal; a late credit doesn't prove the fee was wrong |
| A38 | The same request ID twice | The second is refused, so a retry never moves money twice |
| A39 | A request after Day 6 has closed | Refused, so the interest credit always equals the daily sum |
| — | Unknown account or another currency | Refused; AED and BHD are never mixed |
| — | Calendar dates | Days are numbers 1–6; the brief gives no calendar |

## Known limitations

Not resolved in the code:

- **No approval for back-dated entries.** One caller can rewrite closed days. This is the one failing
  test (`KnownGapBackDatedDebitApprovalTest`).
- **Date checks.** A settlement dated before its authorization, or a reversal dated before its
  original, is accepted. A request keeps a booking day that has already closed.
- **Holds never expire** and can't be cancelled before settlement.
- **Plain debits have no limit** and ignore active holds.
