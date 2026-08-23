---
schema: 2
id: TASK-030911
title: The way back steps aside for the rematch
type: task
status: backlog
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, result, design]
depends_on: [TASK-030910]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +561 passed \(561\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers a way back to the lobby'
  - cd web-client && npm run check
---

## Goal

The result panel has exactly one bright action again, and it is the rematch: the way back takes the
design's ghost treatment.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify — the `<a href="/">`'s classes only |
| `web-client/src/result/DuelResult.test.tsx` | modify — one existing test gains two assertions |
| `design/screens/duel-end.html` | read — `.btn.fill` for Rematch, `.btn.ghost` for Back to lobby |

## Scope

- The `Back to the lobby` anchor's class list becomes
  `rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text`,
  replacing `bg-accent-fill` and `text-on-accent`.
- Its `href`, its text and its position are untouched.
- One comment: `design/screens/duel-end.html` puts the two actions in one stack and gives the fill
  to Rematch — *the only bright action is the way back in* was true while there was no rematch to
  offer, and `TASK-030910` has just made it false.
- Nothing else in either file changes.

## Out of scope

- The rematch button's own classes — `TASK-030907` set them.
- Turning the anchor into a button, or making it forget anything. It is still a plain `<a href="/">`
  and the reload is still how this client reaches an empty store.
- `design/` itself. The design is the source here, not the thing being changed.

## Tests

`web-client/src/result/DuelResult.test.tsx`, describe block `"the result screen"`. **None added** —
the existing `offers a way back to the lobby` grows two assertions, in the form the file already
uses for the verdict colours (`heading.className.split(" ")`).

| Test | Proves |
| --- | --- |
| `offers a way back to the lobby` | the link's `href` is still `/` (unchanged), **and** its class list contains `border-hairline` and does **not** contain `bg-accent-fill` |

## Proof

| Command | Proves |
| --- | --- |
| `Tests 561 passed (561)` | none added, none lost |
| the `--reporter=verbose` grep | the test still exists under its own name |
| `npm run check` | Prettier's Tailwind plugin has ordered the new class list, so `format:check` passing means the list is written the way the repo writes them |

**Name the edit that makes the assertion red:** put `bg-accent-fill` back on the anchor → `offers a
way back to the lobby` fails on the second of the two new checks. Revert, and quote it in the PR.

## Acceptance criteria

- [ ] `the result screen > offers a way back to the lobby` passes, with the `href` assertion it already had plus the two class assertions
- [ ] `DuelResult.tsx` contains no `bg-accent-fill` and no `text-on-accent`
- [ ] `RematchControl.tsx` is unchanged from `develop`
- [ ] No other `it` block in `DuelResult.test.tsx` differs
- [ ] `npm run --silent test` reports `Tests  561 passed (561)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
