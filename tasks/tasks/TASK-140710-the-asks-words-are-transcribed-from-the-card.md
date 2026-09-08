---
schema: 2
id: TASK-140710
title: The ask's words are transcribed from the card
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140709]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - perl -ne 'print "$1\n" while m{<div class="hero-h">([^<]*)</div>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-heading.txt"
  - perl -ne 'print "$1\n" while m{<p class="what">([^<]*)</p>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-what.txt"
  - perl -ne 'print "$1\n" while m{<p class="keeps">([^<]*)</p>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-keeps.txt"
  - perl -ne 'print "$1\n" while m{<button class="btn fill">([^<]*)</button>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-take.txt"
  - perl -ne 'print "$1\n" while m{<button class="btn ghost">([^<]*)</button>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-skip.txt"
  - perl -ne 'print "$1\n" while m{<label>([^<]*)</label>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-label.txt"
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-heading.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-what.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-keeps.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-take.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-skip.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-label.txt" web-client/src/profile/name-ask-text.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-heading.txt" web-client/src/profile/name-ask-text.test.ts
  - grep -qFf "${TMPDIR:-/tmp}/pd-ask-skip.txt" web-client/src/profile/name-ask-text.test.ts
  - cd web-client && test "$(grep -ci 'permanent' src/profile/name-ask-text.ts || true)" = "0"
  - cd web-client && test "$(grep -c 'PERMANENCE_LINE' src/profile/name-ask-text.ts || true)" = "0"
  - cd web-client && test "$(grep -c '^import' src/profile/name-ask-text.ts || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/name-ask-text.test.ts > "${TMPDIR:-/tmp}/nt-after.txt" 2>&1; grep -qF 'Tests  1 passed (1)' "${TMPDIR:-/tmp}/nt-after.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/name-ask-text.ts` holds the six strings the ask says, transcribed
byte-for-byte from `design/screens/name-ask.html`, and a golden test states each of them as a
literal. Nothing downstream re-spells a word: `TASK-140711` and `TASK-140712` import these names.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-ask-text.ts` | create |
| `web-client/src/profile/name-ask-text.test.ts` | create |

Read `design/screens/name-ask.html` — the merged card is where every string in this ticket comes
from — and `web-client/src/result/account-offer-text.ts` with its test, which is the shape this
module copies: constants with a KDoc naming what each one owes, and one test asserting the exact
export set and every literal. **Nothing outside the table above is changed.**

## Scope

- **Six exported constants, and no seventh:**

  | Constant | The card's element |
  | --- | --- |
  | `NAME_ASK_HEADING` | `<div class="hero-h">` |
  | `NAME_IS_FOR` | `<p class="what">` — what a name is for (`ADR-0119` §3, obligation 1) |
  | `NAME_KEEPS` | `<p class="keeps">` — `ADR-0130` §5's pair |
  | `NAME_FIELD_LABEL` | `<label>` |
  | `TAKE_NAME_LABEL` | `<button class="btn fill">` |
  | `SKIP_AND_PLAY_LABEL` | `<button class="btn ghost">` |

- **Transcribed, not composed.** Each value is a single string literal, copied from the card
  character for character. Six `verify:` gates extract the card's own strings and require each to
  appear in this module, so a re-worded transcription fails here rather than on screen.
- **The test states the literals, and the module is what references constants.** The test writes
  each string out in full — a golden test that imported the constant it is checking would assert
  nothing — and also pins the exact export set with
  `expect(Object.keys(text).sort()).toEqual([...])`, so a seventh export or a missing one fails even
  though every literal still matches.
- **The module imports nothing.** These are strings, and a text module that reached for
  `name-text.ts` would be the first step toward two vocabularies for one product.
- **The word *permanent* appears nowhere, and neither does `PERMANENCE_LINE`.** `ADR-0130` §5
  supersedes `ADR-0119` §3's obligation 2 and takes that sentence out of the product; two gates
  refuse both spellings.
- **No refusal sentence lives here.** `refusalSentence` and `mayTryAgain` ship in `name-text.ts` and
  are `ADR-0052`'s golden strings; the ask calls them (`TASK-140712`). Adding one here would be the
  second vocabulary `ADR-0119` §3 forbids.

## Out of scope

- **Rendering** — `TASK-140711`.
- **`name-text.ts`**, its refusal sentences and `PERMANENCE_LINE`: read, never edited, and the
  retirement of that constant is `STORY-1409`'s.
- **The card.** If a word reads wrong, the repair is a ticket against
  `design/screens/name-ask.html` first and this module second — never this module alone, or the two
  drift and the gates above stop meaning anything.

## Tests

`web-client/src/profile/name-ask-text.test.ts`, `describe("the ask's words")`

| Test | Proves |
| --- | --- |
| `states every string exactly, character for character, and exports no seventh` | the six constants equal the card's six strings as literals, and `Object.keys` is exactly those six names |

## Acceptance criteria

- [ ] `npx vitest run src/profile/name-ask-text.test.ts` reports `Tests  1 passed (1)`
- [ ] Each of the card's six strings — heading, `what`, `keeps`, `<label>`, `btn fill`, `btn ghost` —
      appears verbatim in `name-ask-text.ts`, checked by `grep -Ff` against the card itself
- [ ] The heading and the skip label appear verbatim in the **test** file too, written as literals
- [ ] `Object.keys` of the module is exactly the six names in the table above
- [ ] `name-ask-text.ts` contains no `import`, no `PERMANENCE_LINE` and no occurrence of *permanent*
      in any case
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
