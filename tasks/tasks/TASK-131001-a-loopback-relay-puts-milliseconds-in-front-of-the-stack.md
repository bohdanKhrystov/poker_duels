---
schema: 2
id: TASK-131001
title: A loopback relay puts milliseconds in front of the stack
type: task
status: done
parent: STORY-1310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [qa, harness, refresh, manual-verify]
depends_on: []
verify:
  - node scripts/qa/delay.mjs --selftest
  - node scripts/qa/delay.mjs >/dev/null 2>&1; test $? -eq 2
  - awk '/^import / && $0 !~ /"node:/ { bad = 1 } /require\(/ { bad = 1 } END { exit bad ? 1 : 0 }' scripts/qa/delay.mjs
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/delay.mjs` exists: a loopback TCP relay that forwards a port to another port and puts a
fixed delay on every byte in both directions — so the rejoin round trip this story has to observe
becomes wider than the 250 ms at which `drive.mjs` samples the screen.

## Why this exists before any path is driven

`EPIC-13` measured five refresh paths on a bare localhost stack and wrote down the honest limit:
***"No lobby flash was observable at the sampling resolution `drive.mjs` allows."*** `ADR-0112` §6
then made *"real latency, where the rejoin round-trip is visible — a lobby flash localhost sampling
could not see"* one of the six paths this story owes.

Both statements are about the same missing instrument. On localhost the whole
socket-open → `JoinRoom` → `RoomJoined` round trip finishes inside one `wait`/`absent` sample, and
`record` cannot help across a page load because its `MutationObserver` dies with the document. So
**five of this story's seven readings would be statements about the sampler rather than about the
product.** This ticket is the sampler's answer, and it is deliberately the first thing that merges.

**Three instruments were considered; two do not work here.**

- **Chrome's `Network.emulateNetworkConditions`.** DevTools throttling shapes HTTP but is not
  documented to shape WebSocket frames, and the round trip this story needs to widen is a socket
  round trip. It would delay the boot bundle instead — the wrong side of the race.
- **`dnctl`/`pfctl` or Network Link Conditioner.** Real, and needs `sudo` and a loopback-shaping
  rule this repository cannot own. `ADR-0089` already carries one machine-local binary; a second
  one that needs root is worse.
- **A loopback relay in Node's own `net`.** No dependency (`ADR-0089` §2a holds literally and in
  substance), no root, delays HTTP and the WebSocket identically because it is below both, and
  deleting it is `ADR-0089` §6's one `git rm`.

## The layout, which is not the obvious one

**Vite moves and the relay takes `5173`.** `npm run dev -- --port 5273`, and the relay listens on
`5173` and forwards to `5273`.

Do not do it the other way round. `drive.mjs` hard-codes `localhost:5173` in two places — `APP`,
and the `close` verb's `list.find(x => x.url.includes("localhost:5173"))` — so a browser pointed at
a relay on some other port loses `close` outright and every `open` with no argument. Worse, the
page's **origin** would change, `localStorage` is per-origin, and `pd.deviceId` and `pd.roomCode`
would silently not be the ones the rest of the drive is about. Putting the relay on `5173` leaves
the browser's origin, the invite link the app prints, and every verb in `drive.mjs` byte-unchanged.

Two things this rests on were checked rather than assumed: `web-client/package.json`'s `dev` script
is bare `vite`, so `npm run dev -- --port 5273` forwards the flag; and `stack.sh wait-web` polls
`http://localhost:5173/`, so it waits on the **relay** and is the right readiness check for this
layout without being edited.

## Files

| File | Action |
| --- | --- |
| `scripts/qa/delay.mjs` | create |

Read, and nothing else: `scripts/qa/drive.mjs` for the file's conventions — a shebang-less ESM
script, `node:`-only, `process.exit` codes where `2` is usage.

## Scope

- `node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs>` listens on `127.0.0.1:<listenPort>`
  and, per accepted connection, opens one `net.connect` to `127.0.0.1:<targetPort>`.
- **Every chunk in both directions is written to the far side after `delayMs`**, via `setTimeout`.
  Chunk order is preserved because equal timeouts fire in the order they were armed; that is the
  whole ordering argument and it is worth one comment.
