---
schema: 2
id: TASK-040525
title: A blank credential is invalid, not absent
type: task
status: ready
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, auth, security]
depends_on: [TASK-040518]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketSessionIdentityTest'
  - ./gradlew :poker-server:detekt
---

## Goal

A `Hello` carrying a blank or whitespace-only `sessionToken` must reach `IdentityResolver` as a
credential that is **present and invalid**, and be refused. It must not be read as *no session
presented* and quietly downgraded to the device beside it.

## Why this exists

`TASK-040510`'s deep review predicted this seam by name: *"a caller that mis-parses a malformed
`Authorization` header into 'no token presented', rather than a present-but-invalid `SessionToken`,
reintroduces that exact bug one layer up — outside the diff that proved it."*

It has now appeared three times. At the HTTP routes (`TASK-040511`) a coder wrote the bug and **one**
test in eighty-five caught it — the one written because of that warning. At the socket
(`TASK-040518`) the same coder wrote the bug again:

```kotlin
hello.sessionToken?.takeIf { it.isNotBlank() }?.let(::SessionToken)
```

and **all seventeen tests across both socket classes stayed green.** None of the eight tests in
`DuelSocketSessionIdentityTest` presents a blank token, so nothing observes the difference between
*absent* and *blank*. The resolver below is correct; the parsing above it is ungated.

`TASK-040518` was right not to fix it — its Tests table names exactly eight tests, and a finding
outside a ticket becomes a new ticket. This is that ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketSessionIdentityTest.kt` | modify |

## Scope

One test. A `Hello` whose `sessionToken` is `""` — and, if the assertion is cheap to extend, one
that is whitespace-only — presented alongside a **resolvable** device, must be refused exactly as
`"nonsense"` is: a `Failure(INVALID_SESSION)` frame and a close with `INVALID_SESSION_PRESENTED`,
and **no** `Welcome` naming the device's player.

## Out of scope

- Changing `DuelSocket.kt`. Today's code already treats a blank token as present-and-invalid; this
  ticket makes that a fact a gate holds, not an accident the next refactor may drop.
- The HTTP side. `TASK-040511`'s `aMalformedAuthorizationHeaderIsRefusedNotDowngradedToTheDevice`
  already covers a malformed header there; a blank bearer is a separate shape and, if it is also
  ungated, it is its own ticket.
- Any change to `IdentityResolver`. It is correct — `resolve()` gates on `!= null`, so a blank
  `SessionToken` already takes the invalid path.

## Tests

| Test | Proves |
| --- | --- |
| `aBlankTokenIsRefusedNotTreatedAsAbsent` | `Hello(deviceId = "d-anon", sessionToken = "")` with `d-anon` already resolved answers `Failure(INVALID_SESSION)` and closes with `INVALID_SESSION_PRESENTED`, and never a `Welcome` for the device's player |

**The device must be seeded.** `directory.resolve(DeviceId("d-anon"))` before the `Hello`, exactly as
`anInvalidTokenIsRefusedNotDowngraded` does — without it a downgrade has nothing to downgrade *to*,
and a refused connection and a downgraded one look identical.

## Acceptance criteria

- [ ] The new test exists and passes.
- [ ] `./gradlew :poker-server:test --tests 'duels.poker.server.DuelSocketSessionIdentityTest'` exits 0.
- [ ] The other eight tests in the class are unchanged.
- [ ] `DuelSocket.kt` is not edited.

## Proof

Apply `hello.sessionToken?.takeIf { it.isNotBlank() }?.let(::SessionToken)` in `DuelSocket.kt` — a
blank credential becomes *absent* — and the new test goes red while the other eight stay green.
Revert. Before this ticket that mutation turns nothing red, which is the whole reason it exists.

**Run it.** Nine `## Proof` sections in this run were wrong or incomplete when actually executed,
including one describing an edit that could not change behaviour at all.
