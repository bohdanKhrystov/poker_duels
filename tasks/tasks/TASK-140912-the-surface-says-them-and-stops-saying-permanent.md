---
schema: 2
id: TASK-140912
title: The surface says them, and stops saying a name is permanent
type: task
status: backlog
parent: STORY-1409
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, account]
depends_on: [TASK-140911]
verify:
  - cd web-client && npm ci
  - sh -c '! grep -rqF "permanent" web-client/src/profile'
  - sh -c '! grep -qF "PERMANENCE_LINE" web-client/src/profile/NameSurface.tsx'
  - sh -c '! grep -qF "PERMANENCE_LINE" web-client/src/profile/NameSurface.test.tsx'
  - grep -qF "CHANGEABLE_LINE" web-client/src/profile/NameSurface.tsx
  - grep -qF "SPENT_LINE" web-client/src/profile/NameSurface.tsx
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/profile/NameSurface.test.tsx 2>&1 | grep -qF "NameSurface.test.tsx  (10 tests)"'
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The name surface prints `ADR-0130` §5's two obligations above the field and prints
`PERMANENCE_LINE` nowhere, so a player is told the name can be changed and that the one they give
up is gone for good **before** they send.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify |
| `web-client/src/profile/NameSurface.test.tsx` | modify |

`name-text.ts` is **not** in this list: `PERMANENCE_LINE` stays exported for one more ticket, which
is what keeps this diff to two files instead of four.

Read, and do not edit:
[`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md) §5;
`web-client/src/profile/name-text.ts` — `CHANGEABLE_LINE` and `SPENT_LINE`.

## Scope

- **`NameSurface.tsx`** imports `CHANGEABLE_LINE` and `SPENT_LINE` instead of `PERMANENCE_LINE`, and
  renders both as `<p className="text-small">` in the same slot, in that order, still **above** the
  form.
- **The stale comment at `NameSurface.tsx:55` goes.** It reads *"A name is permanent: a second
  submit while one is already in flight must send nothing, not race the first and let the server
  pick a winner (200) and a loser (403)"*. Both halves are now false — a name is not permanent and
  there is no `403`. The **guard stays**: `ADR-0130` §1 makes a second write a *rename*, so racing
  two submits is worse than it was, not better. Rewrite the comment to say that.
- **`NameSurface.test.tsx`'s `says the choice is permanent before anything is sent`** becomes
  `says the name can be changed and the one given up is gone, before anything is sent`: both
  sentences are on screen, the field **follows** `CHANGEABLE_LINE` in document order — the merged
  test's `compareDocumentPosition` assertion, kept — and `setName` was not called.
- **The comment at `NameSurface.test.tsx:376`** explaining why the *taken* sweep is scoped to the
  status elements is rewritten: it currently justifies itself by `PERMANENCE_LINE` legitimately
  saying a name *"can be taken away"*. `SPENT_LINE` now carries that sense. The scoping stays; only
  its reason is corrected.

## Out of scope

- **Deleting `PERMANENCE_LINE` from `name-text.ts`.** `TASK-140913`, one ticket later, with no
  importer left.
- **Offering the form to a player who already holds a name.** `TASK-140918`. Until then the named
  branch still returns early and these two sentences render only for a player with no name.
- **Moving the surface anywhere.** `TASK-140914` and after.
- **Changing the removal notice, the refusal sentences or `mayTryAgain`.**

## Tests

`NameSurface.test.tsx` — 10 tests on `develop` at `1c3c7fd9` and after `TASK-140910`, 10 after this
ticket: the permanence test is rewritten, not removed, and no test is added.

| Test | Proves |
| --- | --- |
| `says the name can be changed and the one given up is gone, before anything is sent` | *(rewritten)* both `CHANGEABLE_LINE` and `SPENT_LINE` are found by text, the textbox follows `CHANGEABLE_LINE` in document order, and `setName` has not been called |

## What would still pass if the coder got it wrong

- **If only one of the two sentences were rendered**, a test that asserted `getByText` on the other
  fails — so both `getByText` calls are required, and the test may not settle for querying one and
  asserting the section is non-empty.
- **If both were rendered below the field**, every `getByText` passes. The merged
  `compareDocumentPosition` assertion is the only gate on order, which is `ADR-0130` §5's whole
  point — *before the send* — and it must be kept rather than dropped as incidental.
- **If the stale comment were left**, nothing fails: a comment has no gate but the sweeping
  `! grep -rqF "permanent" web-client/src/profile`, which is in `verify` for exactly that reason. It
  is safe to run here and not before, because this is the ticket that clears the last two lowercase
  uses in that directory (`NameSurface.tsx:55` and `NameSurface.test.tsx:70`) — `TASK-140910`
  cleared the other fourteen.
- **If the two-submit guard were removed with its comment**, `sends what the player typed, once,
  however many times the button is pressed` fails. That merged test must stay green and unweakened.

## Acceptance criteria

- [ ] `NameSurface.test.tsx`'s rewritten test passes and keeps the `compareDocumentPosition`
      assertion
- [ ] `NameSurface.test.tsx` reports exactly 10 tests, all passing
- [ ] `sends what the player typed, once, however many times the button is pressed` passes with no
      assertion weakened
- [ ] `PERMANENCE_LINE` appears in neither `NameSurface.tsx` nor `NameSurface.test.tsx`
- [ ] `CHANGEABLE_LINE` and `SPENT_LINE` both appear in `NameSurface.tsx`
- [ ] The lowercase word `permanent` appears in no file under `web-client/src/profile`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