- **`end`, `close` and `error` are propagated after the same `delayMs`**, so a close never overtakes
  the bytes still in flight behind it. A relay that tore down immediately would truncate the last
  frame and the reading would be about this file.
- Backpressure is deliberately ignored — say so in a comment. The payloads are a dev bundle and JSON
  frames on loopback; a `write()` return value handled here would be machinery nothing needs.
- One line to `stderr` per accepted connection, so an operator watching the background task can see
  the relay is live.
- No argument, or a non-numeric one, prints usage to `stderr` and exits **2** — `drive.mjs`'s
  convention.
- `--selftest` is described under *Tests*.
- **The whole file is `S` — 120 changed lines, comments included.** The self-test's two relays are
  one helper called twice with a different delay, not two copies of the same block. If it does not
  fit, say so rather than growing it: the relay's behaviour is four small responsibilities and none
  of them is subtle.

## Out of scope

- **The cut.** Severing live connections on command is `TASK-131002`; do not add a control port here.
- **Any change to `scripts/qa/drive.mjs` or `scripts/qa/stack.sh`.** The layout above exists
  precisely so neither has to move. If one turns out to need a change, stop and say so rather than
  making it — it would widen `ADR-0089`'s licensed surface inside a ticket that is meant to narrow
  it.
- **Bandwidth, jitter, packet loss, a percentage of dropped frames.** One knob: a fixed delay.
- **Driving anything.** No browser, no stack, no room, no duel in this ticket.
- **Recording a path.** The story's table is untouched here, and the `verify:` gate that counts its
  seven rows is a regression guard, not this ticket's deliverable.

## Tests

**No suite covers `scripts/qa/`, and no suite is going to.** It is in no Gradle project, no
`package.json` script and no CI job (`ADR-0089` §6 depends on exactly that). So the instrument's own
claim is checked by a self-test inside it, and that self-test **is** the gate.

`node scripts/qa/delay.mjs --selftest` must:

| Assertion | Proves | Fails when |
| --- | --- | --- |
| the bytes echoed back equal the bytes sent, over **at least three** separately written chunks | the relay forwards faithfully and in order | a chunk is dropped, doubled or reordered |
| the round trip through a **50 ms** relay is ≥ 100 ms | the delay is applied at all, on both legs | the relay forwards immediately |
| the round trip through a **200 ms** relay exceeds the 50 ms one by ≥ 250 ms | the delay is the **argument** and not a constant | `delayMs` is ignored and a fixed sleep was written instead |

**The third row is the one that matters and it is why two relays are run rather than one.** A
self-test at a single delay is passed by a relay that sleeps a hard-coded 200 ms; only a second
input tells an argument from a constant.

The self-test binds **ephemeral ports** (`listen(0)`) and proxies to a throwaway `net` echo server
it starts itself. It touches no product port, no browser, no database and no dev server, and it
prints both measured round trips before exiting.

**On `ADR-0089` §2b, since a reviewer will ask.** §2b forbids a pull request waiting on a *QA case*
— a row of `docs/test-plan.md` driven against the running product through a browser. This self-test
drives nothing but itself: no Chrome, no stack, no case id, no `docs/test-plan.md` row, and it is
hermetic on loopback in under two seconds. It is a unit check of a file, and putting it in `verify:`
is what stops a broken instrument from producing five confident false readings downstream.

## Acceptance criteria

- [ ] `node scripts/qa/delay.mjs --selftest` exits 0 and prints two round-trip figures, the second
      at least 250 ms larger than the first
- [ ] `node scripts/qa/delay.mjs` with no arguments exits **2** and prints a usage line naming
      `<listenPort> <targetPort> <delayMs>`
- [ ] `scripts/qa/delay.mjs` contains no `import` outside `node:` and no `require(` — `ADR-0089`
      §2a, checked by the third `verify:` line
- [ ] **Driven once by hand, and the readings pasted into the PR body as text**: with Vite on
      `5273`, the relay on `5173` at 300 ms and the stack otherwise as it is, `node scripts/qa/drive.mjs
      9232 open` renders the first screen, `… click "Create a duel room"` produces a room code, and
      `… link` prints an invite link whose host is `localhost:5173`. This is the layout every later
      ticket in the story depends on, and it is a human's verdict, never a gate (`ADR-0089` §2b)
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
