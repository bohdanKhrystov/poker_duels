---
schema: 2
id: TASK-040526
title: A refund returns one slot even with a window behind it
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, auth, rate-limit]
depends_on: [TASK-040523]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AuthRouteTest'
  - ./gradlew :poker-server:detekt
---

## Goal

A successful sign-in returns **exactly one** slot to the budget, even when earlier attempts from the
same address are still inside the window. Refunding twice must fail a test.

## Why this exists

`TASK-040523` made sign-in the only caller that refunds, and gated the dangerous direction: an
unconditional refund on the **failure** path reddens three tests, so an attacker cannot hand
themselves back every wrong password.

The opposite direction is ungated. The coder wrote the bug — `refund` called twice on the success
path — and reported `BUILD SUCCESSFUL`, nothing red. The reviewer confirmed the reasoning:

- `AttemptBudget.refund` guards underflow with an emptiness check, so a second call on an empty
  window is a no-op
- every sequential request in `AuthRouteTest` drains its window to empty via the first, legitimate
  refund, so the second call finds nothing to remove
- the tests that *do* leave entries in the window are the budget-exhausting ones, which never reach
  the success branch at all

So the double refund is invisible **only because no test constructs the state where it would show**:
a sign-in that succeeds while still under budget, with earlier attempts from the same address still
live in the window. That is not an exotic case — it is a user who mistypes twice and then gets it
right, and today they would get two slots back instead of one. A limiter that returns more than it
took is weaker than its configuration claims.

`TASK-040523` was right not to fix it: its Tests table names exactly six tests, and a finding outside
a ticket becomes a new ticket. This is that ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify |

## Scope

One test. From one address, with a limit of ten in sixty seconds: two failed sign-ins (leaving two
live entries), then a **successful** one. The success refunds its own slot, so three attempts should
have consumed **two**. Prove that by exhausting what remains — the next eight attempts must be
admitted and the ninth refused, or by whatever counting the existing doubles make cheapest.

## Out of scope

- Changing `AttemptBudget` or `AuthRoutes.kt`. Today's code is correct; this ticket makes that a fact
  a gate holds rather than an accident the next refactor may drop.
- Sign-up, which never refunds (`TASK-040521`).
- Any change to the six tests `TASK-040523` added.

## Tests

| Test | Proves |
| --- | --- |
| `aSuccessRefundsOneSlotNotTwo` | two failures then a success from one address leave exactly two slots consumed, not one |

## Acceptance criteria

- [ ] The new test exists and passes.
- [ ] The six tests `TASK-040523` added are unchanged.
- [ ] `AuthRoutes.kt` and `AttemptBudget.kt` are not edited.
- [ ] Every command in `verify:` exits 0.

## Proof

Call `budget.refund(key)` twice on the success path in `AuthRoutes.kt` — the new test goes red while
the six from `TASK-040523` stay green. Revert. **Before this ticket that mutation turns nothing red**,
which is the whole reason it exists.

**Run it.** Nine `## Proof` sections in this run were wrong or incomplete when actually executed,
including one describing an edit that could not change behaviour at all.
