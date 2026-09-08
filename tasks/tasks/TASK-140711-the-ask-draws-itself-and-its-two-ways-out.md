---
schema: 2
id: TASK-140711
title: The ask draws itself, and both ways out end in the duel
type: task
status: backlog
parent: STORY-1407
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140710]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'const { random = Math.random } = props;' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'useState(() => suggestName(random))' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'aria-label="choose your name"' src/profile/NameAsk.tsx
  - cd web-client && test "$(grep -c '<h2' src/profile/NameAsk.tsx || true)" = "1"
  - cd web-client && test "$(grep -ciE 'permanent|PERMANENCE_LINE' src/profile/NameAsk.tsx || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/ask-after.txt" 2>&1; grep -qF 'Tests  4 passed (4)' "${TMPDIR:-/tmp}/ask-after.txt"
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{onClick=\{props\.onSkip\}}{onClick={() => { void props.setName(""); props.onSkip(); }}}' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/a1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 3 passed (4)' "${TMPDIR:-/tmp}/a1.txt" && grep -qF 'sends nothing at all when the player skips' "${TMPDIR:-/tmp}/a1.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/profile/NameAsk.tsx` is the screen `ADR-0119` §1 puts between a press and the room:
one heading, the two sentences the card fixed, a field already holding a drawn suggestion, and two
controls — take this name, or skip and play. Taking one sends the shipped write and tells the caller
once the **server** has said yes; skipping sends nothing at all.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameAsk.tsx` | create |
| `web-client/src/profile/NameAsk.test.tsx` | create |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§2–4, [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§5, 7,
`design/screens/name-ask.html`, and `web-client/src/result/AccountOffer.tsx` — the shipped
two-ways-out surface this one is shaped like: props in, no storage, no navigation, one section.
**Nothing outside the table above is changed** — `NameSurface.tsx`, `name-text.ts` and `Lobby.tsx`
are not opened.

## Scope

- **The props, and nothing else:**

  ```tsx
  export function NameAsk(props: {
    readonly setName: (name: string) => Promise<SetNameOutcome>;
    readonly onNamed: () => void;
    readonly onSkip: () => void;
    readonly random?: () => number;
  }): ReactElement;
  ```

  No profile, no storage, no send: the surface asks a question and reports the answer. What the
  caller does with either answer is `TASK-140715`'s.
- **`const { random = Math.random } = props;`** — the default lives at the ask's own call site
  (`ADR-0137` §7), so the module that draws stays pure and a test injects an exact source. A
  `verify:` gate holds that line.
- **One draw per mount, in the initialiser:** `const [drawn] = useState(() => suggestName(random));`
  — the setter arrives in `TASK-140713`, which is the only ticket that redraws. `ADR-0137` §5
  requires the draw to happen once, never in a render body; a **source gate** holds that spelling,
  because no rendering test can tell the two apart — the field is controlled, so its value comes
  from state either way. The gate is the honest instrument here and the ticket says so rather than
  pretending a test covers it.
- **The field is the player's the moment they type.** The drawn string is the field's **initial
  value** and nothing more (`ADR-0119` §4): the field is controlled by
  `const [value, setValue] = useState(drawn);`, an `onChange` sets it, and no effect, no timer and no
  later draw ever writes over it in this ticket. Those two names — `drawn` and `value` — are the
  ones `TASK-140713`'s gates and mutations read, so keep them.
- **What it draws**, in this order, inside one `<section aria-label="choose your name">`: a single
  `<h2>` holding `NAME_ASK_HEADING`; `NAME_IS_FOR`; `NAME_KEEPS`; a `<form>` holding the labelled
  field (`NAME_FIELD_LABEL`, `htmlFor`/`id` paired) and a submit button reading `TAKE_NAME_LABEL`;
  and, **outside the form**, a `<button type="button">` reading `SKIP_AND_PLAY_LABEL`. Outside,
  deliberately: a skip inside the form would be reachable by `Enter` from the field, and a player
  pressing return over a name they typed must not land in a duel unnamed.
- **Exactly one heading, at level 2** — the level every screen this client renders uses
  (`AccountScreen`, `LadderScreen`, `HistoryScreen`). No wordmark: `ADR-0098` §1's lockup belongs to
  the front door's pre-create branch and this screen stands in place of it.
- **Submitting sends the field's string, exactly as typed** (`setDisplayName` is what canonicalises
  nothing and the server is what canonicalises everything, `ADR-0029` §2), and calls `onNamed()`
  **only** on the `named` outcome, **after** the promise settles. Every other outcome is
  `TASK-140712`'s and, in this ticket, leaves the surface as it was.
- **Skipping sends nothing.** No request, no write, no storage: it calls `onSkip()` and that is all.
  This is `ADR-0119` §5's *nothing is written unless the player deliberately takes a name* at the one
  place it could be broken, and a mutation in `verify:` — a skip that calls `setName("")` — reddens
  the test that guards it.
- **No `permanent`, no `PERMANENCE_LINE`** (`ADR-0130` §5), gated.

## Out of scope

- **Refusals, `mayTryAgain`, the in-flight guard and `useReportNameWrite`** — `TASK-140712`.
- **The reroll on `conflict`** — `TASK-140713`.
- **Mounting it anywhere** — `TASK-140715`. Nothing renders `NameAsk` when this ticket merges, which
  is why every test here mounts it directly.
- **A *suggest another* control**, a labelled suggestion, or any hint about availability.
- **Styling beyond the card.** Compose the classes the client already uses on
  `AccountOffer`/`NameSurface`; mint no value and add no raw length literal — `ADR-0091` §4's fourth
  client guard fails a `-[380px]`.

## Tests

`web-client/src/profile/NameAsk.test.tsx`, `describe("the name ask")`

| Test | Proves |
| --- | --- |
| `puts a drawn suggestion in the field` | with `random` scripted to `[0, 0, 0]` the field holds `NAME_VOCABULARY.map(l => l[0]).join(" ")` — built from the vocabulary, not copied as a literal — and a **second** scripted source gives a different exact string |
| `keeps what the player types` | after typing, the field holds the typed string and no later render restores the draw |
| `tells the caller only once the server has said yes` | `setName` is called once with the string in the field; `onNamed` is **not** called while the promise is pending and is called exactly once after it resolves `named`; `onSkip` is never called |
| `sends nothing at all when the player skips` | pressing the skip control calls `onSkip` once and `setName` **zero** times |

## Acceptance criteria

- [ ] All four tests pass: `npx vitest run src/profile/NameAsk.test.tsx` reports `Tests  4 passed (4)`
- [ ] `puts a drawn suggestion in the field` asserts **two** different exact strings from two
      different scripted sources
- [ ] `tells the caller only once the server has said yes` asserts `onNamed` uncalled while the write
      is in flight **and** called once after it resolves
- [ ] A skip that also calls `setName` fails `sends nothing at all when the player skips` and nothing
      else: `Tests  1 failed | 3 passed (4)`, file restored byte-for-byte
- [ ] `NameAsk.tsx` contains `const { random = Math.random } = props;` and
      `useState(() => suggestName(random))` verbatim
- [ ] `NameAsk.tsx` holds exactly one `<h2`, the section's `aria-label` is `choose your name`, and
      the skip control is outside the `<form>`
- [ ] Neither *permanent* nor `PERMANENCE_LINE` appears in `NameAsk.tsx`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
