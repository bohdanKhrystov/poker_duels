---
schema: 2
id: TASK-140715
title: The press is held until the ask is answered, and then it goes through
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, lobby, profile]
depends_on: [TASK-140714]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'useState<CreateRoom | JoinRoom | null>(' src/lobby/Lobby.tsx
  - cd web-client && grep -qF 'if (heldPress !== null && setName !== null) {' src/lobby/Lobby.tsx
  - cd web-client && test "$(grep -n 'if (state.roomCode !== null) {' src/lobby/Lobby.tsx | cut -d: -f1)" -lt "$(grep -n 'if (heldPress !== null && setName !== null) {' src/lobby/Lobby.tsx | cut -d: -f1)"
  - cd web-client && test "$(grep -c '<NameSurface' src/lobby/Lobby.tsx || true)" = "1"
  - cd web-client && test "$(grep -c 'skipNameAskHere()' src/lobby/Lobby.tsx || true)" = "1"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lb-after.txt" 2>&1; grep -qF 'Tests  105 passed (105)' "${TMPDIR:-/tmp}/lb-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx > "${TMPDIR:-/tmp}/app-after.txt" 2>&1; grep -qF 'Tests  36 passed (36)' "${TMPDIR:-/tmp}/app-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/e2e > "${TMPDIR:-/tmp}/e2e-after.txt" 2>&1; grep -qF 'Test Files  7 passed (7)' "${TMPDIR:-/tmp}/e2e-after.txt"
  - cd web-client && git diff --quiet -- src/e2e
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/lb.fixed.tsx" && perl -0pi -e 's{skipNameAskHere\(\);}{}' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/w1.txt" 2>&1; cp "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  1 failed | 104 passed (105)' "${TMPDIR:-/tmp}/w1.txt" && grep -qF 'records the skip and sends the press the player already made' "${TMPDIR:-/tmp}/w1.txt"
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/lb.fixed.tsx" && perl -0pi -e 's|if \(setName !== null && askForName\(\{ profile, skipped: nameAskSkipped \}\)\) \{|if (false) {|' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/w2.txt" 2>&1; cp "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/lb.fixed.tsx" src/lobby/Lobby.tsx && grep -qF 'Tests  4 failed | 101 passed (105)' "${TMPDIR:-/tmp}/w2.txt" && grep -qF 'stands the ask in place of the front door, and sends nothing' "${TMPDIR:-/tmp}/w2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two front-door controls stop sending straight to the socket. A press by a player the predicate
names is **held**: the ask stands in place of the front door, and when it is answered — either way —
the frame the player already pressed for is sent, unchanged. They do not press twice, and no room is
opened before the answer.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§1–2, 5 and `STORY-1407`'s *Design notes*. **Nothing outside the table above is changed** —
`main.tsx` already exports the two bindings (`TASK-140714`), `NameAsk` is already built, and no file
under `src/e2e/` is opened.

## Scope

- **One held frame, typed as what it is:**

  ```tsx
  const [heldPress, setHeldPress] = useState<CreateRoom | JoinRoom | null>(
    null,
  );
  ```

  Not `ClientMessage`: the ask can only ever release a frame that starts a duel, and the type is
  what says so. The wrapping above is prettier's — **measured**: that declaration is 80 columns on
  one line and the formatter breaks it — so the `verify:` gate holds the substring
  `useState<CreateRoom | JoinRoom | null>(`, which survives the wrap. Both `CreateRoom` and
  `JoinRoom` are type-only imports from `../protocol`.
- **One browser answer, read once per mount:** `const [nameAskSkipped, setNameAskSkipped] = useState(nameAskSkippedHere);`
  — the initialiser, not a call, exactly as `offerSettled` above it, so a skip takes effect on this
  render rather than on the next boot.
- **One helper, called by both controls:**

  ```tsx
  const startDuel = (intent: CreateRoom | JoinRoom): void => {
    if (setName !== null && askForName({ profile, skipped: nameAskSkipped })) {
      setHeldPress(intent);
      return;
    }
    send(intent);
  };
  ```

  `setName !== null` is part of the condition rather than a second guard below: with no
  `SetNameProvider` above, there is no write path, so there is nothing to ask for. The `Play duel`
  button calls `startDuel({ type: "CreateRoom" })`; the code form keeps its empty-code refusal
  **first** — an empty code must still spend nothing — and then calls
  `startDuel({ type: "JoinRoom", code })`.
