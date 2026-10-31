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
| AED decimal places | **2** | Given by the brief. The dirham is divided into 100 fils | 1 decimal place could not hold 0.05 or 0.93 |
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

*(The interest arithmetic and the `…Exact` guards arrive with the code that uses them. This section
is updated then.)*
