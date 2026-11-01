# Ambiguities

Every place where the brief could be read more than one way, the choice I made, and what the
other choice would have meant. Each decision is pinned by a test.

## How I decided

When the brief was unclear, I chose using this order:

1. **The written rules.** If a rule's wording decides it, follow it literally.
2. **The acceptance criteria that are true.** They show what the author intended.
3. **The non-negotiables.** Append-only, money adds up, each currency at its own precision.
4. **What a real bank would do.**
5. **The simplest option to explain.**

**Only two decisions change a final number**: A4 and A5. A1 changes how many fees are charged, but
not where the account ends up. Everything else has one clearly better answer, or doesn't affect the
brief's figures.

| Decision | My choice | Effect | The alternative would give |
|---|---|---|---|
| A4: fees after the reversal | Refunded | ACC-001 ends at **466.03** | 390.93 (fees kept) |
| A5: interest on which balances | As corrected | **1.03** interest | 0.84 (frozen each night) |
| A1: fees for days already closed | Re-check them | **3 fees** charged on Day 5 | 1 fee, or none. Because every fee is refunded after E9, the final balance is 466.03 either way |

---

## Time and value dating

**A1. Does a back-dated entry cause fees for days that have already closed?**
E7 is booked on Day 5 but dated Day 2. Days 2 and 4 had closed positive; with E7 counted they
close negative.
- **Choice:** re-check every closed day after each accepted event, so E7 charges Days 2 and 4, and
  Day 5 is charged at its own close: **three fees**.
- **Why:** the brief defines a day's closing balance by value date, and the fee by the closing
  balance. Freezing closed days would leave Days 2 and 4 negative with no fee (one fee). Checking fees
  only once at the end would see E9 first and charge nothing, as if the bank knew on Day 5 about a
  reversal that happened on Day 6.
- **Test:** `OverdraftFeeTest.br25_aBackDatedDebitCausesAFeeForEveryClosedDay…`

**A2. When is each fee charged?**
- **Choice:** fees for days already closed are charged as soon as the entry that causes them lands;
  the current day's fee waits for that day's close.
- **Why:** the current day isn't over, and a deposit could still arrive. It changes nothing here:
  Auth-B is declined either way (available −205, or −230 with the Day 5 fee).

**A3. What dates does a fee carry?**
- **Choice:** value date = the day it is for; booking day = the day the bank found it. E7's fees for
  Days 2 and 4 are booked on Day 5.
- **Why:** the brief says fees are booked with the value date of the day assessed. A fee is
  therefore never dated after its booking day (checked after every replay step).

**A16. E10 is booked Day 5 but listed after E9 (Day 6).**
- **Choice:** replay strictly in the listed order. E10 is applied after Day 6 has started, as a late,
  back-dated arrival.
- **Why:** the brief says "replayed in this order". Sorting by booking day would quietly change the
  stream I was given. In the day-end log ACC-002 is still 0.000 on Day 5, because E10 wasn't there
  that night; in the final view it counts for Day 5.

**A25. An event booked or dated outside Days 1–6.**
- **Choice:** refuse it (`OUTSIDE_WINDOW`), record it, move no money.
- **Why:** fees and interest only exist inside the window. The brief's events never leave it, so no
  number changes.

**A32. Can days close out of order, or twice?**
- **Choice:** no. Days close in order, once each; anything else is a programming error.
- **Why:** fee checks depend on knowing which days have closed.

**Days are numbers, not calendar dates.** The brief only ever says "Day 1" to "Day 6". Real dates
would mean inventing a start date, weekends and holidays the brief never gives.

## Overdraft fees

**A4. Are E7's fees refunded when E9 reverses it?**
- **Choice:** yes (policy P-1). When a reversal makes a charged day non-negative again, that day's fee
  is refunded by a **new** entry of AED 25.00, booked on the reversal's day (Day 6) and dated the
  fee's own day (Days 2, 4, 5). The books show three fees and three refunds, net zero; nothing is
  deleted. ACC-001 ends at 466.03, and the balances of Days 2–5 return to their pre-E7 values, which is
  why I accept criterion 6.
- **Why:** E9 says E7 should never have happened, and a customer shouldn't pay for a debit that was
  undone. Dating each refund on its fee's day also gives back the interest the fee cost. This is
  also what a UAE bank must do when the debit was its own error: the CBUAE Consumer Protection
  Standards require charges caused by an error to be refunded immediately, and the bank must not
  benefit from its error.
- **Only days from the reversal's value date onwards**, and only when that day is non-negative
  without fees. A day before the reversal wasn't changed by it, so its fee stays.
- **The alternative:** keep the fees (390.93). The brief defines no refund rule, so this is a
  judgment call, and the reason I list it here.
- **What I'd add in production:** a reason code on every reversal. A refund fits a bank error, fraud
  or a mistaken posting; a genuine transaction cancelled at the customer's request might keep its
  fees. The brief gives E9 no reason, so the ledger refunds on every reversal that clears a day.

**A18. A fee on the BHD account?**
- **Choice:** none. The fee exists only in AED.
- **Why:** charging BHD 25.000 or converting would invent a tariff or an exchange rate. ACC-002
  never goes negative anyway.

**A20. "Once per day": a day that is checked again, dips negative twice, or was refunded.**
- **Choice:** at most one fee per account per day, ever, even after a refund.
- **Why:** the rule says once per day per account.

## Interest

**A5. Interest on each day's balance as it was that night, or as later corrected?**
- **Choice:** as corrected: interest is computed from the ledger as it stands. 1.03 for ACC-001.
- **Why:** interest is only paid on Day 6, when the true balances are known, and the brief says
  0.04% "on the closing ledger balance". Freezing each night's figure would give 0.84 (Day 5 closed at
  −230.00 that night and would earn nothing), paying interest on balances later shown to be wrong.

