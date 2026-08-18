---
schema: 2
id: TASK-041013
title: The permanence trigger has exactly one exception, and it is a transition
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, schema, identity, moderation]
depends_on: [TASK-041012]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DisplayNamePermanenceTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`player_display_name_is_permanent` lets a name go only by `name → NULL` and only when that name is
already `RETIRED`, and refuses every other transition for everybody — including whoever is running
the takedown.

## Why the shape of the exception is the whole point

`ADR-0051` §3 scopes the exception to the **transition**, never to `current_user`, a GUC or
`SECURITY DEFINER`, and argues that this is *strictly stronger*: *"even with every privilege in the
cluster, the ordinary route cannot free a name."* The tests below are what turn that argument into
something a reader can check, and they are the reason this is `deep` — a privilege-shaped exception
would pass a happy-path test and hand back exactly what `ADR-0029` §4 was installed to take away.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNamePermanenceTest.kt` | modify — three new tests, no existing test edited |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — the replaced function body |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §3's first half |

## Scope

- Three new tests in the existing class, using the helpers `TASK-041006` left in place:
  `insertPlayerWithName` (registers), `forceWriteName` (raw), `clearPlayerName` (raw `SET
  display_name = NULL`), `readDisplayName`.
- A row is promoted to `RETIRED` by the permitted transition —
  `UPDATE name_registry SET reason = 'RETIRED', retired_from = ? WHERE name = ?` — run directly, not
  through `retire_display_name`. Calling the function would prove the function works, which is
  `TASK-041012`'s job; the trigger has to be shown to allow the transition on its own.
- Refusals assert `sqlState == "23001"`. The message is `display_name is permanent once set (ADR-0029,
  ADR-0051)`; assert on the code, and if a message is asserted at all assert only the stable prefix
  `display_name is permanent once set`, as `anUpdateToAnAlreadySetDisplayNameIsRejected` already
  does in `SchemaConstraintsTest`.
- Nothing existing in the file is edited.

## Out of scope

**A gap found while converting the permanence fixtures (`TASK-041006`), recorded here because this
is the ticket that owns the trigger.** `ADR-0051` §2's orphaned-registry-row defect has **two**
paths, and only one is currently guarded:

- the `UPDATE` matching zero rows — covered by `TASK-041003`'s `aRefusedSecondNameLeavesNoRegistryRow`;
- **the permanence trigger raising `23001` *after* a successful registry insert** — covered by
  nothing. A rename from a held name to a fresh one registers the new name, then the trigger refuses
  the `player` write. If that does not roll back, the fresh name is `TAKEN` by nobody, forever, and
  every assertion in `DisplayNamePermanenceTest` still passes: they check the SQLSTATE and the stored
  name, never the registry.

`TASK-041006` deliberately writes those three refused cases through a raw helper that does not
register, so it does not create the state — which is correct for that ticket and also means it does
not test it. If this ticket's work does not close the second path, it needs its own ticket.


- `retire_display_name` — `TASK-041012`.
- The ordering of the two writes inside the function. That is covered by the function's own tests;
  what is covered here is that the trigger is what makes the order matter.
- Any test that grants, revokes or changes role. The exception is deliberately not about who is
  writing, and a test that set up a second role would be asserting a design this ADR refused.

## Tests

`DisplayNamePermanenceTest`, `-PrequireDocker=true`. Three added; the seven already there must all
still pass, in particular `aNamedProfileCannotBeUnnamed`, which is the negative half of the first
test below and is **not** edited.

| Test | Proves |
| --- | --- |
| `aRetiredNameMayBeGivenUp` | Player holds `"bob"`; the `"bob"` registry row is promoted to `RETIRED`; `clearPlayerName` then **succeeds** and `readDisplayName` is `null`. This is the whole exception, and it is the only transition that changes behaviour |
| `aNameThatIsNotRetiredStillCannotBeGivenUp` | The same player, the same `clearPlayerName`, with the registry row left `TAKEN`: `23001`, and the name is still `"bob"`. Paired with the test above this is what proves the exception is conditional on the row rather than on the operation. `aNamedProfileCannotBeUnnamed` covers the same ground from before this story and stays as a second witness |
| `aRetiredNameStillCannotBecomeADifferentName` | The `"bob"` row is promoted to `RETIRED`, `"robert"` is registered, and `forceWriteName(player, "robert")` still raises `23001` with `"bob"` still stored. **This is the wrong implementation the ticket exists to catch**: an exception written as *"allow the change when the old name is `RETIRED`"* rather than *"allow it when the new name is `NULL` **and** the old name is `RETIRED`"* passes both tests above and lets an operator rename a profile — the one thing `ADR-0038` says must never happen (*"it must never end up holding a name it did not choose"*) |

## Acceptance criteria

- [ ] `DisplayNamePermanenceTest.aRetiredNameMayBeGivenUp` passes and asserts the stored name is
      `null` afterwards
- [ ] `DisplayNamePermanenceTest.aNameThatIsNotRetiredStillCannotBeGivenUp` passes and asserts
      `23001` plus the name still stored
- [ ] `DisplayNamePermanenceTest.aRetiredNameStillCannotBecomeADifferentName` passes and asserts
      `23001` plus the name still stored
- [ ] All seven pre-existing tests in the file pass with their assertions unchanged, and none is
      edited or renamed
- [ ] No test in the file references `current_user`, `SET ROLE`, `GRANT` or `SECURITY DEFINER`
- [ ] No migration file is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
