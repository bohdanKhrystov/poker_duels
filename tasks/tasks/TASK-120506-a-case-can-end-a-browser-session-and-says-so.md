---
schema: 2
id: TASK-120506
title: A case can end a browser session, and says so
type: task
status: backlog
parent: STORY-1205
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [process, qa, harness]
depends_on: [TASK-120503, TASK-120505]
verify:
  - node scripts/qa/drive.mjs 2>&1 | grep -q '^  close'
  - node --check scripts/qa/drive.mjs
  - awk -F'|' '/^\| `CORE-18` \|/ { if ($3 ~ /close/) found=1 } END { exit !found }' docs/test-plan.md
  - awk -F'|' '/^\| `CORE-19` \|/ { if ($3 ~ /open/) found=1 } END { exit !found }' docs/test-plan.md
  - grep -qF '`CORE-18`' docs/test-plan.md && grep -qF '`CORE-19`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A case that needs a player to be **gone** has a verb that makes them gone, so `CORE-18` stops being
red for a reason that has nothing to do with the product.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(1)`**, and
**no production code may be changed by this ticket** — the two files are the driver and the
catalogue. A `## Files` table naming anything under `web-client/`, `poker-server/` or
`poker-engine/` is grounds to reject the diff on sight. It supersedes `TASK-120502`, which was
filed as a product defect and is `dropped`.

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. `CORE-18` failed. It should not
have.

`CORE-18`'s `do` cell is the whole of *"during A's absence"*. It does not say how absence is
produced, and **`drive.mjs` has no verb that produces it** — the ten verbs are `open`, `text`,
`click`, `wait`, `absent`, `type`, `link`, `device`, `forget-room`, `eval`. So the round improvised
with `open about:blank`, and on this headless Chrome **a navigation does not close the page's
WebSocket**:

| what was done to a `ws://localhost:8080/ws` socket, proxy **not** in the path | after 30s |
| --- | --- |
| explicit `.close()` | **gone within 3s** — 2 sockets → 1 |
| `location.href='about:blank'` | **still ESTABLISHED** — 2 sockets → 2 |

Same tab, same socket, one variable. The counter was validated in the same run (open took it 1 → 2,
close took it 2 → 1), so it detects a real close. The player never left, so the server never said
they had, so `CORE-18` observed nothing — correctly.

**The product is fine.** Closing the tab, which is what a player does, works through the dev proxy
in under four seconds:

```
close A's app tab over CDP
  +4s   B: "Your rival is away. The duel is paused." 56   (seat plate reads Away)
  +16s  ... 44                                            (the grace window ticking)
reopen the room on A
  +4s   B: "Your rival is back."
```

Vite's upstream connections to Ktor fell 3 → 1 at the close, so the teardown crossed the proxy and
the push came back through it.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/drive.mjs` | modify |
| `docs/test-plan.md` | modify |

## Scope

- Add a **`close`** verb to `drive.mjs`: end this profile's app session the way a player closing a
  tab does. `PUT /json/new?url=about:blank` first so the browser survives with a tab left, then
  `GET /json/close/<targetId>` on the `localhost:5173` page. Exit 0 when the page is gone, 1 when
  there was none to close.
- List it in the `usage` block, in the same one-line shape as the other verbs. The first `verify:`
  command reads that block.
- Rewrite `CORE-18`'s `do` cell to `A close`, and `CORE-19`'s to `A open <link>` — so both cases
  say, in driver verbs, how absence and return are produced.
- Add one sentence to the CORE suite's Reconnect preamble: **a navigation is not a disconnect on
  this browser; only `close` ends a session.** One sentence, so the next case needing an absent
  player does not rediscover this at the cost of a round.

## Out of scope

- **`web-client/vite.config.ts`.** The dev proxy was suspected and **tested**: it forwards both the
  teardown and the `OpponentPresence` push. Changing it would be repairing a phantom.
- **Serving `dist/` instead of `npm run dev` in a round.** A good idea for a different reason —
  `ADR-0088` gap 3, that the built bundle is proven by nothing — and a poor fix for this, which is
  not a dev-server defect. If it is wanted it is its own ticket against `EPIC-12`, argued on gap 3.
- **`kill`, `pkill`, `killall`.** Denied in `settings.json`, and deny beats allow. The verb closes
  a tab over CDP, which is the same mechanism `stack.sh chrome-down` already uses.
- **`CORE-17`.** A reload is a reload; `A open` already expresses it and it passed.
- **Any change to the presence copy, the grace window, or the store.** All correct, all observed
  working above.

## Tests

None — `scripts/qa/` has no test runner. The gates are the driver's own usage output, a syntax
check, and two structural reads of the catalogue.

| Gate | Proves | Today |
| --- | --- | --- |
| `usage` lists `close` | the verb exists and is documented | **exits 1** |
| `node --check` | the driver still parses | exits 0 — it must keep doing so |
| `CORE-18` `$3 ~ /close/` | the case says how absence is produced | **exits 1** |
| `CORE-19` `$3 ~ /open/` | the case says how return is produced | **exits 1** |
| both ids still present | neither case was deleted to satisfy the above | exits 0 — it must keep doing so |

All five were run at commit `fe4bbf2a`.

## Acceptance criteria

- [ ] `node scripts/qa/drive.mjs <port> close` ends that profile's app session and leaves the
      browser running, so the next `open` on that port still works.
- [ ] `CORE-18` and `CORE-19` name driver verbs in their `do` cells.
- [ ] Their `expect` and `fails if` cells are byte-identical to what they are now.
- [ ] The Reconnect preamble gained the sentence about navigation not being a disconnect.
- [ ] The diff touches exactly two files.
- [ ] Every command in `verify:` exits 0.

**Manual reproduction, for the reviewer.** Seat two profiles in a duel. `A close`, then read B
within 10s: `Your rival is away. The duel is paused.` and a countdown. `A open <link>`, then read
B: `Your rival is back.`

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
