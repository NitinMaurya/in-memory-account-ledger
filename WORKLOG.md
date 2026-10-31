# Work log

Times are local (UTC+4).

- **2026-10-31 08:52** — Received the brief. Created the project and initialised the repository.
- **2026-10-31 08:53** — Received Part 2 of the brief (architecture & trade-offs PDF, 2–4 pages).
- **2026-10-31 09:01** — Project scaffold: Maven, Java 21 target, JUnit 5, no other dependencies. Set a repo-only git identity. AI assistance (Claude) used for the scaffold; commits involving AI carry a Co-Authored-By trailer.
- **2026-10-31 09:04** — README in open-source style: overview, design principles, build and test instructions, documentation index. Unbuilt parts are marked *(planned)* so the README never claims more than exists. Drafted with Claude.
- **2026-10-31 09:31** — Public interface: value types (Currency, Money as whole minor units in a long), six event types, Outcome with rejection reasons, Entry, LoggedEvent, Authorization; Ledger as a stub with two commands and six queries. Brief scenario as data, a replay driver that closes days as the stream moves on, Money tests, and the first business-rule invariant (BR-6), which runs after every step. It is red: the Ledger stub is not built yet. Designed together with Claude; interface reviewed before code.
- **2026-10-31 09:39** — NUMBERS.md started with the money representation: why whole minor units in a long (not double, int or BigDecimal), the precision constants and their "why not half", and the costs of the choice. Written after talking the trade-off through with Claude.
