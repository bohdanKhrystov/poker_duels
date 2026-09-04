---
schema: 2
id: TASK-131105
title: The lobby rules on the ask, and the refusal restores the address in one act
type: task
status: ready
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby]
depends_on: [TASK-131104]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 80) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - sh -c '! grep -qF "seatedByAFrame" web-client/src/lobby/Lobby.tsx'
  - sh -c '! grep -qF "screen, mailedToken" web-client/src/lobby/Lobby.tsx'
  - awk '{ n += gsub(/useLayoutEffect/, "&") } END { exit (n < 2) }' web-client/src/lobby/Lobby.tsx
  - awk '{ n += gsub(/roomStanding\(/, "&") } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - awk '{ n += gsub(/rulingOn\(/, "&") } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - grep -qF 'shown === "verify"' web-client/src/lobby/Lobby.tsx
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`Lobby` asks `rulingOn` once per render what happens to the address it is looking at, and a refusal
restores `/` in the same commit that refused it — so an ask over a **waiting** or **finished** room
stops being erased.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §§2–4, and
`web-client/src/routing/room-standing.ts`. Nothing else.

## Scope

- Compute, **once, above every branch**:

  ```tsx
  const standing = roomStanding(state, roomAwaited);
  const ruling = rulingOn(screen, standing);
  const shown = ruling === "honour" ? screen : "first";
  ```

  `roomAwaited` comes from `useRoomAwaited()`.

- **Delete `seatedByAFrame` and its `useEffect`.** In its place:

  ```tsx
  useLayoutEffect(() => {
    if (ruling === "refuse") leave();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ruling]);
  ```

  `useLayoutEffect`, not `useEffect`, is the whole of `ADR-0114` §3 and the comment must say why: a
  layout effect runs synchronously after the commit that refused the ask and **before the browser
  paints it**, which is the only thing in a React tree that makes `ADR-0112` §2's *"these are one
  act, not two"* true. Swapping it back breaks nothing any test can name, so the reason lives in the
  comment.
- `leave()`, never a raw `history.replaceState` — `replaceState` fires neither `popstate` nor
  `hashchange`, so a raw call would restore the address and leave every `useScreen()` caller
  rendering the stale screen (`ADR-0076` §5). `use-screen.ts` and `screen.ts` are **not modified**.
- The dependency array is `[ruling]` and `leave` is deliberately omitted, with the same
  `eslint-disable` and the same reason the token effect beside it already gives.
- **Re-key the merged token effect from `screen` to `shown`**, condition and dependency array both.
  This is not optional and not separable: the restore is now a layout effect and the token effect is
  passive, so keyed on `screen` it would fire *after* the restore and write `#/verify` back over the
  `/` that had just been written — two writers of one address, with the wrong one last
  (`ADR-0114` §3). Keyed on `shown` it cannot fire unless the ask was honoured.

## Out of scope

- **Moving any branch.** The six chosen-screen branches stay exactly where they are and keep testing
  `screen`; `TASK-131106`, `TASK-131108`, `TASK-131110` and `TASK-131111` move them, one contiguous
  block at a time. Until then a held room still wins the render, and the tests below assert the
  address and the store rather than the screen for exactly that reason.
- **The mailed hold's proof.** `rulingOn` already answers `hold`, but the `verify` branch still tests
  `screen`, so a mailed link over an unknown room still mounts and still spends. That is today's
  behaviour, not a new one; `TASK-131106` closes it and `TASK-131107` proves it. Do not write a
  `verifyEmail` call-count assertion here — it would have to assert the wrong number.
- **`/` rendering nothing.** `ADR-0118` §2 is `TASK-131112`'s. The fall-through is untouched here.
- **A notice, a dialog or any new string.** `ADR-0112` §2: the refusal is silent. If this ticket
  finds it wants a sentence, that is a stop and an ADR (`ADR-0105` §4), not a string.

## Tests

Three more `it` blocks in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `refuses an ask made while the duel is running and restores the address` | store fed `RoomJoined` + `Snapshot`; render; then `act(() => { window.location.hash = "#/leaderboard"; })` → `Pot 30` is still on screen and `window.location.hash` reads `""`. `ADR-0114` §7's *Refused*, driven from the address the way a player typing one drives it |
| `keeps the address a player chose over a room whose duel is not running` | store fed `RoomJoined` **alone**; render; then the same assignment → `window.location.hash` still reads `"#/leaderboard"`, and `store.getState().roomCode` is still `"ABCDEFGH"`. The screen is deliberately not asserted: the branch order has not moved yet |
| `moves nothing on screen when it refuses` | the running-duel case again, comparing `container.textContent` captured **before** the assignment with the same value after → identical. One assertion for *silently, with nothing added*: a notice, a dialog or any new string fails it |

`render()` returns `container`; take it from there rather than adding a `data-testid` (`ADR-0100`
§5).

**These four merged tests must still pass unchanged, and none of their assertions moves:**
`shows the duel to a player a frame seats, whatever address they were reading`,
`replaces the address a frame overruled, and stacks no entry doing it`,
`leaves the address alone while no frame has seated anybody`, and
`lets a frame that seats this tab outrank a mailed link`. Each is still true under the new
predicate — the first three because `standing` reads `running` and `none` exactly where
`seatedByAFrame` read `true` and `false`, the fourth because `shown` is `"first"` on a refusal and
the re-keyed token effect therefore cannot fire. If one of them goes red, the predicate is wired
wrong; do not edit the test.

## Acceptance criteria

- [ ] All three tests above exist under those exact names and pass
- [ ] `Lobby.test.tsx` reports at least 80 tests and `App.test.tsx` at least 36
- [ ] `Lobby.tsx` contains no `seatedByAFrame` and no `[screen, mailedToken]` dependency array
- [ ] `Lobby.tsx` names `useLayoutEffect` at least twice (the import and the call)
- [ ] `roomStanding(` and `rulingOn(` each appear exactly once in `Lobby.tsx`
- [ ] The token effect's condition tests `shown`, not `screen`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
