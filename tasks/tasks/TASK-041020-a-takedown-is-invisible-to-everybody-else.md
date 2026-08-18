---
schema: 2
id: TASK-041020
title: A takedown is invisible to everybody else, and its two strings live where they should
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, moderation, read-path]
depends_on: [TASK-041019]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.TakedownIsInvisibleTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A duel line for an opponent whose name was removed is byte-identical to one for an opponent who never
set a name, `retired_from` is readable from exactly one production file, and `retire_display_name` is
readable from none.

## Why these three sit in one ticket

`ADR-0053` §6 bundles them: *"`ADR-0052` §7's negative criterion is unchanged and is now also
structural … plus the code-shape assertion that `retired_from` appears in exactly one main source
file."* They are one guarantee — *nothing about a takedown reaches anybody but the player it happened
to* — asserted once behaviourally and twice structurally, and they share one directory sweep.

`ADR-0052` §5 is the reason it is a criterion rather than an omission: *"To the player themselves the
two `null` states are distinguishable; to everyone else they are not. That asymmetry is the decision,
and it is the half that protects the person who lost the name."*

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/TakedownIsInvisibleTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — `RECENT_DUELS_SQL` and `DUEL_LINES` |
| `docs/adr/ADR-0053-the-profile-says-the-name-was-removed.md` | read — §4's three numbered points |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §4's *"The server never calls it"* bullet |

## Scope

- One new class. Its single top-level declaration is the class, so the filename matches it.
- The behavioural test uses `PostgresProfileReads.recentDuelsOf` and compares two
  `DuelSummaryResponse` values **as encoded JSON**, with the three fields that legitimately differ
  normalised first:

  ```kotlin
  fun DuelSummaryResponse.normalised() = copy(duelId = "d", opponentPlayerId = "p", finishedAt = "t")
  ```

  then `assertEquals(protocolJson.encodeToString(...normalised()), protocolJson.encodeToString(...))`.
  Comparing encoded text rather than `opponentDisplayName == null` is what makes the assertion survive
  a future field: a `nameRemoved` badge added to `DuelSummaryResponse` fails this test, and an
  equality on one property would not.
- The two structural tests share one sweep over `poker-server/src/main/kotlin`, located the way
  `HttpEndpointDocumentationTest` locates `docs/protocol.md` — walk up from `File("")` until the
  directory is found — so the test passes whatever working directory Gradle uses.
- **The sweep must be asserted non-empty before it is asserted about.** A universal claim over a
  directory that was not found is vacuously true; assert the walk saw at least fifty `.kt` files and
  that `PostgresProfileReads.kt` is among them, and only then assert the matches.

## Out of scope

- Adding anything to `DuelSummaryResponse` or `RECENT_DUELS_SQL`. Both are named prohibitions.
- `docs/operations.md` — `TASK-041022`. This ticket asserts `retire_display_name` is absent from
  production code; that one asserts the document that tells an operator how to call it exists.
- A lint rule or detekt config enforcing either grep. `ADR-0051` calls this class of assertion *"the
  weakest guarantee in this document"* and it stays a test.

## Tests

`TakedownIsInvisibleTest`, `-PrequireDocker=true`. Three tests.

| Test | Proves |
| --- | --- |
| `aRemovedNameLooksExactlyLikeANameNeverSet` | One database: alice beats bob, alice beats carol. Bob set `"Ann"` and an operator retired it; carol never set a name. `recentDuelsOf(alice)` returns two lines whose normalised encodings are **equal**, and both have `opponentDisplayName == null`. The second assertion is not redundant: two lines could be equal and both wrongly carry a name |
| `retiredFromIsReadInExactlyOneFile` | Exactly one file under `poker-server/src/main/kotlin` contains `retired_from`, and it is `PostgresProfileReads.kt`. This catches a **new** file reading the column. **What it does not catch**: the paste `ADR-0053` §4.2 names by hand — the same `EXISTS` dropped into `RECENT_DUELS_SQL`, where `p` is the *opponent's* row. That second occurrence lands in a file already in the matching set, so a file-granular assertion cannot see it, and `readDuelSummary()` names six columns and discards the seventh, so the behavioural test misses it too |
| `retiredFromAppearsExactlyTwiceInPostgresProfileReads` | The occurrence count inside `PostgresProfileReads.kt` is pinned, so the §4.2 paste makes it three and goes red. Blunt on purpose: a legitimate third reference means updating the number **after** checking it is in neither `DUEL_LINES` nor `RECENT_DUELS_SQL`. Forcing a human to answer that question is the mechanism |
| `retireDisplayNameIsCalledByNoProductionCode` | **No** file under `poker-server/src/main/kotlin` contains `retire_display_name`. `ADR-0051` §4: the server never calls it, there is no port, no endpoint and no Gradle task, and the operator path is `psql` |

Both structural tests assert on the **set of matching file names**, not on a count — a count of `1`
cannot tell `PostgresProfileReads.kt` from `PostgresProfileWrites.kt`.

## Acceptance criteria

- [ ] `TakedownIsInvisibleTest.aRemovedNameLooksExactlyLikeANameNeverSet` passes and compares encoded
      JSON
- [ ] `TakedownIsInvisibleTest.retiredFromIsReadInExactlyOneFile` passes and asserts the matching set
      equals `setOf("PostgresProfileReads.kt")`
- [ ] `TakedownIsInvisibleTest.retiredFromAppearsExactlyTwiceInPostgresProfileReads` passes and fails against the `ADR-0053` §4.2 paste into `DUEL_LINES`
- [ ] `TakedownIsInvisibleTest.retireDisplayNameIsCalledByNoProductionCode` passes and asserts the
      matching set is empty
- [ ] Both structural tests assert the sweep saw at least fifty `.kt` files and included
      `PostgresProfileReads.kt` before asserting anything about matches
- [ ] `DuelSummaryResponse` and `PostgresProfileReads.RECENT_DUELS_SQL` are unmodified
- [ ] No file outside this ticket is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
