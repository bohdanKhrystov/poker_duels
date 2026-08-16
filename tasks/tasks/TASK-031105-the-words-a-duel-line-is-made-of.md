---
schema: 2
id: TASK-031105
title: The words a profile line is made of — outcome, coin, balance, and when
type: task
status: done
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, text]
depends_on: [TASK-031104]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +341 passed \(341\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names each outcome the wire can carry'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'signs a coin delta the way the server signed it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states a balance with no plus in front of it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states when a duel finished in the browser locale'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says nothing about an instant it cannot read'
  - cd web-client && npm run check
---

## Goal

Four pure functions turn what the server sent into what a player reads: the outcome word, the signed
coin delta, the plain balance, and the finishing time in the browser's own locale — with the raw
ISO instant never among them.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-text.ts` | create |
| `web-client/src/profile/profile-text.test.ts` | create |
| `web-client/src/result/outcome-text.ts` | read — the precedent, including the exact minus glyph it prints |
| `web-client/src/profile/recent-duels.ts` | read — `DuelOutcomeWord` |

## Scope

- Four exported functions, no React, no state, no `Date.now()`:

  ```ts
  /** The outcome in the reader's words: Won, Lost, Drew. */
  export function outcomeWord(outcome: DuelOutcomeWord): string;

  /** A coin delta, signed as the server signed it: `+1`, `−1`, `0`. */
  export function coinDeltaText(delta: number): string;

  /** A balance, stated: `7`, `0`, `−1`. No plus, and never clamped (`ADR-0014`). */
  export function coinBalanceText(balance: number): string;

  /** When the duel finished, in the reader's locale. `""` if it cannot be read. */
  export function finishedAtText(
    instant: string,
    options?: { readonly locales?: string; readonly timeZone?: string },
  ): string;
  ```

- The minus is **U+2212 MINUS SIGN**, not the ASCII hyphen — `result/outcome-text.ts` already prints
  `−1 duel coin` with it, and two spellings of one figure on two screens is a defect nobody sees
  until it is on a design review. Write it in the source as `"−"` with a comment naming it.
- `outcomeWord` is an exhaustive `switch` over the three words, with no `default` — a fourth word on
  the wire must stop the build, not fall through to a placeholder.
- `finishedAtText` builds one `Date` and formats it with
  `{ dateStyle: "medium", timeStyle: "short" }`, passing `options?.locales` and `options?.timeZone`
  straight through. An unreadable instant (`Number.isNaN(date.getTime())`) is `""` — the row then
  shows no time rather than the words *Invalid Date*.
- The `options` parameter exists so a test can pin a locale and a zone. Screens call
  `finishedAtText(duel.finishedAt)` and get the browser's own.

## Out of scope

- Relative times — *2 hours ago* — which need a clock the client would then have to inject, and a
  refresh to stay true. `ADR-0032`'s store holds no clock and this module takes none.
- Pluralising *hand* / *hands*. The row prints the count beside a fixed word; `DuelResult` already
  pluralises for the result screen and nothing here duplicates that rule.
- Any word for the duel's opponent. There is none on the wire (`ADR-0021`, `DEC-017`).
- Colour. `EPIC-06` authored no design for this strip; `TASK-031107` composes tokens only.

## Tests

`web-client/src/profile/profile-text.test.ts`, describe block `"the profile's words"`.

| Test | Proves |
| --- | --- |
| `names each outcome the wire can carry` | all three: `WON` → `Won`, `LOST` → `Lost`, `DREW` → `Drew`. The type has exactly three and the test names three |
| `signs a coin delta the way the server signed it` | `1` → `"+1"`, `-1` → `"−1"`, `0` → `"0"`. The minus is asserted **as an escape**, never as a pasted glyph, so an ASCII hyphen cannot pass by looking right |
| `states a balance with no plus in front of it` | `7` → `"7"`, `0` → `"0"`, `-1` → `"−1"`. Two of the three differ from `coinDeltaText`, which is why they are separate functions |
| `states when a duel finished in the browser locale` | with `{ locales: "en-GB", timeZone: "UTC" }`: `"2026-08-14T21:03:05Z"` gives text containing `"Aug"`, `"2026"` and `"21:03"`; `"2026-01-02T07:00:00Z"` gives text containing `"Jan"` and `"07:00"`. **Two instants**, so a constant cannot pass, and each result contains neither the raw instant string it came from nor a `T` between two digits |
| `says nothing about an instant it cannot read` | `"not a date"` gives `""` |

The fourth test asserts *parts*, not the whole formatted string: the exact spacing and separators
of a locale are ICU's and change between Node releases, while the month name, the year and the
24-hour time do not. That is a deliberate choice, not laziness — pinning the whole string would
make this test fail on a Node upgrade that broke nothing.

Five tests added. Three hundred and thirty-six exist, so the suite reports **341**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 341 passed (341)` | five ran and nothing else moved |
| the five `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks under `strict`, lints, is formatted |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Spell the minus as the ASCII `"-"` → `signs a coin delta the way the server signed it` and
   `states a balance with no plus in front of it` both fail.
2. Return `instant` unchanged from `finishedAtText` → `states when a duel finished in the browser
   locale` fails on the "contains no raw instant" half while its `"2026"` half still passes.
3. Give `outcomeWord` a `default` branch returning `"Played"` → nothing fails, which is the point:
   delete a `case` instead and the **build** fails. Say in the PR which one you did.

## Acceptance criteria

- [ ] `the profile's words > names each outcome the wire can carry` passes
- [ ] `the profile's words > signs a coin delta the way the server signed it` passes
- [ ] `the profile's words > states a balance with no plus in front of it` passes
- [ ] `the profile's words > states when a duel finished in the browser locale` passes
- [ ] `the profile's words > says nothing about an instant it cannot read` passes
- [ ] Every minus a player reads is `−` (U+2212), and neither file contains the string literal
      `"-1"` written with an ASCII hyphen
- [ ] `outcomeWord` has no `default` branch
- [ ] `profile-text.ts` imports nothing from React and names no global clock
- [ ] `npm run --silent test` reports `Tests  341 passed (341)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
