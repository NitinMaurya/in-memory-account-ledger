# NUMBERS

Every constant in this ledger: why it has that value, and why not half of it.

> Built up as the code grows. Each section is added in the same commit as the code that uses it.

---

## Money representation

Every amount is a **whole number of minor units** (fils) held in a Java `long`, together with its
currency: `Money(long minorUnits, Currency currency)`.

| Brief amount | Stored as |
|---|---|
| AED 1,200.00 | `Money(120000, AED)` |
| AED 25.00 | `Money(2500, AED)` |
| BHD 10.000 | `Money(10000, BHD)` |
| BHD 3.334 | `Money(3334, BHD)` |

### Constants

| Constant | Value | Why this value | Why not half |
|---|---|---|---|
| AED decimal places | **2** | Given by the brief. The dirham is divided into 100 fils | 1 decimal place could not hold 0.05 or 0.19 |
| BHD decimal places | **3** | Given by the brief. The dinar is divided into 1,000 fils | At 1.5 there is no such precision; at 2 places 3.334 could not exist and the E10 split would lose fils |
| AED minor units per unit | **100** | Follows from 2 decimal places: 1.00 AED = 100 fils | 50 per dirham would make the smallest step 0.02, so 0.01 could not be represented |
| BHD minor units per unit | **1,000** | Follows from 3 decimal places: 1.000 BHD = 1,000 fils | 500 per dinar would make the smallest step 0.002, so 3.333 could not be represented |
| Storage width | **64-bit `long`** | Largest value ≈ 9.2 × 10¹⁸ fils, about 92 quadrillion dirhams: far beyond any balance or total | Half the width is a 32-bit `int`: its maximum, 2,147,483,647 fils, is only about **21 million dirhams** (2.1 million dinars). Past that it silently wraps to a large negative number, which is wrong money with no error |

### Why whole minor units, not decimals

- **No floating-point.** `double` cannot store most decimals exactly (`0.1 + 0.2 = 0.30000000000000004`).
  Errors compound across transactions and the books stop balancing.
- **A fraction of a fils cannot exist.** A `long` has no fractional part, so the brief's rule
  "amounts stored and rounded to their own precision" is enforced by the type itself, not by
  remembering to round. `Money.of("3.334", AED)` is refused when the amount is created.
- **Leftovers are visible.** BHD 10.000 is 10,000 fils; 10,000 ÷ 3 = 3,333 remainder **1**. Integer
  division exposes the leftover fils, which must then be given to one instalment
  (3.333 + 3.333 + 3.334). It can't quietly round all three up to 3.334, which would create 0.002 BHD.
- **The currency travels with the amount.** `Money(2500, AED)` and `Money(2500, BHD)` are different
  values, so AED and BHD can never be mixed silently.

### Why not `BigDecimal`

`BigDecimal` is exact and a common choice for money in Java, so this is a trade-off, not a
correction.

| | `double` | `BigDecimal` | `long` minor units (chosen) |
|---|---|---|---|
| Exact | No | Yes | Yes |
| A fraction of a fils is impossible | No | No: it holds 3.3333333 unless every step rounds | **Yes, by design** |
| Overflow | — | Cannot overflow | Possible, guarded (below) |
| Equality trap | Yes | Yes: `2.0.equals(2.00)` is `false` | None: `120000 == 120000` |
| Rounding modes built in | — | Yes | No: done explicitly in integer arithmetic |

**Chosen:** `long`, because it makes the brief's precision rule impossible to break and leaves no
`equals`-versus-`compareTo` trap. `BigDecimal` is used in exactly one place: parsing text such as
`"1200.00"` when an amount is created.

### What this choice costs, and how it's handled

| Cost | Handling |
|---|---|
| A `long` can overflow | All money arithmetic uses `Math.addExact` / `Math.multiplyExact`, which fail loudly instead of wrapping |
| Rounding is done by hand | Interest is integer arithmetic: `balance × 4 / 10,000`, rounded half-up as `(balance × 4 + 5,000) / 10,000` for a positive balance, and proved by a test on an exact .5 tie |
| Raw numbers read poorly (`120000`) | `Money.toString()` prints at the currency's precision: `AED 1200.00`, `BHD 3.334` |

