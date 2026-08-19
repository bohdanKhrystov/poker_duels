---
schema: 2
id: TASK-041117
title: No name on the screen is built from a player id, and a takedown is invisible
type: task
status: done
parent: STORY-0411
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, profile, identity, guard, moderation]
depends_on: [TASK-041116]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +419 passed \(419\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no player id on the screen, named opponent or nameless'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders a removed name and a name never set as the same pixels'
  - cd web-client && npm run check
---

## Goal

The two rules `STORY-0411` states about names — nothing rendered is derived from a player id, and a
takedown is invisible to everybody but its subject — are assertions in the suite rather than
promises in a document.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/profile-no-derivation.test.tsx` | modify — two tests added |

Read, not edited: `web-client/src/profile/profile-fixture.ts`,
`docs/adr/ADR-0052-a-takedown-is-told-to-the-player-it-happened-to.md` §5.

## Scope

- Both tests go through `readProfileStrip` from a stubbed `fetch` and render `ProfileStrip`, as the
  file's two merged tests already do, and scan with the file's own `allContentOnScreen` — which
  reads text nodes, `aria-label`, `title` **and every attribute value**, because a name a client
  worked out for itself does not have to be printed to reach a player.
- The bodies carry real `opponentPlayerId` values, distinct from every other string in the fixture,
  so the scan has something to catch. `duelRowBody` supplies one by default and these tests pass
  their own.
- **No new production code.** If either test fails, the bug is in `TASK-041115` or `TASK-041116` and
  is fixed there, not worked around here.


**Carried from `TASK-041114`.** `ADR-0058` rests on `nameOrNone` being the **only** place the client
decides what to print where a name would be — that is what keeps `ADR-0052` §5's invisibility from
being undone by a second surface choosing differently. Measured, nothing enforces it: another
component could branch on a null `displayName` and print its own word, and no test or lint rule
would notice. This ticket already sweeps the client for name derivation, so **assert that
`nameOrNone` is the sole decision point** — no other file may branch on a null display name to
produce a label.

## Out of scope

- The request half of the rule — `TASK-041106`'s `sends a body whose only key is the name` already
  asserts it, and duplicating it here would leave two tests to update and one of them forgotten.
- The engine's and the server's own no-derivation guards. They exist and are not this file's.
- Anything about the player's *own* removed name. It is deliberately visible to them
  (`TASK-041109`); this file is about what everybody else sees.

## Tests

`web-client/src/profile/profile-no-derivation.test.tsx`, describe block
`"the profile strip's surface"`.

| Test | Proves |
| --- | --- |
| `puts no player id on the screen, named opponent or nameless` | One read whose two rows carry `opponentPlayerId: "player-91"` with `opponentDisplayName: "Ada"`, and `opponentPlayerId: "player-92"` with `opponentDisplayName: null`. The scan finds neither id, finds `Ada`, and finds the nameless treatment. Fails against a component that falls back to the id when the name is `null` — the exact `Player-3F2A` shortcut `ADR-0029` §6 forbids — and against a scan that only reads text nodes, since the sanity assertions prove it is looking |
| `renders a removed name and a name never set as the same pixels` | Two reads rendered separately: one profile whose `displayNameRemoved` is `true`, one whose is `false`, each with one nameless opponent line. The two `container.innerHTML` strings are **equal**. Fails against any badge, tooltip, class or word marking a takedown on a line, which `ADR-0052` §5 forbids — and it is a criterion rather than an omission because the wire cannot express the difference and no screen may invent it |

Two tests added to 417, so the suite reports **419**.

## Acceptance criteria

- [ ] `the profile strip's surface > puts no player id on the screen, named opponent or nameless`
      passes
- [ ] `the profile strip's surface > renders a removed name and a name never set as the same pixels`
      passes, comparing full markup
- [ ] The two merged tests in this file pass unchanged
- [ ] No file outside `web-client/src/profile/profile-no-derivation.test.tsx` differs
- [ ] `npm run --silent test` reports `Tests  419 passed (419)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