**A6. Do fees and refunds count in the balance that earns interest?**
- **Choice:** yes. A fee lowers that day's interest; its refund, dated the same day, restores it.
- **Why:** both are part of the closing ledger balance.

**A7. Does the Day 6 interest credit earn interest itself?**
- **Choice:** no.
- **Why:** that would be interest on interest.

**A8. Rounding mode.**
- **Choice:** half-up, per day, to the fils.
- **Why:** ordinary commercial rounding. No balance in the brief lands on an exact half, so the choice
  changes no figure; a test on an exact tie proves the behaviour (AED 12.50 → 0.01).

**A9. What is credited on Day 6?**
- **Choice:** exactly the sum of the six rounded days.
- **Why:** the brief requires the rounded days to sum to the credit, so the credit is their sum and
  nothing can be left over to discard.

**A19. Does a zero balance earn interest?**
- **Choice:** no. Zero isn't positive.

**A23. The order of things on Day 6.**
- **Choice:** fee check, then the interest credit.
- **Why:** fees affect the balance; the credit itself doesn't earn Day 6 interest.

## Card authorizations and settlements

**A15. Does available balance include back-dated entries already posted?**
- **Choice:** yes: every posted entry, whatever its value date, minus active holds.
- **Why:** E7 is on the books when Auth-B asks on Day 5. Ignoring it "because it is dated Day 2"
  would approve spending on a negative account. This is why Auth-B is declined.

**A12. A settlement for less than the hold (Auth-A: 185.00 of 200.00).**
- **Choice:** accept it, take 185.00 and release the whole 200.00 hold.
- **Why:** the merchant has finished; leaving 15.00 held would block the customer's money. Criterion
  3 says accept.

**A13. A settlement for more than the hold, or a second settlement.**
- **Choice:** refuse both (`SETTLEMENT_EXCEEDS_HOLD`, `ALREADY_SETTLED`).
- **Why:** money must not leave beyond what was authorized.

**A14. A settlement for an authorization that doesn't exist (Auth-Z).**
- **Choice:** refuse it (`UNKNOWN_AUTHORIZATION`), record it, move no money.
- **Why:** criterion 4 says exactly this.

**A26. Settling an authorization that was declined.** Refused as unknown: a declined authorization
reserved nothing, so there is no hold to settle against.

**A27. Settling another account's authorization.** Refused as unknown: one account's hold can't pay
another account's settlement.

**A28. A second authorization reusing an ID.** Refused (`DUPLICATE_AUTHORIZATION`): each ID must
point to exactly one hold.

## Reversals

**A17. E9 states no amount.**
- **Choice:** the reversal copies the original's amount with the opposite sign (+620.00), and carries
  the value date it is given (Day 2).
- **Why:** a reversal cancels one specific transaction; copying the amount means it can't be wrong.

**A29. Reversing a request that was refused.** Refused as unknown: it never moved money.

**A30. What can be reversed?** Only plain credits and debits. Settlements, fees, refunds, interest,
instalments and other reversals are refused (`NOT_REVERSIBLE`): card captures are undone through
refunds or chargebacks, fees and interest are the bank's own postings, and reversing a reversal
would just recreate the original.

**A31. Reversing another account's transaction.** Refused as unknown.

## Instalments

**A10. "Three equal instalments" of BHD 10.000 is impossible at 3 decimal places.**
- **Choice:** floor the shares and give the leftover fils to the last part: 3.333 + 3.333 + 3.334.
- **Why:** the parts must add up to exactly 10.000. Three times 3.334 is 10.002, which creates money
  (criterion 7); three times 3.333 loses a fils. More decimal places can't help, because ten never
  divides evenly by three. First or last are equally valid; last is the usual convention.

**A11. One value date, or spread over days?** All on Day 5, the event's value date. Spreading them
would need a Day 7, outside the window.

**A33. What type of entry is each part?** A separate `INSTALMENT` type, so the parts stay traceable
as one split and can't be reversed one third at a time.

**A34. Zero instalments, or more parts than fils.** Refused (`INVALID_INSTALMENTS`): every part must
be at least one fils.

## Records and output

**A21. Are refused requests kept?** Yes, in the event log with their reason, but they move no money.
The output has to show errors, and nothing is ever deleted.

**A22. Opening balance.** A property of the account (zero), not an entry. There is no opening event
in the stream.

**A24. What counts as an "error" in the output?** A refused request with its reason. In the brief:
E6 (unknown authorization) and E8 (insufficient available).

**Unknown accounts and other currencies.** An event for an account that doesn't exist, or with an
amount in another currency, is refused (`UNKNOWN_ACCOUNT`, `CURRENCY_MISMATCH`). The brief gives
no exchange rate, so AED and BHD are never mixed.

**A35. Which "per day" does the replay print?** Both: a day-end log (each day as it was when it
closed) and a final view (each day by value date after every event). Back-dating changes a day's
numbers after it closes, so one view alone would hide either what the bank knew or how the history
ended up. Day 5 is −230.00 that night and 465.00 in the end.

## Known limitations

These are not resolved in the code, and are listed so nobody assumes they are:

- **No approval for back-dated entries.** One caller can post a debit that rewrites closed days. This
  is the one failing test (`KnownGapBackDatedDebitApprovalTest`).
- **A settlement dated before its authorization** would be accepted.
- **A reversal dated earlier than its original** would be accepted, and could create an overdraft
  that never existed. In production it should be required to carry the original's value date.
- **Holds never expire**, and an authorization cannot be cancelled before settlement.
