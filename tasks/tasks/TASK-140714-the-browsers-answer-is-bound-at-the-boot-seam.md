---
schema: 2
id: TASK-140714
title: The browser's answer is bound at the boot seam, and the fake keeps up
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140713]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'export function nameAskSkippedHere(): boolean {' src/main.tsx
  - cd web-client && grep -qF 'export function skipNameAskHere(): void {' src/main.tsx
  - cd web-client && test "$(grep -c 'pd.nameAskSkipped' src/main.tsx || true)" = "0"
  - cd web-client && grep -qF 'nameAskSkippedHere' src/App.test.tsx
  - cd web-client && grep -qF 'skipNameAskHere' src/App.test.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx > "${TMPDIR:-/tmp}/app-after.txt" 2>&1; grep -qF 'Tests  36 passed (36)' "${TMPDIR:-/tmp}/app-after.txt"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts > "${TMPDIR:-/tmp}/ok-after.txt" 2>&1; grep -qF 'Tests  5 passed (5)' "${TMPDIR:-/tmp}/ok-after.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`main.tsx` binds the skipped key to the browser's real `localStorage` and exposes the two functions
the lobby will call — `nameAskSkippedHere()` and `skipNameAskHere()` — and `App.test.tsx`'s fake of
that module names both. Neither is called yet; `TASK-140715` is the caller, and this ticket exists
so that ticket can be two files instead of four.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§5, `web-client/src/profile/name-ask-skipped.ts`, and `main.tsx`'s existing `offerSettledHere` /
`settleOfferHere` pair, which is the shape this one copies. **Nothing outside the table above is
changed.**

## Why this ticket exists at all

`App.test.tsx` mocks `./main` with an **explicit factory**, not a spread of the real module, so any
export `Lobby` imports that the factory does not name throws at render:

> `Error: [vitest] No "nameAskSkippedHere" export is defined on the "./main" mock.`

Measured on `develop` at `9dd8571c` with a one-line probe: **24 of that file's 36 tests fail**, whole
suite `24 failed | 1159 passed (1183)`, every failure in that one file. A mock factory may name an
export **before** anything imports it, so splitting the binding from the import keeps every
intermediate state green and keeps `TASK-140715` inside the ordinary three-file cap. Landing them
together would be a four-file ticket claiming `atomic:` for a split that was available.

## Scope

- **Two module-scope functions in `main.tsx`**, beside the offer's pair and shaped exactly like it:

  ```ts
  export function nameAskSkippedHere(): boolean;
  export function skipNameAskHere(): void;
  ```

  Each delegates to `readNameAskSkipped` / `markNameAskSkipped` with a `Storage` this file binds.
- **A binding of its own, not the offer's.** Declare `const askStorage: Storage = localStorage ?? nullStorage;`
  beside `offerStorage` rather than reusing it, with a comment saying why:
  [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
  retires the account offer whole, so `offerStorage` and its two functions leave the product in
  `STORY-1408`, and a shared constant would drag this story's code into that deletion.
- **The reach for the global lives here and only here** (`DEC-032`): a component that reached for
  `localStorage` itself would be a component whose tests do not test the browser, and the `?? nullStorage`
  fallback is what keeps a Node environment without one from throwing at boot.
- **`main.tsx` never spells the key literal** — `name-ask-skipped.ts` owns it, and the ownership gate
  in `one-module-owns-each-storage-key.test.ts` fails if a second production file does. A `verify:`
  gate pins zero occurrences here.
- **`App.test.tsx`'s factory gains the two names**, `nameAskSkippedHere: () => false` and
  `skipNameAskHere: vi.fn()`, beside `offerSettledHere` and `settleOfferHere`, which stand there for
  exactly the same reason. No test in that file changes, and its count stays **36**.

## Out of scope

- **Calling either function.** `Lobby.tsx` is not opened — `TASK-140715`.
- **`Lobby.test.tsx`.** It mocks `../main` by spreading `importOriginal()`, so it picks the two
  functions up with no edit at all; only the explicit factory needed one.
- **Anything about the account offer**, which `STORY-1408` retires.

## Tests

None of its own. `main.tsx` is the boot file and has no test; this ticket's gates are that
`App.test.tsx` still reports **36 passed**, that the two exports exist, that the key literal appears
in neither file, and that the ownership gate still names one owner.

## Acceptance criteria

- [ ] `main.tsx` exports `nameAskSkippedHere(): boolean` and `skipNameAskHere(): void`, both bound to
      a `Storage` this file resolves
- [ ] `askStorage` is a binding of its own, with a comment naming `ADR-0125`/`STORY-1408` as the
      reason it is not `offerStorage`
- [ ] `pd.nameAskSkipped` appears **zero** times in `main.tsx`, and
      `npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts` still reports
      `Tests  5 passed (5)`
- [ ] `App.test.tsx`'s `./main` factory names both functions, and
      `npx vitest run src/App.test.tsx` reports `Tests  36 passed (36)`
- [ ] No test in `App.test.tsx` is added, removed or edited
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
