---
schema: 2
id: TASK-041012
title: retire_display_name takes the name away and leaves the profile unset
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, moderation, operations]
depends_on: [TASK-041011]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RetireDisplayNameTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`retire_display_name(player_id, expected_name)` promotes the registry row to `RETIRED`, leaves the
player holding **no name** rather than a new one, and refuses — writing nothing — when the second
argument does not name what that player holds.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/RetireDisplayNameTest.kt` | create |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the function body |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §4 in full |

## Scope

- One new test class calling the function as an operator would: `SELECT retire_display_name(?, ?)`
  with the player's `UUID` and the expected name, on a plain autocommit connection.
- The fixture gives a player a name the way the product does — `PostgresProfileWrites.setDisplayName`
  — so the registry row exists with `reason = 'TAKEN'` and the test is not asserting against a state
  it hand-built.
- Refusals are matched on `SQLSTATE`, never on a message:

  | Refusal | `SQLSTATE` |
  | --- | --- |
  | the player holds no display name | `P0002` (`no_data_found`) |
  | the player does not hold that name | `23001` (`restrict_violation`) |

- Every refusal test also asserts the world is unchanged: the player still holds the name, the
  registry row is still `TAKEN`, and `retired_from` is still `NULL`. `ADR-0051` §4 makes the second
  argument *"the interlock"*, and an interlock that raises after writing one of its two rows is not
  one.

## Out of scope

- Any Kotlin production code. **No file under `poker-server/src/main/kotlin` is touched, now or ever,
  by this function** (`ADR-0051` §4: *"The server never calls it"*). The code-shape assertion that
  proves it lives in `TASK-041022` with `docs/operations.md`.
- The permanence trigger's exception — `TASK-041013` — even though this function depends on it. Here
  the exception is exercised implicitly by the happy path; there it is pinned directly.
- Coin balances and `duel_result` — `TASK-041014`.
- What the player is told — `TASK-041018` and `STORY-0411`.
- A second operator, a role, an endpoint, a Gradle task. `ADR-0051` §9 refuses all four.

## Tests

`RetireDisplayNameTest`, `-PrequireDocker=true`. Six tests.

| Test | Proves |
| --- | --- |
| `aTakedownLeavesTheProfileWithNoName` | After `retire_display_name(alice, 'Ann')`: `player.display_name` is `NULL` — **not** a server-chosen replacement. `ADR-0051` §4 and `ADR-0021` both forbid the server minting a name, and `ADR-0038`'s heading sentence says *force-rename*, which is why this is asserted rather than assumed |
| `aTakedownRetiresTheStringAndRecordsWhoHeldIt` | The `name_registry` row for `"Ann"` still exists, with `reason = 'RETIRED'` and `retired_from` equal to alice's id. **The wrong implementation this must fail against**: a takedown that deletes the registry row — the name is then free, which is the one outcome `ADR-0038` exists to prevent, and a test that only checked `display_name IS NULL` would pass |
| `theFunctionReturnsTheNameItTookAway` | Alice holds `"Ann"`; `retire_display_name(alice, 'ANN')` answers `"Ann"` — the string **as stored**, so an operator can paste it into a record. The argument must differ in case from the stored column or the test proves nothing: with both `"Ann"`, a function returning its own argument and one returning the column it read produce the same string, and the test passes against either |
| `aMismatchedExpectedNameWritesNothing` | `retire_display_name(alice, 'Bea')` while alice holds `"Ann"` raises `23001`, and afterwards alice still holds `"Ann"` **and** the `"Ann"` row is still `TAKEN` with `retired_from IS NULL`. This is the operator-accident interlock |
| `theInterlockIsCaseInsensitive` | Alice holds `"Ann"`; `retire_display_name(alice, '  aNN ')` succeeds and returns `"Ann"`. The comparison is under `ADR-0029` §1's fold with the expected name trimmed and NFC-normalised, so an operator need not reproduce case. **Fails against** a function comparing raw equality, which would refuse a correct takedown at the moment it is needed |
| `aPlayerWithNoNameCannotHaveOneTakenAway` | `retire_display_name(bob, 'Anything')` where bob's `display_name` is `NULL` raises `P0002` and writes nothing |

## Acceptance criteria

- [ ] `RetireDisplayNameTest.aTakedownLeavesTheProfileWithNoName` passes and asserts `display_name IS
      NULL`
- [ ] `RetireDisplayNameTest.aTakedownRetiresTheStringAndRecordsWhoHeldIt` passes and asserts the row
      exists with `reason = 'RETIRED'` and the correct `retired_from`
- [ ] `RetireDisplayNameTest.theFunctionReturnsTheNameItTookAway` passes, calling with a case variant, and fails against a function returning its argument
- [ ] `RetireDisplayNameTest.aMismatchedExpectedNameWritesNothing` passes and asserts `23001` plus
      both unchanged rows
- [ ] `RetireDisplayNameTest.theInterlockIsCaseInsensitive` passes
- [ ] `RetireDisplayNameTest.aPlayerWithNoNameCannotHaveOneTakenAway` passes and asserts `P0002`
- [ ] No file under `poker-server/src/main/kotlin` is modified
- [ ] No migration file is modified — if the function needs a fix, this ticket stops and says so
      rather than editing a merged `V<n>`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