- **One branch, immediately above the front door's `return` and below every other branch:**

  ```tsx
  if (heldPress !== null && setName !== null) {
    return <NameAsk setName={setName} onNamed={…} onSkip={…} />;
  }
  ```

  A `verify:` gate compares line numbers and fails unless it sits **after** `if (state.roomCode !== null)`.
  That placement is what makes *never over the table, never over the waiting room, never over the
  result, never over a chosen screen* structural: a frame that seats the player wins the branch race
  the moment it arrives, and `TASK-140716` pins it.
- **`onNamed` sends the held frame and clears it.** Nothing else: the profile the client holds was
  already brought up to date by the ask itself (`TASK-140712`), so the condition that stood the ask
  up is false by the time the player presses again.
- **`onSkip` records the answer, then sends the held frame.** `skipNameAskHere()` — once, gated at
  exactly one occurrence — then `setNameAskSkipped(true)`, then `send(heldPress)`, then clear.
  `ADR-0119` §5: skipping spends the ask, in this browser, and nothing else does.
- **`Lobby.test.tsx`'s `../main` mock gains the two names**, beside `offerWiring`: a `skipped`
  boolean the tests set and a `skip` spy they assert on. That file spreads `importOriginal()`, so
  only the two overrides are added.
- **A render helper mounts the tree the ask needs** — `ProfileProvider` over `SetNameProvider` over
  `DuelProvider` — and returns the `send` and `setName` spies. `renderLobby()` and
  `renderLobbyWithProfile()` are **not** edited: every merged press test renders without a
  `ProfileProvider`, so the predicate answers `false` and all 104 stay green, measured.

## Out of scope

- **The moments the ask never stands** — `TASK-140716`, which adds five tests and touches no
  production file.
- **`NameSurface` on the front door.** It keeps rendering exactly where it does today; the move to
  the account screen is `ADR-0130` §6 and `STORY-1409`. A gate pins it at one occurrence.
- **The invite path, the rematch and the resume.** `boot.ts`, `RematchControl` and `room-memory.ts`
  are not opened, and no test here touches them: the ask is reachable only from the two handlers
  this ticket edits, which is `ADR-0119` §1's *never shown* table held structurally.
- **Any file under `src/e2e/`.** A `verify:` gate runs `git diff --quiet` over that directory and
  the drives stay at 7 files.

## Tests

`web-client/src/lobby/Lobby.test.tsx` — the 104 merged tests unchanged, plus:

| Test | Proves |
| --- | --- |
| `stands the ask in place of the front door, and sends nothing` | a nameless player pressing `Play duel` sees the ask's region, no `Play duel` control remains, and `send` was not called |
| `sends the press the player already made once they take a name` | with the write resolving `named`, `send` is called exactly once with `{ type: "CreateRoom" }` and the ask leaves the screen |
| `records the skip and sends the press the player already made` | pressing the skip control calls `skipNameAskHere` exactly once and `send` exactly once with `{ type: "CreateRoom" }` |
| `carries the join code through the ask` | a pasted code, `Join the duel`, the ask, then the skip: `send` is called once with `{ type: "JoinRoom", code: "ABCDEFGH" }` — the code the press carried, not a re-read of the field |

## Acceptance criteria

- [ ] `npx vitest run src/lobby/Lobby.test.tsx` reports `Tests  105 passed (105)`, and no merged
      assertion in that file is edited
- [ ] `npx vitest run src/App.test.tsx` reports `Tests  36 passed (36)`
- [ ] `npx vitest run src/e2e` reports `Test Files  7 passed (7)` and `git diff --quiet -- src/e2e`
      exits 0
- [ ] The ask's branch is on a **later line** than `if (state.roomCode !== null) {`
- [ ] Removing `skipNameAskHere();` fails `records the skip and sends the press the player already
      made` and nothing else: `Tests  1 failed | 104 passed (105)`, file restored byte-for-byte
- [ ] Turning the `startDuel` condition into `if (false)` fails all four new tests and nothing else:
      `Tests  4 failed | 101 passed (105)`, file restored byte-for-byte
- [ ] `<NameSurface` still appears exactly once in `Lobby.tsx`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
