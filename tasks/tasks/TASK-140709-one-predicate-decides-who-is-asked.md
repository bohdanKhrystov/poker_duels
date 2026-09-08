---
schema: 2
id: TASK-140709
title: One predicate decides who is asked for a name
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140708]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'export function askForName(input: AskInput): boolean {' src/profile/name-ask.ts
  - cd web-client && test "$(grep -c 'signedIn' src/profile/name-ask.ts || true)" = "0"
  - cd web-client && test "$(grep -cE 'localStorage|sessionStorage|window\.|document\.|fetch\(' src/profile/name-ask.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-ask.test.ts > "${TMPDIR:-/tmp}/na-after.txt" 2>&1; grep -qF 'Tests  6 passed (6)' "${TMPDIR:-/tmp}/na-after.txt"
  - cd web-client && cp src/profile/name-ask.ts "${TMPDIR:-/tmp}/na.fixed.ts" && perl -0pi -e 's{export function askForName\(input: AskInput\): boolean \{}{export function askForName(input: AskInput): boolean {\n  if (input !== null) return true;}' src/profile/name-ask.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-ask.test.ts > "${TMPDIR:-/tmp}/p1.txt" 2>&1; cp "${TMPDIR:-/tmp}/na.fixed.ts" src/profile/name-ask.ts; cmp -s "${TMPDIR:-/tmp}/na.fixed.ts" src/profile/name-ask.ts && grep -qF 'Tests  5 failed | 1 passed (6)' "${TMPDIR:-/tmp}/p1.txt"
  - cd web-client && cp src/profile/name-ask.ts "${TMPDIR:-/tmp}/na.fixed.ts" && perl -0pi -e 's{export function askForName\(input: AskInput\): boolean \{}{export function askForName(input: AskInput): boolean {\n  if (input !== null) return false;}' src/profile/name-ask.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-ask.test.ts > "${TMPDIR:-/tmp}/p2.txt" 2>&1; cp "${TMPDIR:-/tmp}/na.fixed.ts" src/profile/name-ask.ts; cmp -s "${TMPDIR:-/tmp}/na.fixed.ts" src/profile/name-ask.ts && grep -qF 'Tests  1 failed | 5 passed (6)' "${TMPDIR:-/tmp}/p2.txt" && grep -qF 'asks a player who holds no name and has not skipped' "${TMPDIR:-/tmp}/p2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/name-ask.ts` answers, in one function, the question `ADR-0119` §1's table
asks: does the ask stand at this press? One condition says yes; four inputs say no; and the
function takes no argument that could make it say yes for a reason the ADR does not name.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-ask.ts` | create |
| `web-client/src/profile/name-ask.test.ts` | create |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§1, 5, `web-client/src/profile/profile-strip.ts` (the `ProfileStripState` union) and
`web-client/src/result/account-offer.ts`, whose `offerAccount` is this predicate's shipped sibling —
an input interface, one boolean, and a KDoc saying why every condition is essential. **Nothing
outside the table above is changed.**

## Scope

- **The shape:**

  ```ts
  export interface AskInput {
    readonly profile: ProfileStripState | null;
    readonly skipped: boolean;
  }

  export function askForName(input: AskInput): boolean;
  ```

  `true` exactly when `profile.kind === "profile"`, its `displayName` is `null`, its
  `displayNameRemoved` is `false`, and `skipped` is `false`.
- **There is no `signedIn` input, and that is load-bearing.** `ADR-0119` §1's condition is *holding
  no name*, not *holding no account*, and §6 says the ask is not a claim prompt wearing a different
  hat. A `verify:` gate pins the string `signedIn` at zero occurrences in this file, so the input
  cannot arrive quietly later.
- **`displayNameRemoved` is a refusal, not an omission.** `ADR-0052` §1 puts that conversation on
  the name surface *and nowhere else*; an ask that said nothing about why their name vanished would
  be the product pretending nothing happened.
- **`null`, `no-profile` and `unavailable` all answer `false`.** Before the read lands the client
  has not been told whether this player holds a name, and `ADR-0119` §2's *"no state of this screen
  ever leaves a player unable to duel"* refuses a wait on the path into a duel. The KDoc says this in
  its own words, and names the consequence: the player meets the ask at their **next** press, which
  costs nothing, because being asked spends nothing (`ADR-0119` §5).
- **It reaches for nothing.** No storage, no window, no fetch — `skipped` arrives as a boolean the
  caller read (`TASK-140708`'s module, bound in `TASK-140713`). A gate pins that too.
- **Six tests, each flipping exactly one field of one asking input.** A refusal test that also
  changes the profile shape proves nothing about which condition fired; the asking input is built
  once by a helper and each refusal is that input with one field replaced.

## Out of scope

- **Where `skipped` comes from and where the answer is used.** `TASK-140713` binds the key;
  `TASK-140714` calls the predicate from the two front-door handlers.
- **A `signedIn` condition, a duel count, a device fact, or any second reason to ask.** `ADR-0119`
  §1 lists what is asked and §5 lists what spends the ask; nothing else may enter this function.
- **Rendering anything.** This module returns a boolean and imports no React.

## Tests

`web-client/src/profile/name-ask.test.ts`, `describe("who is asked for a name")`

| Test | Proves |
| --- | --- |
| `asks a player who holds no name and has not skipped` | the one case that answers `true` |
| `never asks a player who already holds a name` | the asking input with `displayName: "Ravenpost"` |
| `never asks a player whose name was removed` | the asking input with `displayNameRemoved: true` |
| `never asks a browser that has already skipped` | the asking input with `skipped: true` — the profile is otherwise identical, so only the skip can be what refused |
| `never asks before the profile read has landed` | `profile: null` |
| `never asks when the read answered no-profile or unavailable` | both other members of `ProfileStripState`, asserted separately in one test |

## Acceptance criteria

- [ ] All six tests pass: `npx vitest run src/profile/name-ask.test.ts` reports `Tests  6 passed (6)`
- [ ] A body that answers `true` for every input fails five of them:
      `Tests  5 failed | 1 passed (6)`, file restored byte-for-byte
- [ ] A body that answers `false` for every input fails exactly
      `asks a player who holds no name and has not skipped`: `Tests  1 failed | 5 passed (6)`, file
      restored byte-for-byte
- [ ] The string `signedIn` appears nowhere in `name-ask.ts`, and neither does `localStorage`,
      `sessionStorage`, `window.`, `document.` or `fetch(`
- [ ] Every refusal test differs from the asking input in exactly one field
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
