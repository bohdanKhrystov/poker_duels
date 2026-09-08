---
schema: 2
id: TASK-140712
title: A refusal reads in the words the product already uses, and never blocks the skip
type: task
status: done
parent: STORY-1407
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, profile, lobby]
depends_on: [TASK-140711]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && grep -qF 'if (submitInFlight.current) return;' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'hidden={!canTryAgain}' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'refusalSentence(refusal)' src/profile/NameAsk.tsx
  - cd web-client && grep -qF 'reportNameWrite(outcome)' src/profile/NameAsk.tsx
  - cd web-client && test "$(grep -c 'case \"conflict\"' src/profile/NameAsk.tsx || true)" = "0"
  - cd web-client && test "$(grep -ciE 'permanent|PERMANENCE_LINE' src/profile/NameAsk.tsx || true)" = "0"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/ask-after.txt" 2>&1; grep -qF 'Tests  8 passed (8)' "${TMPDIR:-/tmp}/ask-after.txt"
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{if \(submitInFlight\.current\) return;}{}' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/b1.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 7 passed (8)' "${TMPDIR:-/tmp}/b1.txt" && grep -qF 'sends one write for two presses' "${TMPDIR:-/tmp}/b1.txt"
  - cd web-client && cp src/profile/NameAsk.tsx "${TMPDIR:-/tmp}/ask.fixed.tsx" && perl -0pi -e 's{ hidden=\{!canTryAgain\}}{}' src/profile/NameAsk.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/profile/NameAsk.test.tsx > "${TMPDIR:-/tmp}/b2.txt" 2>&1; cp "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx; cmp -s "${TMPDIR:-/tmp}/ask.fixed.tsx" src/profile/NameAsk.tsx && grep -qF 'Tests  1 failed | 7 passed (8)' "${TMPDIR:-/tmp}/b2.txt" && grep -qF 'withdraws the form a refusal has closed, and leaves the skip standing' "${TMPDIR:-/tmp}/b2.txt"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every way `PUT /api/me/name` can refuse now reads on the ask in the words the product already
uses — `name-text.ts`'s `refusalSentence`, no second vocabulary — the form withdraws for the
refusals `mayTryAgain` closes while the **skip stays standing and working**, two presses send one
write, and an accepted name reaches the profile the client is holding on the same render.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameAsk.tsx` | modify |
| `web-client/src/profile/NameAsk.test.tsx` | modify |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§2–3, `web-client/src/profile/name-text.ts` and `web-client/src/profile/NameSurface.tsx` — the
shipped surface whose refusal handling this one copies, down to *hidden, not unmounted*. **Nothing
outside the table above is changed**: `name-text.ts` is read, never edited.

## Scope

- **One refusal at a time, replacing the last.** A `useState` holds the most recent settled
  non-`named` outcome kind; the next attempt replaces it. A player who fails twice reads one
  sentence, not a log.
- **The sentence is `refusalSentence(refusal)` and nothing else.** No `switch` on outcome kinds
  lives in this component — a `verify:` gate pins `case "conflict"` at zero occurrences — so when
  `STORY-1409` deletes the `permanent` kind and `ADR-0134`'s `403` with it, **this file does not
  change**. It is rendered in a `role="status"` paragraph, as `NameSurface` renders it.
- **`mayTryAgain` decides whether the form survives, and the form is *hidden*, not unmounted.** The
  `<form>` carries `hidden={!canTryAgain}`, exactly as `NameSurface` does and for the reason its
  comment gives: the field and the button stay the same DOM node across a settle the player can act
  on, so what they typed is never lost to a remount, and `role` queries agree there is no form the
  instant `mayTryAgain` says there is none.
- **The skip is never hidden, never disabled and never inside the form.** This is `ADR-0119` §2's
  *"every refusal `set-name.ts` can settle leaves the skip available and working"*, and it is the
  clause that keeps `ADR-0036`, `ADR-0058` and `ADR-0063` true. The test presses it **after** the
  refusal that withdrew the form and asserts `onSkip` fires.
- **Two presses send one write.** A `useRef` guard — `if (submitInFlight.current) return;` on one
  line, gated and mutated — plus a `disabled` submit while a write is in flight. `NameSurface`
  carries the same pair for the same reason, and it matters more here than there: under `ADR-0130`
  §1 a second write is a **rename**, and a rename spends a second string out of a registry that
  releases nothing.
- **`useReportNameWrite()` is called with the outcome, unconditionally.** The hook is a no-op on
  every refusal kind, so branching here would duplicate a decision the provider already makes. It is
  what stops a second press in the same tab from asking a player who now holds a name — `ADR-0119`
  §1's *"already holds a display name"* row — and it is a no-op where no provider is above, so the
  three tests that mount the component bare are unaffected.

## Out of scope

- **The reroll on `conflict`** — `TASK-140713`. This ticket prints the sentence; the next one
  redraws the field.
- **Editing `name-text.ts`**, retiring `PERMANENCE_LINE`, or removing the `permanent` outcome —
  `STORY-1409`.
- **A retry that resends by itself.** The player presses again or skips; nothing here retries on
  their behalf, for the reason `set-name.ts`'s KDoc already gives.
- **Any new refusal kind**, and any mapping of a status to a sentence: `set-name.ts` owns the map and
  is not opened.

## Tests

`web-client/src/profile/NameAsk.test.tsx`, `describe("the name ask")` — the four from
`TASK-140711`, unchanged, plus:

| Test | Proves |
| --- | --- |
| `reads a refusal in the words the product already uses` | a `conflict` outcome puts the literal `That name is not available. Try another.` on screen in a `role="status"`, and `onNamed` is never called |
| `withdraws the form a refusal has closed, and leaves the skip standing` | an `unavailable` outcome leaves no submit control reachable by role, while the skip control is still there and pressing it calls `onSkip` once |
| `sends one write for two presses` | two submits while the first write is in flight call `setName` exactly once |
| `carries the accepted name to the profile the client holds` | mounted inside a real `ProfileProvider` whose read answered a nameless profile, a `named` outcome carrying `Ravenpost` makes `useProfileStrip()` report `Ravenpost` on the next render |

## Acceptance criteria

- [ ] All eight tests pass: `npx vitest run src/profile/NameAsk.test.tsx` reports `Tests  8 passed (8)`
- [ ] The refusal test asserts the **literal** sentence, while `NameAsk.tsx` calls
      `refusalSentence(refusal)` — the encoder references the constant, the test does not
- [ ] `NameAsk.tsx` contains no `case "conflict"` and no other branch on an outcome kind except
      `named`
- [ ] Deleting `if (submitInFlight.current) return;` fails `sends one write for two presses` and
      nothing else: `Tests  1 failed | 7 passed (8)`, file restored byte-for-byte
- [ ] Deleting `hidden={!canTryAgain}` fails `withdraws the form a refusal has closed, and leaves the
      skip standing` and nothing else: `Tests  1 failed | 7 passed (8)`, file restored byte-for-byte
- [ ] The skip control is reachable and functional in **every** state the component can be in
- [ ] `reportNameWrite(outcome)` is called for every settled outcome, and the profile test proves the
      held profile catches up
- [ ] Neither *permanent* nor `PERMANENCE_LINE` appears in `NameAsk.tsx`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
