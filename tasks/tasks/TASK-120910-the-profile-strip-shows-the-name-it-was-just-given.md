---
schema: 2
id: TASK-120910
title: The profile strip shows the display name it was just given
type: task
status: done
parent: STORY-1209
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/profile/profile-provider.test.tsx 2>&1 | grep -qF "a name that was just set reaches the profile read"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/profile/profile-provider.test.tsx 2>&1 | grep -qF "a name the server refused leaves the held profile alone"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/profile/profile-provider.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

After a display name is accepted, every section of the lobby states the same name on the same
render, so the screen never contradicts itself about who the player is.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`.

**One render, two answers.** Immediately after setting a display name, the lobby's profile strip
still states the previous value while the section beside it already states the new one:

    <section aria-label="your profile">…<p class="text-small">No name</p>…</section>
    <section aria-label="your display name">…<p class="text-small">Losing Larry</p></section>

A full reload resolves it — afterwards both read `Losing Larry`.

**Where it lives.** `ProfileStrip.tsx` is prop-driven and correct: its own KDoc says it renders *"as
a prop-driven presentation over `ProfileStripState`, with no hooks, fetching, or state — the read
that fills it runs outside the tree."* The stale value is the read that fills it: the profile the
provider holds is not re-read when the name write succeeds, so the strip renders the profile the
page booted with.

## Why `medium`

`uat` reported `medium`. **Severity unchanged.** No promise in `EPIC-12`'s `high` row is touched,
nothing is lost or corrupted — the server holds the new name and every later load states it — and
the workaround is a reload the player will perform anyway. A screen stating two answers at once is a
real defect, and this is what `medium` is for.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-provider.tsx` | modify |
| `web-client/src/profile/profile-provider.test.tsx` | modify |

## Scope

- **A successful name write refreshes the profile the provider holds**, so every consumer of it
  renders the accepted name on the next render rather than on the next boot.
- **The accepted name is the server's**, not the string that was typed: `ADR-0029`'s canonical form
  means the two can differ, and a client that displayed its own input would state a name the server
  does not hold.

## Out of scope

- **`ProfileStrip.tsx`.** It renders what it is handed and is not the defect. Do not give it hooks or
  a fetch; its KDoc is a decision, not a description.
- **`set-name-provider.tsx`'s own state.** The section that already shows the new value is correct.
- **Any other staleness.** The account screen's is `TASK-120601`'s, a different mechanism and a
  different ticket.

## Tests

`profile-provider.test.tsx`

| Test | Proves |
| --- | --- |
| `a name that was just set reaches the profile read` | after a successful name write, the profile the provider exposes carries the **server's** returned name — asserted against a fixture whose returned name differs from the submitted string, so a client echoing its own input fails |
| `a name the server refused leaves the held profile alone` | a refused write triggers no refresh and the held profile is byte-identical afterwards |

## Acceptance criteria

- [ ] `profile-provider.test.tsx > a name that was just set reaches the profile read` passes, with
      the fixture's returned name different from the submitted one
- [ ] `profile-provider.test.tsx > a name the server refused leaves the held profile alone` passes
- [ ] Reverting `profile-provider.tsx` alone reddens the first
- [ ] *(moved to [`TASK-120913`](TASK-120913-the-name-the-server-accepted-reaches-the-profile-the-provider-holds.md))*
      **By hand, on a live stack, on a device with no name**: set a name, and read the same name in
      both lobby sections **without** reloading. This ticket's two files give the provider the
      capability; the call site that makes it observable is `NameSurface.tsx`, which this ticket's
      `## Files` table does not name, so the hand check moved with the work rather than being
      claimed here (`CLAUDE.md` rule 4)
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
