---
schema: 2
id: TASK-140716
title: The moments the ask never stands, pinned one at a time
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, lobby, profile]
depends_on: [TASK-140715]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test "$(grep -n 'if (state.roomCode !== null) {' src/lobby/Lobby.tsx | cut -d: -f1)" -lt "$(grep -n 'if (heldPress !== null && setName !== null) {' src/lobby/Lobby.tsx | cut -d: -f1)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lb-after.txt" 2>&1; grep -qF 'Tests  113 passed (113)' "${TMPDIR:-/tmp}/lb-after.txt"
  - cd web-client && git diff --quiet -- src/lobby/Lobby.tsx src/profile src/main.tsx src/e2e
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/lb.fixed.tsx" && perl -0pi -e 's{askForName\(\{ profile, skipped: nameAskSkipped \}\)}{true}' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/x1.txt" 2>&1; cp "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  4 failed | 109 passed (113)' "${TMPDIR:-/tmp}/x1.txt" && grep -qF 'never stands in a browser that already skipped' "${TMPDIR:-/tmp}/x1.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every row of `ADR-0119` §1's *where it is never shown* table that a press can reach is a passing
test, and the one the ADR's table does not have — a press made before the profile read has landed —
is recorded rather than left to be rediscovered. No production file changes: this ticket is the
proof that the previous five were enough.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§1 and `STORY-1407`'s *Design notes* — the paragraph *"The predicate, and the one row `ADR-0119` §1's
table does not have"* is what the fourth test below writes down. **Nothing outside the table above is
changed**, and a `verify:` gate runs `git diff --quiet` over `Lobby.tsx`, `src/profile`, `main.tsx`
and `src/e2e` to prove it.

## Scope

- **Five tests, no production edit.** Each presses `Play duel` in a tree whose only difference from
  `TASK-140715`'s asking case is the one fact named, and asserts the ask is absent **and** the frame
  went straight out — because *no ask* and *no duel* would also satisfy an absence check.
- **The invite path, the rematch and the resume are deliberately not tested here.** They cannot
  reach the ask: `boot.ts` sends `JoinRoom` outside React on the socket's `Welcome`,
  `RematchControl` sends `OfferRematch`, and a resumed room arrives as a frame. None of them calls
  the two handlers `TASK-140715` edited, and that is a **structural** fact rather than a
  conditional one. Writing tests that mount those paths would assert a property of the mounting, not
  of the product. The story says this; the ticket does not re-litigate it.
- **The fifth test is the branch-order one**, and it is the only one that changes state mid-test: a
  nameless player presses, the ask stands, and then a `RoomJoined` frame arrives. The waiting table
  replaces the ask in the same render, because the room branch sits above it. A line-number gate in
  `verify:` holds that order independently of the test.
- **The fourth test is the race the ADR's table does not name.** The profile read is a promise that
  never settles; the press sends at once. `ADR-0119` §2 refuses a wait on the path into a duel, and
  §5 makes being un-asked cost nothing — the player meets the ask at their next press. The test
  carries that reasoning in a comment so a later reader does not read it as an oversight.

## Out of scope

- **Any change to `Lobby.tsx`, `NameAsk.tsx`, the predicate or `main.tsx`.** If one of these tests
  cannot be made to pass, the defect is in a merged ticket and gets a ticket of its own — it is not
  repaired here.
- **A test that the ask stands twice for a player who answered neither control.** That is a reload,
  and a reload is a fresh mount: `ADR-0119` §1's row is satisfied by the state being local, which
  `TASK-140715`'s `useState` already is.
- **`docs/test-plan.md`.** The QA catalogue is the `qa-cases` skill's, from the epic's *Definition of
  done*.

## Tests

`web-client/src/lobby/Lobby.test.tsx` — the 108 unchanged, plus:

| Test | Proves |
| --- | --- |
| `never stands for a player who already holds a name` | the asking tree with `displayName: "Ravenpost"`: no ask, and `send` called once with `{ type: "CreateRoom" }` |
| `never stands for a player whose name was removed` | the asking tree with `displayNameRemoved: true`: no ask, one send — `ADR-0052` §1 keeps that conversation on the name surface and nowhere else |
| `never stands in a browser that already skipped` | the asking tree with `nameAskSkippedHere` answering `true`: no ask, one send |
| `never stands before the profile read has landed` | the asking tree whose read never settles: no ask, one send |
| `a frame that seats the player takes the screen from a standing ask` | the ask is up, `RoomJoined` arrives, and the waiting table is on screen with no ask left |

## Acceptance criteria

- [ ] `npx vitest run src/lobby/Lobby.test.tsx` reports `Tests  113 passed (113)`
- [ ] Each of the first four tests asserts **both** that the ask is absent and that `send` was called
      exactly once with the frame the press carried
- [ ] `git diff --quiet -- src/lobby/Lobby.tsx src/profile src/main.tsx src/e2e` exits 0 — this
      ticket changes one test file and nothing else
- [ ] Replacing `askForName({ profile, skipped: nameAskSkipped })` with `true` fails exactly the four
      refusal tests: `Tests  4 failed | 109 passed (113)`, file restored byte-for-byte
- [ ] The ask's branch is still on a later line than `if (state.roomCode !== null) {`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
