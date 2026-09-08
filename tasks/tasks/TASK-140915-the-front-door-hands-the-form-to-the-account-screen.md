---
schema: 2
id: TASK-140915
title: The front door hands the form to the account screen
type: task
status: backlog
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account]
depends_on: [TASK-140914]
verify:
  - cd web-client && npm ci
  - grep -qF "setName={setName ?? undefined}" web-client/src/lobby/Lobby.tsx
  - sh -c 'test $(grep -c "NameSurface" web-client/src/lobby/Lobby.tsx) -eq 2'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qF "Lobby.test.tsx  (105 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Opening `Account` from the front door shows the name form there, wired to the same `setName` the
front door already holds — so the form stands where `ADR-0130` §6 puts it before anything is taken
away from where it stands today.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Two. The front-door render **stays** in this ticket, which is what keeps the diff to two files:
deleting it in the same commit as the wiring drags `web-client/src/e2e/claimed-here-recovered-there.test.tsx`
into the budget as well, and deleting it *without* the wiring leaves `setName` unused and fails
`tsc` with `TS6133` — both measured.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §6;
`web-client/src/account/AccountScreen.tsx` — the optional `setName` prop.

## Scope

- **One prop on the `<AccountScreen>` element** in the `shown === "account"` branch:
  `setName={setName ?? undefined}`. `Lobby` holds `setName` as `… | null`; `AccountScreen` takes
  `… | undefined`, and the coalesce is the whole of the translation.
- **The front-door `<NameSurface>` render stays.** For the length of one ticket a named player can
  reach the surface from either screen; both render the same component against the same handler, and
  the screens never appear together — `shown === "account"` returns early.
- **One new `Lobby.test.tsx` case**, so the wiring has a gate of its own before the deletion removes
  the old one. 104 tests become 105.

## Out of scope

- **Deleting the front-door render.** `TASK-140917`.
- **The whole-client arc.** `TASK-140916`.
- **Any change to `AccountScreen` or `NameSurface`.**

## Tests

`Lobby.test.tsx` — 104 tests on `develop` at `1c3c7fd9`, 105 after.

| Test | Proves |
| --- | --- |
| `puts the name form on the account screen, wired to the same handler the door holds` | render the lobby with a `profile` state and a `setName` spy, press `Account`, find `your display name` inside the `account` region, type a name, press `Set my name`, and assert the spy was called **once with that string** |

## What would still pass if the coder got it wrong

- **If the prop were passed as `undefined` unconditionally**, the region never renders and
  `findByLabelText` fails — so the test must reach the surface rather than merely assert the element
  exists somewhere on the page.
- **If the prop were wired to a different handler** — a fresh closure, a no-op — every query passes
  and only the spy assertion fails. That is why the test drives a real submit and asserts the call
  **with the typed string**, not merely that something was called: a handler that ignored its
  argument would satisfy `toHaveBeenCalled`.
- **If the surface were found on the front door instead of inside the account region**, the assertion
  would pass against today's tree without any wiring at all. The query is therefore scoped with
  `within(...)` to the element labelled `account`, which does not exist until `Account` is pressed.
- **If the front-door render were deleted here as well**, `shows the name surface beside the strip,
  and only with a profile to show` and `renders the lobby with no headings from the name surface`
  both fail, and the count gate reads 103 rather than 105. Both are `TASK-140917`'s to move.

## Acceptance criteria

- [ ] `Lobby.test.tsx.puts the name form on the account screen, wired to the same handler the door
      holds` passes, and asserts the spy was called once with the typed string
- [ ] `Lobby.test.tsx` reports exactly 105 tests — 104 measured on `develop` at `1c3c7fd9` plus the
      one above
- [ ] `Lobby.tsx` names `NameSurface` exactly twice: the import and the front-door render
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
