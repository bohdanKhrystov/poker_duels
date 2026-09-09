---
schema: 2
id: TASK-140917
title: The form leaves the front door
type: task
status: ready
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-140916]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -qF "NameSurface" web-client/src/lobby/Lobby.tsx'
  - grep -qF "setName={setName ?? undefined}" web-client/src/lobby/Lobby.tsx
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (105 tests)"'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/e2e/claimed-here-recovered-there.test.tsx 2>&1 | grep -qF "claimed-here-recovered-there.test.tsx  (8 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The front door renders the profile strip and no name form; `ADR-0130` §6's *`NameSurface` moves to
the account screen and leaves the front door* is true, and the duplication `Lobby.tsx:433-436`
renders today is over.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Two, and it owns the tests its change invalidates. **Measured**: deleting the render and its import
fails `tsc` with `TS6133` unless `setName` still has a consumer — `TASK-140915` gave it one — and
then fails exactly two `Lobby.test.tsx` cases and nothing else in the client. The arc that also
broke was repaired by `TASK-140916`, one ticket earlier, and its count is a gate here so a
regression is caught rather than assumed.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §6;
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md) §5 —
the strip stays and is what the front door keeps.

## Scope

- **Delete the `<NameSurface …>` render** and the `import { NameSurface }` line from `Lobby.tsx`.
  `{profile !== null && <ProfileStrip state={profile} />}` immediately above it **stays**.
- **`shows the name surface beside the strip, and only with a profile to show` is rewritten**, not
  deleted, into `shows the strip and no name form on the front door`: with a `profile` state in
  hand, `queryByLabelText("your display name")` is `null` while `your profile` is found. Its merged
  sibling assertion — that a lobby showing a duel shows neither — is kept.
- **`renders the lobby with no headings from the name surface` is rewritten** into
  `renders the front door with one heading and the name printed once`: `TestPlayer` now appears
  **once**, in the strip, not twice; the heading count stays at one and is still the wordmark. The
  old comment explaining the count of two is replaced rather than left contradicting the assertion.
- **105 tests stay 105.** Both cases are rewritten in place.

## Out of scope

- **`ProfileStrip`.** `ADR-0125` §5 keeps its `profile` branch; nothing here touches it.
- **The account screen.** `TASK-140914` and `TASK-140915` put the form there.
- **`NameSurface` itself.** `TASK-140918`.
- **Any other `Lobby.test.tsx` case.** 103 of them do not mention the surface and must be untouched.

## Tests

`Lobby.test.tsx` — 105 tests after `TASK-140915`, 105 after this ticket.

| Test | Proves |
| --- | --- |
| `shows the strip and no name form on the front door` | *(rewritten)* with a profile in hand the front door renders `your profile` and no `your display name` |
| `renders the front door with one heading and the name printed once` | *(rewritten)* a profile named `TestPlayer` is printed exactly once, and the page has exactly one heading, the wordmark |
| `puts the name form on the account screen, wired to the same handler the door holds` | *(unchanged, must stay green)* the form is still reachable, one door away |

## What would still pass if the coder got it wrong

- **If the render were deleted and the surface were unreachable everywhere**, both rewritten tests
  pass — they only assert absence. `TASK-140915`'s wiring test is what fails, and it is named as a
  must-stay-green here for exactly that reason: a deletion ticket whose gates are all negative can
  delete a feature instead of moving it.
- **If `ProfileStrip` were deleted along with the surface**, `queryByLabelText("your display name")`
  is still `null` and the first test passes on its absence half. The **same test** asserts
  `your profile` is found, which is why both halves live in one case rather than two.
- **The `TestPlayer` count moves from 2 to 1**, and 1 is not a value the bug leaves unchanged: a
  front door that still rendered the surface reads 2 and fails. The count is asserted with
  `findAllByText(...).toHaveLength(1)` rather than `getByText`, because `getByText` throws on
  multiple matches and would report a confusing error rather than the count.
- **If the arc regressed**, nothing in `Lobby.test.tsx` notices. Its per-file count and its pass are
  `verify` commands here, not an assumption about `TASK-140916` having held.

## Acceptance criteria

- [ ] `Lobby.test.tsx.shows the strip and no name form on the front door` passes, asserting both the
      absence of the form and the presence of the strip
- [ ] `Lobby.test.tsx.renders the front door with one heading and the name printed once` passes
- [ ] `Lobby.test.tsx.puts the name form on the account screen, wired to the same handler the door
      holds` passes unchanged
- [ ] `Lobby.test.tsx` reports exactly 105 tests, all passing
- [ ] `claimed-here-recovered-there.test.tsx` reports exactly 8 tests, all passing, and is not edited
- [ ] `NameSurface` appears nowhere in `Lobby.tsx`, and `setName={setName ?? undefined}` still does
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
