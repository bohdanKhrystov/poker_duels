---
schema: 2
id: TASK-120505
title: The driver does not click what a player cannot see
type: task
status: ready
parent: STORY-1205
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [process, qa, harness]
depends_on: []
verify:
  - grep -qF 'offsetParent' scripts/qa/drive.mjs
  - grep -qF 'e.disabled' scripts/qa/drive.mjs
  - node --check scripts/qa/drive.mjs
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`drive.mjs`'s `click` verb reaches only controls a player could reach, so a case cannot pass by
pressing a button the product has deliberately hidden.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(1)`**, and
**no production code may be changed by this ticket** — the fix is one file under `scripts/qa/`.
A `## Files` table naming anything under `web-client/` is grounds to reject the diff on sight.

The distinction matters here more than usual, because `qa` reported this as a product defect and
**it is not one**. See below.

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. `qa` reported, outside the 36
cases, that the leaderboard's *Show more* *"stays enabled with no effect once every season entry is
already shown"* — clicking it fired no request and changed nothing.

`qa-manager` read the live ladder screen:

```
[{"text":"Show more","innerText":"Show more","hidden":true,"disabled":false,"offsetParent":"null"},
 {"text":"Back","innerText":"Back","hidden":false,"disabled":false,"offsetParent":"shown"}]
```

**The product is right.** `LadderScreen.tsx` renders the control `hidden={!canAskMore}`, and its
own comment says why it is hidden rather than unmounted — so a second press lands on a node the
guard can refuse instead of on one torn out of the tree. A player can neither see it nor click it.

**The driver is wrong.** Its selector is:

```js
const els = [...document.querySelectorAll('button, a, [role=button]')]
  .filter(e => !e.disabled);
```

`disabled` and nothing else. And Chrome's `innerText` falls back to `textContent` for a node that
is not rendered, so the hidden button still matches by label. The driver finds a control no player
can reach and calls `.click()` on it directly — dispatching a handler no hand could dispatch.

**It does not reproduce by hand as a player action**, which is what makes it a harness defect
under §4 rather than a product one.

**It is worth more than the observation that produced it.** `05-04` walks the ladder's pages with
exactly this control; a driver that can press a hidden *Show more* can report a walk that never
happened. The same blindness applies to any control the client hides — and this repository hides
controls on purpose, as a settled pattern.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/drive.mjs` | modify |

## Scope

- Extend `clickExpr`'s filter so a control is eligible only when it is **rendered**: keep
  `!e.disabled`, and add a visibility test — `e.offsetParent !== null` is the cheap one that
  catches `hidden`, `display:none` and a `display:none` ancestor together.
- Apply the same filter to the `saw:` list the verb prints on failure, so the diagnostic names the
  controls a player has rather than the controls the DOM has. A driver that fails and then lists a
  button the player cannot see sends the next agent hunting the wrong thing.
- Leave every other verb alone. `text`, `wait` and `absent` read `innerText` from `#root`, which
  already reflects rendering.

## Out of scope

- **Any file under `web-client/`.** Named again because it is the rule this ticket exists under.
  In particular do not touch `LadderScreen.tsx`: `hidden` is deliberate and the comment above it
  says so.
- **`position: fixed` and other cases where `offsetParent` is `null` on a visible element.** No
  control in this client is positioned that way; if one ever is, that is a new ticket, and
  `checkVisibility()` is the answer then.
- **`aria-disabled`, opacity, and controls scrolled out of view.** One visibility rule, not a
  rendering engine.
- **Adding a case for the ladder's paging.** `05-04` already walks it; this ticket makes that walk
  honest rather than adding a second one.

## Tests

None — `scripts/qa/` has no test runner, and adding one is a larger change than the defect. The
gates are structural, plus a syntax check so a broken predicate cannot merge.

| Gate | Proves | Today |
| --- | --- | --- |
| `offsetParent` in `drive.mjs` | the filter gained a visibility test | **exits 1** |
| `e.disabled` in `drive.mjs` | the `disabled` filter survived | exits 0 — it must keep doing so |
| `node --check scripts/qa/drive.mjs` | the file still parses | exits 0 — it must keep doing so |

The second gate is what stops the cheap fix: replacing the `disabled` filter with a visibility test
would satisfy the first and lose what the driver already got right. All three were run at commit
`fe4bbf2a`.

**These gates are structural and a comment would satisfy the first one.** That is stated rather
than dressed up. The behavioural proof is the manual reproduction below, and the reviewer is asked
to read the filter rather than trust the grep.

## Acceptance criteria

- [ ] `clickExpr`'s filter keeps `!e.disabled` and adds a rendered-ness test.
- [ ] The `saw:` diagnostic lists only controls that pass the same filter.
- [ ] No verb other than `click` changed.
- [ ] The diff touches exactly one file, and it is `scripts/qa/drive.mjs`.
- [ ] Every command in `verify:` exits 0.

**Manual reproduction, for the reviewer.** With the stack up and a ladder that fits on one page:
`node scripts/qa/drive.mjs 9232 click "Show more"`. Before this ticket it prints
`clicked: Show more` and nothing happens. After it, it exits 1 and reports no such control — which
is the truth, because there is none on screen.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
