---
schema: 2
id: TASK-131002
title: The relay learns to cut, so a socket drops without the page dying
type: task
status: ready
parent: STORY-1310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [qa, harness, refresh, manual-verify]
depends_on: [TASK-131001]
verify:
  - node scripts/qa/delay.mjs --selftest
  - node scripts/qa/delay.mjs --selftest-cut
  - node scripts/qa/delay.mjs >/dev/null 2>&1; test $? -eq 2
  - awk '/^import / && $0 !~ /"node:/ { bad = 1 } /require\(/ { bad = 1 } END { exit bad ? 1 : 0 }' scripts/qa/delay.mjs
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The relay can sever every connection running through it on command, so `P3` — *"a genuinely dropped
socket — a reconnect through `reconnecting.ts`, **not** a reload"* — becomes drivable at all.

## Why `drive.mjs close` is not this path

`ADR-0112` §6 asks for a socket that drops **while the page lives**, so that
`web-client/src/protocol/reconnecting.ts` opens the next one and the client rejoins on its own
schedule (`retryDelayMillis`: 500 ms doubling to 10 s, equal jitter). Nothing in the harness does
that today:

- **`drive.mjs close` closes the tab.** Its own comment says so — it opens `about:blank`, finds the
  app tab by URL and closes it. The document is destroyed, so `reconnecting.ts` is destroyed with
  it. That is a player closing a tab, which `CORE-18` already covers, and it is a different event.
- **`open` is a navigation**, which the catalogue already records as *"a disconnect on this browser"*
  that resumes immediately. Also a different event.
- **`eval` cannot reach the socket.** The client exposes no handle on `window`, and reaching in to
  close one would be writing application state — `ADR-0089` §3's line, and a case that reaches its
  precondition that way proves a fiction.

Cutting the relay's TCP connections is none of those: it is the network going away underneath a page
that is still running, which is exactly the event. It also cuts the dev server's own HTTP and HMR
sockets, which is faithful — a real drop takes those too.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/delay.mjs` | modify |

Nothing else is opened. `reconnecting.ts` is named above for the argument, not for reading — this
ticket changes no client code and asserts nothing about it.

## Scope

- The relay takes a fourth argument: `node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs>
  <controlPort>`. It is **optional**; with it absent the relay behaves exactly as `TASK-131001` left
  it, and `--selftest` still passes unchanged.
- With it present, a second `net` server listens on `127.0.0.1:<controlPort>`. Any connection to it
  **destroys every live relayed socket pair**, writes `cut <n>\n` naming how many pairs it
  destroyed, and ends that control connection. The relay keeps listening: the next page load
  reconnects through it normally.
- `node scripts/qa/delay.mjs cut <controlPort>` is the client side — it connects, prints what the
  relay wrote, and exits 0. A control port nothing is listening on exits 1 with the reason.
- `destroy()`, not `end()`. A half-close read as an orderly shutdown is not the event; the socket
  must go away.
- `--selftest-cut` is described under *Tests*.

## Out of scope

- **Cutting one connection rather than all of them.** There is no selector, no id and no filter. The
  drive that needs this has one browser tab through the relay at a time, and a selector would be
  machinery with no caller.
- **Reconnect timing.** How long `reconnecting.ts` waits is merged behaviour with its own unit tests
  (`retry-delay.ts`, `reconnecting.test.ts`); this ticket neither asserts nor changes it.
- **Driving `P3`.** That is `TASK-131006`. Nothing here touches the story's table beyond the
  seven-row regression gate.
- **`drive.mjs` and `stack.sh`.** Still untouched, for `TASK-131001`'s reason.

## Tests

`node scripts/qa/delay.mjs --selftest-cut`, hermetic on ephemeral ports against a throwaway echo
server it starts itself, must assert all four:

| Assertion | Proves | Fails when |
| --- | --- | --- |
| before the cut, a written payload echoes back through the relay | the fixture is a **live** connection, not an already-dead one | the test would have passed against a relay that never connected |
| `cut` reports `cut 1` | the control port counts the pair it destroyed | it reports `cut 0` and the assertion below is passing on a connection that was never relayed |
| the client socket emits `close` within 2 s of the cut | the sever reaches the far end | `end()` was used and the peer sits in a half-open state |
| a write **after** the cut never echoes | the pair is gone rather than merely quiet | the relay reopened, or the payload was buffered and delivered late |

**The first row is the load-bearing one.** A cut test whose connection was already dead passes
against a relay that does nothing at all; the pre-cut echo is what makes the other three mean
something.

`--selftest` from `TASK-131001` stays in `verify:` as a regression guard: the control port is
optional and must not have changed the delaying path.

## Acceptance criteria

- [ ] `node scripts/qa/delay.mjs --selftest-cut` exits 0 and prints the pre-cut echo, the `cut 1`
      line, and the observed close
- [ ] `node scripts/qa/delay.mjs --selftest` still exits 0, and its two round-trip figures still
      differ by ≥ 250 ms
- [ ] `node scripts/qa/delay.mjs` with no arguments still exits **2**, and the usage line now names
      `[controlPort]` and the `cut` verb
- [ ] `scripts/qa/delay.mjs` still contains no `import` outside `node:` and no `require(`
- [ ] **Driven once by hand, and pasted into the PR body as text**: with the relay on `5173` in
      front of Vite on `5273` and a control port, a browser holding a joined room is cut, and the
      page is observed to still be running — `node scripts/qa/drive.mjs 9232 text` answers with the
      room's screen rather than failing to attach. A human's verdict, never a gate (`ADR-0089` §2b)
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
