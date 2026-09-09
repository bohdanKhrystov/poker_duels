---
schema: 2
id: TASK-141512
title: The gate refuses a key listener too
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, rematch, notice, test]
depends_on: [TASK-141504]
verify:
  - cd web-client && npm ci
  - sh -c 'cd web-client && test $(grep -c "onKeyDown\|onKeyUp\|onKeyPress\|addEventListener" src/result/RematchNotice.test.tsx) -ge 1'
  - sh -c 'cd web-client && NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qF "RematchNotice.test.tsx  (6 tests)"'
  - git diff --exit-code -- web-client/src/result/RematchNotice.tsx
  - cd web-client && npm run check
  - cd web-client && npm run build
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The gate that holds `RematchNotice` to `ADR-0138` §6 refuses a **key listener**, which it does not
today. The component is already correct; nothing would catch it becoming wrong.

## Why this exists

`ADR-0138` §6 names three things the surface must never do: *"nothing about it ever takes focus,
sets a tab stop of its own, or listens for a key — a press only reaches it once a player has
deliberately moved to one of its two controls."*

`TASK-141504`'s gate enumerates `role="alert"`, `role="dialog"`, `alertdialog`, `aria-live`,
`aria-hidden`, `autoFocus`, `tabIndex`, `.focus()` and `inert`. It covers the first two clauses and
**not the third**. Measured by probe while landing that ticket, in its own worktree:

| probe | gate |
| --- | --- |
| `tabIndex={0}` added to the root | exit **1** — caught |
| `onKeyDown={() => {}}` added to the root | exit **0** — **not caught** |

Both probes were reverted and the file confirmed byte-identical.

This matters more with each ticket after it. `TASK-141505` gives the surface an accept press and
`TASK-141507` a dismissal; a key listener on a floating panel is exactly the shortcut a later change
reaches for, and §6 exists because a panel that answers the keyboard can take a press the player
aimed at the screen underneath.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.test.tsx` | modify |

Read, do not edit: `web-client/src/result/RematchNotice.tsx`,
`docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md` §6.

## Scope

- One test asserting the rendered root and every element inside it carry **no** key handler and that
  the component registers no document- or window-level key listener. Assert over the rendered DOM
  and a spied `addEventListener`, not over the source text — a source grep is what already exists
  and it is the thing being repaired.
- The count gate moves 5 → 6.

## Out of scope

- **`RematchNotice.tsx`.** It is already correct; this ticket observes it and changes nothing. The
  `git diff --exit-code` above says so.
- **Extending `TASK-141504`'s awk gate.** A source grep cannot see a listener registered through a
  helper, and adding a fourth string to an enumeration that already missed this one repeats the
  mistake rather than repairing it. The test is the repair.
- **The other rematch surfaces.** `RematchControl` predates this story and `TASK-141505`–`141510`
  have their own gates; whether they owe the same test is theirs to say, not this ticket's.

## Tests

| Test | What it pins |
| --- | --- |
| `listens for no key, anywhere` | `ADR-0138` §6's third clause |

## What would still pass if the coder got it wrong

- Asserting only that the root has no `onkeydown` property misses a listener added with
  `addEventListener` on `document` — which is the form a real shortcut takes.
- Asserting the source contains no `onKeyDown` is the gate that already exists and already missed
  this; if the new test greps source, nothing has been repaired.
- Rendering only the null branch (no offer standing) tests an empty tree: the assertion must run
  against the branch that actually renders the panel.

## Acceptance

- [ ] `RematchNotice.test.tsx` reports `(6 tests)` and all pass
- [ ] Adding `onKeyDown={() => {}}` to the root reddens the new test, and the file is restored
- [ ] `RematchNotice.tsx` is byte-identical to `develop`
- [ ] `npm run check` and `npm run build` exit 0 in `web-client`
