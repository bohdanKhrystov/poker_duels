---
schema: 2
id: TASK-120913
title: The name the server accepted reaches the profile the provider holds
type: task
status: done
parent: STORY-1209
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: [TASK-120910]
verify:
  - grep -qF 'useReportNameWrite' web-client/src/profile/NameSurface.tsx
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/NameSurface.test.tsx 2>&1 | grep -qF 'Tests  10 passed (10)'
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The hook `TASK-120910` added is called by the code that completes a name write, so the profile the
provider holds carries the accepted name and every consumer of it — the profile strip among them —
renders that name on the next render rather than on the next boot.

## Why this is a separate ticket

`TASK-120910`'s `## Files` table named two files, `profile-provider.tsx` and its test, and its
`## Scope` claimed a sentence those two files cannot deliver on their own: *"so every consumer of it
renders the accepted name on the next render rather than on the next boot."* The provider gained
`useReportNameWrite()`; **nothing in production calls it.** The review of `#1228` confirmed the
capability is correct and its tests load-bearing, and that the call site lives in a third file the
table never listed.

So the chain is split the way `TASK-121101`'s was into `TASK-121109`, and the way `TASK-121005`'s
was into `TASK-121010`: the half that landed is correct and merged, and the half that was missing
gets its own ticket rather than a widened one (`CLAUDE.md` rule 4). `TASK-120910`'s live-stack
acceptance criterion moved here with the work, because it is this ticket that makes it passable.

## The defect this closes

Round 1 (UAT) filed it: a player sets a display name, the name section shows the new value — it
holds it locally in `wonName` — and the profile strip beside it keeps showing the old one until the
page is reloaded. `NameSurface.tsx:59` receives the `SetNameOutcome` and tells no one but itself.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/NameSurface.tsx` | modify |
| `web-client/src/profile/NameSurface.test.tsx` | modify |

## Scope

- **Call `useReportNameWrite()` with the outcome the write returned**, at the point
  `NameSurface.tsx` already handles it. The hook takes the whole `SetNameOutcome` and is a no-op on
  every refusal kind, so no branching on `kind` belongs at the call site.
- **The provider is the one that adopts the name.** This ticket adds a call, not a second copy of
  the adoption logic.

## Out of scope

- **`profile-provider.tsx`.** It is `TASK-120910`'s, it is merged, and its behaviour is settled. If
  the hook's signature turns out to be wrong for the call site, that is a finding to report, not a
  file to edit.
- **`ProfileStrip.tsx`.** It renders what it is handed; `TASK-120910` said so and it still holds.
- **`wonName`.** The local state that already makes the name section correct stays exactly as it is.
  Removing it is a separate question about which component owns the displayed name, and nothing in
  this ticket answers it.
- **The account screen's staleness.** That is `TASK-120601`'s, a different mechanism.

## Tests

`NameSurface.test.tsx` — the file holds **9** tests today and must hold **10**.

| Test | Proves |
| --- | --- |
| `a successful write is reported to the profile provider` | after the write resolves `named`, the function returned by `useReportNameWrite()` was called **with the server's outcome** — asserted against a fixture whose returned name differs from the submitted string, so a call that forwarded the typed input instead of the outcome fails |

The existing 9 tests never touch the reporting path, so the count moving 9 → 10 is what proves a
test was added rather than an assertion loosened.

## Acceptance criteria

- [ ] `NameSurface.tsx` calls `useReportNameWrite()`, and the call passes the outcome it received.
- [ ] `NameSurface.test.tsx` runs **10** tests, all passing.
- [ ] Deleting the new call from `NameSurface.tsx` reddens the new test, and reddens nothing else.
- [ ] **By hand, on a live stack, on a device with no name**: set a name, and read the same name in
      both lobby sections **without** reloading. (Carried here from `TASK-120910`, whose two files
      could not satisfy it.)
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
