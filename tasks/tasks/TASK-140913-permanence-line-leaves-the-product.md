---
schema: 2
id: TASK-140913
title: PERMANENCE_LINE leaves the product
type: task
status: backlog
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, account]
depends_on: [TASK-140912]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -rqF "PERMANENCE_LINE" web-client/src design'
  - sh -c '! grep -rqF "A name is chosen once" web-client/src design'
  - sh -c '! grep -rqF "PERMANENCE_LINE" docs/protocol.md docs/test-plan.md docs/architecture.md'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/name-text.test.ts 2>&1 | grep -qF "name-text.test.ts  (7 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The sentence *"A name is chosen once. You cannot change it later, and it can be taken away."* is
gone from this product, together with the constant that carried it — `ADR-0130` §5, in one line.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/name-text.ts` | modify |
| `web-client/src/profile/name-text.test.ts` | modify |

Two, and no more, because `TASK-140912` already removed both importers. **Measured on `develop` at
`1c3c7fd9`**: `PERMANENCE_LINE` is named in exactly four files under `web-client/src` —
`name-text.ts`, `name-text.test.ts`, `NameSurface.tsx` and `NameSurface.test.tsx` — in **no** file
under `design/` or `poker-server/`, and under `docs/` only in four ADRs (`ADR-0119`, `ADR-0130`,
`ADR-0134` and the index), which record the decision and must keep the word. The `verify` greps are
scoped accordingly: `web-client/src` and `design` swept whole, `docs/` narrowed to the three
documents that describe the shipped product. Deleting it breaks no unrelated test: `npm run check`
was driven to `exit 0` over the deletion without touching `Lobby.test.tsx`, so the order dependence
this epic found there (`STORY-1408` §Design notes) does not fire here.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §5.

## Scope

- **Delete `export const PERMANENCE_LINE`** from `name-text.ts`.
- **Delete `says a name can be taken away, before anything is sent`** from `name-text.test.ts`, and
  its `PERMANENCE_LINE` import. It is the golden test for a string that no longer exists; there is
  nothing to re-point it at, because `TASK-140911`'s test already pins the two sentences that
  replaced it. Eight tests become seven.

## Out of scope

- **`NAME_REMOVED_HEADING` and `NAME_REMOVED_BODY`.** `ADR-0052` §1's four sentences are unchanged
  and `ADR-0130` §6 says the notice travels with the surface untouched.
- **`nameOrNone`, `refusalSentence` and `mayTryAgain`.**
- **Anything outside `web-client/src/profile`.**

## Tests

`name-text.test.ts` — 8 tests after `TASK-140911`, 7 after this ticket.

No test is added. The deletion's gate is the pair of `verify` greps over `web-client/src`, `design`
and `docs`, plus the per-file count.

## What would still pass if the coder got it wrong

- **If the constant were deleted and the sentence left inline somewhere**, every count and every
  test passes. The second grep — for the sentence's own first five words, `A name is chosen once` —
  is the gate on that, and it is a separate command from the identifier grep for exactly that
  reason: a deletion of a *name* is not a deletion of a *string*.
- **If the test were left and re-pointed at `CHANGEABLE_LINE`**, the count would be 8 and the file
  would carry two tests asserting the same two constants. The count gate is an equality at 7 and
  catches it.
- **If both the constant and `NAME_REMOVED_BODY` were deleted**, the count would be 6 and
  `says the four sentences ADR-0052 shipped, and no fifth` fails. Both halves matter.

## Acceptance criteria

- [ ] `PERMANENCE_LINE` appears in no file under `web-client/src` or `design`, and in none of
      `docs/protocol.md`, `docs/test-plan.md` or `docs/architecture.md`
- [ ] `A name is chosen once` appears in no file under `web-client/src` or `design`
- [ ] The four ADRs that name it — `ADR-0119`, `ADR-0130`, `ADR-0134` and `docs/adr/README.md` — are
      **not** edited: an ADR records what was decided and does not stop being true when the code
      changes
- [ ] `name-text.test.ts` reports exactly 7 tests, all passing — 8 after `TASK-140911` minus the one
      golden test named above
- [ ] `says the four sentences ADR-0052 shipped, and no fifth` passes unchanged
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
