---
schema: 2
id: TASK-041509
title: The room code key gets the row it never had
type: task
status: backlog
parent: STORY-0415
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, storage, gate]
depends_on: [TASK-041505]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Test Files +1 passed \(1\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'only the room-memory module writes the room code key'
  - cd web-client && npm run check
---

## Goal

All four of this client's storage keys have a row in the gate that says one module owns each of
them — the fourth being `pd.roomCode`, which has had none since it was added.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/one-module-owns-each-storage-key.test.ts` | modify |

Read, and do not edit:

- [`ADR-0086`](../../docs/adr/ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md)
  §Context and §*What this does not settle* — where the gap was measured, and the sentence that asks
  for this ticket: *"`pd.roomCode` has no row in the gate. Found while measuring, out of scope to fix
  here, and named so it becomes a ticket rather than a discovery someone else makes twice."*
- `web-client/src/protocol/room-memory.ts` — `ROOM_CODE_STORAGE_KEY = "pd.roomCode"`, the owner this
  row names.

## Scope

- **One `it` block, appended last**, in the shape the three rows above it already use:

  ```ts
  it("only the room-memory module writes the room code key", () => {
    expect(productionSourcesContaining("pd.roomCode")).toEqual([
      "room-memory.ts",
    ]);
  });
  ```

- **The literal is written out**, not imported from `room-memory.ts`. The walk skips `*.test.ts`, so
  the row does not match its own file, and a row that read the constant would be asserting the
  constant against itself.
- Nothing else in the file changes: no helper, no comment, and none of the three existing rows.

## Out of scope

- **Changing `room-memory.ts`.** It already owns the key correctly; this ticket adds the row that
  proves it and would catch a second writer later.
- **Widening the scan** — no new helper, no directory argument, no assertion over the whole key set.
  The file's header comment states its two honest limits and this ticket does not move either.
- **A fifth key.** There are four.

## Tests

`web-client/src/protocol/one-module-owns-each-storage-key.test.ts` — **3 rows become 4**, the fourth
being the one above. The three merged rows are pinned by the count `Tests 4 passed (4)`, never by
name.

**No `try` in the added code, and no `expect()` inside one** — a failing assertion is itself a throw
(`TASK-041409`).

## Acceptance criteria

- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'only the room-memory module writes the room code key'`
      — passes, and the expected array **names the owner** rather than being empty
- [ ] `cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | grep -qE 'Tests +4 passed \(4\)'`
      and `… | grep -qE 'Test Files +1 passed \(1\)'` — **exactly four in exactly one file**: three
      merged plus this one. Both lines, because a collection error prints a *passing* `Tests` count
      with no failure line at all
- [ ] `cd web-client && npm run check` exits 0. With `TASK-041505`–`TASK-041508` merged the suite
      reads **836 passed (836)** over **107** files
- [ ] The three merged rows pass unchanged — this diff appends one and edits none
- [ ] No file outside the one listed differs
- [ ] Every command in `verify:` exits 0

## Proof

**Both steps were run in this worktree, on `develop` at `77c61708` with this row and nothing else
applied** — the gate file at three rows there, since `TASK-041505` had not been applied.

1. The row as written is **green**, and it is green naming the owner:
   `productionSourcesContaining("pd.roomCode")` returns `["room-memory.ts"]`. Measured,
   `Tests 3 passed (3)` on the filtered run. A repository-wide search confirms why — exactly one file
   in `web-client/src` contains the literal, and no test file contains it either, so this row is the
   first thing in the tree that would notice a second one.
2. Put `// PROBE: … "pd.roomCode"` in `web-client/src/store/boot.ts`, a production file that already
   reads and writes the room code through `room-memory.ts`'s functions. **The new row reddens alone**
   — measured, `1 failed | 2 passed (3)`, with
   `AssertionError: expected [ 'boot.ts', 'room-memory.ts' ] to deeply equal [ 'room-memory.ts' ]`.
   That is the defect the row exists for, and `boot.ts` is a realistic place for it: it is the one
   module that would plausibly inline the key rather than call the owner. Revert `boot.ts` completely.

> **No key here is a prefix of another** — `pd.deviceId`, `pd.sessionToken`, `pd.roomCode` and
> `pd.accountOfferSettled` — so the substring scan that forced `ADR-0086` §1's long name returns one
> file per row. Checked, because that is the trap this gate has.
>
> **Expect `The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.` on stderr.** It is
> wrong; `NO_COLOR=1` is what keeps the summary line plain enough for `grep -qE` to match it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why it sits under `STORY-0415`.** The key is `STORY-0305`'s and the gate is `STORY-0412`'s, but the
gap was found by `ADR-0086` while deciding this story's key, and the ADR asks for it by name. Filing
it here keeps the trail readable: the reader who asks *why is there a fourth row* lands on the ADR
that noticed the first three were only two.

**Why it is not part of `TASK-041505`.** That ticket adds the module *and* its row in one diff
because `ADR-0086` §*The deadline* requires it. This row guards a key that has been merged for
months; folding it in would take that ticket to four files with no gate holding them together, which
is a split (`ADR-0068`, `ADR-0070`). It depends on `TASK-041505` only because both edit this file, so
the two must not be in flight at once.

**Measured size: 6 changed lines.**