The overflow guards are in place: `Money.plus` uses `Math.addExact`, `Money.negate` uses
`Math.negateExact`, and the interest arithmetic uses `Math.multiplyExact` / `Math.addExact`.
`MoneyTest.failsLoudlyInsteadOfOverflowing` proves an overflow throws instead of wrapping.

---

## Window

| Constant | Value | Why this value | Why not half |
|---|---|---|---|
| `FIRST_DAY` | **1** | The brief's window starts on Day 1 | — (there is no Day 0.5; Day 0 is outside the window and refused) |
| `LAST_DAY` | **6** | The brief's window ends on Day 6, when interest is capitalized | A 3-day window would drop E7–E10: no back-dating, no reversal, no capitalization |

## Overdraft fee

| Constant | Value | Why this value | Why not half |
|---|---|---|---|
| `OVERDRAFT_FEE` | **AED 25.00** | Given by the brief: charged when a day's closing balance is negative | AED 12.50 is not the stated tariff; charging it twice a day would break "once per day per account" |
| Fees per account per day | **at most 1** | "Once per day per account" | — |
| BHD overdraft fee | **none** | The tariff is quoted only in AED; a BHD fee or an exchange rate would be invented | BHD 25.000 or a converted amount would invent a tariff the brief never gave |

In the brief this gives **three** fees (AED 75.00), dated Days 2, 4 and 5, all charged on Day 5: E7
(booked Day 5, dated Day 2) turns Days 2 and 4 negative after they had closed, and Day 5 closes
negative too. Day 3 stays at +5.00 thanks to its 400.00 deposit. When E9 reverses E7 on Day 6, the
three fees are **refunded** (policy P-1): three refunds of AED 25.00, each dated its fee's day, so the
fees net to zero.

## Interest

| Constant | Value | Why this value | Why not half |
|---|---|---|---|
| Daily rate | **0.04%** = `DAILY_INTEREST_NUMERATOR / DAILY_INTEREST_DENOMINATOR` = **4 / 10,000** | Given by the brief. Kept as a whole-number fraction so interest is computed in fils, with no floating point | 0.02% is a different product and would halve every accrual |
| Rounding | **half-up, per day, to the fils** | Each day's accrual must be a real amount in that currency. Half-up is ordinary commercial rounding. In fils: `(balance × 4 + 5,000) / 10,000` | Half-even would only differ on an exact tie, and no balance in the brief lands on one (the six balances are listed below the table). How a tie rounds is pinned by `InterestTest.br29_anExactHalfFilsRoundsUp` (AED 12.50 → 0.01). Truncating would bias every accrual down |
| Capitalized amount | **the exact sum of the six rounded days** | The brief requires the rounded days to sum exactly to the credit, so the credit *is* their sum: nothing discarded | Rounding the unrounded total instead could differ by a fils and break the rule (criterion 8) |
| Balance used | **corrected closing balance, fees included, interest credit excluded** | Interest is only paid on Day 6, when the true balances are known; the credit earning interest would be interest on interest | — |

In the brief, ACC-001 earns **0.10 + 0.10 + 0.26 + 0.19 + 0.19 + 0.19 = 1.03** on the corrected
balances (250, 250, 650, 465, 465, 465), so it ends at **466.03**. Freezing each night's balance
instead would give 0.84 (Day 5 closed at −230.00 that night and earned nothing).

## Instalments (E10)

| Constant | Value | Why this value | Why not half |
|---|---|---|---|
| E10 instalments | **3** | Given by the brief: "three equal instalments" | Two or six parts would be a different payment schedule |
| E10 split | **BHD 3.333 + 3.333 + 3.334** | 10.000 BHD = 10,000 fils; 10,000 ÷ 3 = 3,333 remainder **1**. The remainder fils goes to the last part, so the parts add up to exactly 10.000 | Exactly equal parts are impossible at 3 decimal places: 3 × 3.334 = 10.002 creates 2 fils; 3 × 3.333 = 9.999 loses one. More decimal places can't fix it, because 10 ÷ 3 never divides evenly |
| Smallest instalment | **1 fils** | A zero-value part isn't a payment, so a split into more parts than there are fils is refused | — |

ACC-002 earns 0.04% on 10.000 on Days 5 and 6: 0.004 + 0.004 = **0.008**, so it ends at **10.008**.
