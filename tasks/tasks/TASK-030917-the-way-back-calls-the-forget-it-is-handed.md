---
schema: 2
id: TASK-030917
title: The way back calls the forget it is handed, and still navigates
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, result, ui]
depends_on: [TASK-030916]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +573 passed \(573\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'calls onLeave when the way back is taken'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the way back with no onLeave to call'
  - cd web-client && ! grep -qF 'preventDefault' src/result/DuelResult.tsx
  - cd web-client && ! grep -qF 'window.location' src/result/DuelResult.tsx
  - cd web-client && npm run check
---

## Goal

`DuelResult`'s way back runs a caller's handler before the browser leaves the page, and is still a
plain link.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/DuelResult.tsx` | modify — one optional prop, one `onClick` |
| `web-client/src/result/DuelResult.test.tsx` | modify — two added |
| `docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md` | read — §5, and the Consequences entry on the modifier click |

## Scope

- `DuelResult`'s props gain `onLeave?: () => void;`.
- The existing `<a href="/">` gains `onClick={props.onLeave}` and nothing else: its `href`, its text,
  its classes and its position are untouched.
- **No `preventDefault`, no `window.location`, no router.** `Storage.removeItem` is synchronous, so
  a handler that forgets has finished before the browser leaves the page, and the navigation stays
  the browser's — which is what keeps the lobby an empty store (`TASK-030807`'s argument, `ADR-0072`
  §5). Two `verify` commands assert both words are absent from the file.
- `DuelResult` stays a function of its props: it reads no hook and no storage, exactly as
  `TASK-031009`'s criterion asked and `ADR-0072` §5 keeps.

## Out of scope

- Deciding **what** `onLeave` does. This component neither knows nor cares; `TASK-030918` hands it
  boot's forget.
- **Narrowing the modifier click.** A ctrl-, cmd- or shift-click fires this handler and opens the
  lobby in a *new* tab, leaving this one on the result screen having already run it. `ADR-0072`
  records that cost in Consequences — *small, real, and not fixable while keeping both the link and
  the forget* — and this ticket leaves it exactly there. A guard on `event.metaKey`/`ctrlKey` is a
  behaviour the ADR did not decide: it is a new `DEC`, not a coder's call, and not a review finding.
- The anchor's classes — `TASK-030911` set them.

## Tests

`web-client/src/result/DuelResult.test.tsx`, describe block `"the result screen"`. Two added; the
merged `offers a way back to the lobby` is **not** edited.

| Test | Proves |
| --- | --- |
| `calls onLeave when the way back is taken` | rendered with a `vi.fn()` as `onLeave`, `fireEvent.click(link)` leaves the spy with `toHaveBeenCalledOnce()`, **returns `true`** — nothing called `preventDefault` — and the link still reads `href="/"` |
| `takes the way back with no onLeave to call` | rendered with **no** `onLeave`, `expect(() => fireEvent.click(back)).not.toThrow()` and the link still reads `href="/"` |

`fireEvent.click` returns `false` when a handler prevented the default, so the first test's boolean
is the whole of *"it is still a link"*: a handler that called `preventDefault` and navigated itself
would pass the spy assertion and fail that one.

The second test is not decoration: the prop is optional (`ADR-0072` §4), and `driveScriptedDuel` and
seven other render sites go on rendering this component without one.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 573 passed (573)` | two added to 571 |
| the two `--reporter=verbose` greps | both names exist |
| the two `! grep` commands | no `preventDefault`, no `window.location` in the file |

**Name the edit that makes each assertion red:**

1. Wrap the handler as `onClick={(event) => { event.preventDefault(); props.onLeave?.(); }}` →
   `calls onLeave when the way back is taken` fails on the returned `false`. Revert.
2. Make `onLeave` required and call it as `props.onLeave()` → `takes the way back with no onLeave to
   call` fails with a `TypeError`, and `npm run check` fails at every render site. Revert.

Quote the first in the PR.

## Acceptance criteria

- [ ] `the result screen > calls onLeave when the way back is taken` passes
- [ ] `the result screen > takes the way back with no onLeave to call` passes
- [ ] `DuelResult.tsx` contains no `preventDefault` and no `window.location`
- [ ] `DuelResult.tsx` imports nothing from `../store/duel-provider` and reads no storage — it is still a function of its props
- [ ] The way back is still `<a href="/">Back to the lobby</a>`, in the same position in the panel
- [ ] Every pre-existing `it` block in `DuelResult.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  573 passed (573)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
