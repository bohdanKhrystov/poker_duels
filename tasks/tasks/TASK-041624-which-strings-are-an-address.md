---
schema: 2
id: TASK-041624
title: Which strings are an address
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, blocked]
depends_on: [TASK-041623]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.EmailAddressSyntaxTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Blocked

**`DEC-071` — the product owner's.** *Which strings does `POST /api/auth/recovery-email` accept as
an address, and what is the player told when one is refused?*

`ADR-0031` §5 answers `400` for *"an address that is not syntactically an address"* and states no
rule. §2 already settles storage and the `lower(... COLLATE "und-x-icu")` fold, so only the
acceptance predicate is open — and that `400` is the **only** thing this endpoint ever tells a
player about their address; every other outcome, including an address already verified to somebody
else, is a silent `202`.

The cost runs one way. A rule that refuses a real mailbox denies that player recovery, and
`ADR-0031`'s Consequences make no recovery a **total, permanent** loss of the account, its coins
and its ladder place. A rule that is too loose costs nothing today, because `ADR-0031` §7 defers
the transport to `EPIC-07` and no mail is sent under any answer.

**Do not implement a regex and call it done.** This ticket is a shell: its *Scope* and *Tests*
below are written for whatever the answer turns out to be, and the answering ADR fills in the rule
and the fixtures. Everything downstream of a guessed rule reads as settled.

## Goal

`emailAddressOrNull` exists and applies `DEC-071`'s answer, so the one refusal this flow makes is
made in one place.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddressSyntax.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/EmailAddressSyntaxTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt` — `loginHandleOrNull` is the
house shape for a canonicalise-or-refuse function, and the answer should land in the same form;
`poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddress.kt`;
the ADR that answers `DEC-071`.

## Scope

- One `public fun emailAddressOrNull(raw: String): EmailAddress?` in `duels.poker.server.auth`,
  returning the address **unmodified** on acceptance — no trim, no fold, no normalisation. §2 stores
  what the player typed, and a canonicalising function here would silently change what is delivered
  to.
- The rule is `DEC-071`'s and nothing else. Whatever it is, it lives in this one function: not in
  the DTO, not in the route, not in a `CHECK` constraint — the same reasoning `ADR-0031` §1 gives
  for the handle rule living in the write path.
- The KDoc cites the answering ADR by number and states the rule in prose beside the code.

## Out of scope

- The endpoint that calls it — `TASK-041625`.
- Any DNS, MX or deliverability check. Nothing in this repository sends mail, and a network call
  inside a validation function is a new dependency and a new failure mode.
- Folding or trimming. The fold is the database index's, per §2.
- A second rule at the client. `STORY-0417` may mirror it for a friendlier form, and the server's
  answer is authoritative either way.

## Tests

`EmailAddressSyntaxTest` — **the fixtures are `DEC-071`'s answer and are not written yet.** The
*shape* is fixed now so the answering ADR fills a table rather than designing a test:

| Test | Proves |
| --- | --- |
| `everyAcceptedFormIsAccepted` | A table of strings the answer names as acceptable, each returning a non-null `EmailAddress` whose `value` is the input **unchanged**. At least three, and at least one that a naive regex would refuse |
| `everyRefusedFormIsRefused` | A table of strings the answer names as refused, each returning `null`. At least three, and at least one that a naive regex would accept |
| `anAcceptedAddressIsNotCanonicalised` | `emailAddressOrNull("Bob@Example.com")?.value` is `"Bob@Example.com"`, assuming the answer accepts it |

Two tables, both non-empty and both non-trivial, because one list cannot tell a rule from a
constant: a function returning `null` always passes the second, and one returning the input always
passes the first.

## Acceptance criteria

- [ ] `DEC-071` is answered by a **merged** ADR before this ticket leaves `blocked`
- [ ] `EmailAddressSyntaxTest.everyAcceptedFormIsAccepted` passes
- [ ] `EmailAddressSyntaxTest.everyRefusedFormIsRefused` passes
- [ ] `EmailAddressSyntaxTest.anAcceptedAddressIsNotCanonicalised` passes
- [ ] Both tables hold at least three entries, and every entry appears in the answering ADR
- [ ] `emailAddressOrNull` returns the input string unchanged on acceptance — no `trim`, no
      `lowercase`, no `Normalizer`
- [ ] `EmailAddressSyntax.kt` opens no socket and reads no configuration
- [ ] Every command in `verify:` exits 0

## Proof

Written against the shape, and to be completed with the answer's fixtures:

1. Replace the body with `return EmailAddress(raw)` — accept everything.
   **`everyRefusedFormIsRefused` reddens alone**, on its first entry. Revert.
2. Replace the body with `return null` — refuse everything.
   **`everyAcceptedFormIsAccepted` and `anAcceptedAddressIsNotCanonicalised` both redden**;
   `everyRefusedFormIsRefused` passes. The pair is why both tables must be non-empty. Revert.
3. Return `EmailAddress(raw.lowercase())` on acceptance.
   **`anAcceptedAddressIsNotCanonicalised` reddens alone**, and `everyAcceptedFormIsAccepted`
   reddens too if its table holds any uppercase entry — it should, so predict **two**. Revert.
4. Delete the one accepted entry *a naive regex would refuse* and the one refused entry *a naive
   regex would accept*, then replace the implementation with `raw.matches(Regex(".+@.+\\..+"))`.
   **Nothing reddens.** That is the finding this ticket must record: without those two entries the
   test suite does not distinguish `DEC-071`'s answer from the first regex anyone would write, and
   the acceptance criterion demanding them is what makes the tests worth running. Restore both.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
