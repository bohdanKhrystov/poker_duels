---
schema: 2
id: TASK-120502
title: The rival's presence reaches the other table
type: task
status: dropped
parent: STORY-1205
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, harness, superseded]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent check
  - ./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'
  - python3 .github/scripts/lint_tickets.py
---

## Addendum, 2026-09-01 — the reclassification below was wrong

**This ticket stays `dropped`, and this section is why it must.** On 2026-09-01 a human played a
real duel through two browsers and could not play it: both tables rendered correct, agreeing game
state and **each seat was told the other one had vanished**. That is this ticket's defect, alive,
and it reached a person because a round classified it as the round's own instrumentation.

**The sentence that was wrong** is in the drop note below: *"A player who is still connected is
present, and the server is right to say so."* Measured on the live stack at `e1a37a80`, four
reproductions and a controlled negative: the server sends
`{"type":"OpponentPresence","presence":"AWAY","graceRemainingMillis":60000}` to a **connected**
player, **320 ms before that player sends `CreateRoom`** — a frame about the room they left,
delivered to the connection they opened after it. With no stale frame in flight the same
create-and-join is clean, with zero presence frames on either wire.

**The experiment below was sound; its scope was not.** `location.href='about:blank'` really does
leave this browser's socket `ESTABLISHED`, and the original reproduction really had not established
its precondition. What was not tested is the navigation the catalogue actually uses: `drive.mjs
open` produces `OpponentPresence AWAY` on the other screen within milliseconds. So *"a navigation
is not a disconnect on this browser"* — the sentence in `docs/test-plan.md` that this drop note
rests on — is false, and `TASK-121401` removes it. A negative measured on one path was generalised
to a claim about the product.

**The process lesson, which is the point of keeping this file.** A failing case that does not
reproduce by hand is a harness defect (`ADR-0089` §4) — and that rule is right. What it does not
supply is the step this round skipped: **before reclassifying, reproduce the defect by a second
mechanism.** The paired experiment varied the *operator* (`close` versus `about:blank`) but never
the *mechanism*, so it could only ever confirm the instrument. `B(1)` was reduced from 2 to 1 on
that basis, and the cycle went on to report `PASS` three rounds later on a product that deadlocks
two connected players on the first hand of a fresh room.

**Where the repair lives now.** [`TASK-121403`](TASK-121403-presence-is-about-the-room-the-reader-is-in.md),
under [`STORY-1214`](../stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md),
blocked on `DEC-107`. **`TASK-120506` is not withdrawn** — it is `done`, and a case that can end a
browser session is correct and still wanted. The error was the reclassification, not the ticket it
produced. `B(1)` is **not** recomputed and round 1 is not reopened: `EPIC-12` §Termination rule 1
freezes a round's set at triage, and rewriting a closed round's arithmetic afterwards would hide
this event exactly as un-dropping this file would.

**Nothing below this line is edited.** `tasks/README.md` keeps a dropped ticket's file *"because
rewriting it as done would hide a real event in the trail"*, and the mistaken reclassification is
itself the event worth keeping.

---

## Dropped — reclassified as a harness defect, 2026-08-29

**Do not implement this ticket.** `CORE-18` was upheld as a `high` product defect at triage and
that classification was **wrong**. `qa-manager` re-ran `ADR-0089` §4's test on a path the original
reproduction did not cover, and the product is correct. Superseded by
[`TASK-120506`](TASK-120506-a-case-can-end-a-browser-session-and-says-so.md), a harness ticket.

**What decides it.** Both notices work, end to end, through the Vite dev proxy, when the player
performs an action that actually ends their session — closing the tab:

```
close A's app tab over CDP (/json/close/<targetId>)
  +4s   B: "Your rival is away. The duel is paused." 56   (seat plate reads Away)
  +8s   ... 52     +12s ... 48     +16s ... 44            (the grace window ticking)
reopen the room on A
  +4s   B: "Your rival is back."
```

Vite's upstream connections to Ktor fell from 3 to 1 at the moment of the close, so the teardown
crossed the proxy and the `OpponentPresence` push came back through it.

**Why the original reproduction was wrong.** It produced A's "absence" with
`location.href='about:blank'`, and on this headless Chrome **that does not close the WebSocket**.
A paired experiment on one tab, one socket type, one variable:

| what was done to a `ws://localhost:8080/ws` socket **not** going through the proxy | after 30s |
| --- | --- |
| explicit `.close()` | **gone within 3s** (2 sockets → 1) |
| `location.href='about:blank'` | **still ESTABLISHED** (2 sockets → 2) |

The instrument was validated in the same run: opening the socket took the count 1 → 2 and closing
it took it 2 → 1, so it detects a real close. The navigation leaves the connection up with the
proxy entirely out of the path — so the proxy cannot be what swallows a teardown, because on this
browser there is no teardown to swallow. **A player who is still connected is present, and the
server is right to say so.** The case's precondition was never established, so `CORE-18` was never
actually run.

**Why the dev-proxy hypothesis is rejected**, having been tested rather than argued: it predicts a
frame lost in transit, and the run above shows the frame arriving through that same proxy in under
four seconds. `web-client/vite.config.ts` is not changed by anything in this round.

**No production code may change for this** (`ADR-0089` §4, `EPIC-12` §Termination rule 6). This
ticket named three files under `web-client/src/` and would have had a coder edit them to satisfy a
defect in how the harness makes a player leave. That is the exact outcome §4 exists to prevent,
which is why the record below is kept rather than deleted.

`B(1)` falls from 2 to 1.

---

## Goal

When one player leaves a duel in progress, the other player's table says so — and says so again
when they come back.

## This ticket is `manual-verify`, and you must read why before you start

**None of the three commands in `verify:` fails today.** They are regression guards, not the gate.
Saying that plainly is the point: a gate that cannot fail is worse than an honest manual step, and
inventing a `grep` that passes either way would let a "fix" merge on a gate that never went red.

The gate is the **manual reproduction** under `## Acceptance criteria`. It must be run before and
after, and the before-run must fail.

The reason no command can express it is the defect itself: **both halves are already green.**

- `poker-server`'s `DuelSocketDisconnectTest` asserts the *other* seat is told —
  `aClosingSocketTellsTheOpponentItIsAway` and `aClosingHostTellsTheGuestItIsAway` — and passes.
- The client's `PresenceNotice.test.tsx`, `lobby/presence-copy.test.tsx` and `store/duel-state.ts`'s
  `OpponentPresence` reducer case are all tested and all pass.
- `protocol/frames.ts` lists `OpponentPresence` in its decode table, and the discriminator is
  identical on both sides.

Every unit is right and the composition is broken. That is `ADR-0088`'s named gap, and
`ADR-0089` §2b forbids a `verify:` that waits on a QA case, so there is nowhere else for the gate
to live. **If your investigation finds a unit-testable cause, add the failing test and put it in
`verify:` — that is strictly better than this ticket and you are asked to prefer it.**

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. Reported as `CORE-18` at
`medium`; raised to `high` by `qa-manager` and reproduced by hand. Recorded in
[`STORY-1205`](../stories/STORY-1205-round-1-the-identity-write-path-and-the-presence-line.md).

Two players are mid-hand. One navigates away from the app and stays away for ninety seconds, then
returns. The other player's screen never changes: no `Your rival is away. The duel is paused.`, no
countdown, and — when the first returns — no `Your rival is back.` The presence line renders as
the empty string throughout, so `state.rivalPresence` never leaves `null`.

**The remaining player's socket is alive.** The instant the returning player acted, their screen
updated in full — stacks, committed, the turn, the whole action set. Everything arrives except
presence.

Both directions of the channel are dark, so this is not the away half alone: `CORE-19`'s subject
is broken by the same cause, and repairing only the away notice would leave the return notice
missing.

## Files

The root cause is **not known**, which this ticket says rather than guessing. The evidence above
points between `connection.ts` and the reducer, so that is the budget:

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | modify |
| `web-client/src/store/boot.ts` | modify |
| `web-client/src/store/duel-provider.tsx` | modify |

You may **read** `web-client/src/store/duel-state.ts`, `web-client/src/lobby/Lobby.tsx` and
`web-client/src/table/PresenceNotice.tsx` to understand the path. You may not modify them without
a finding that says why — all three are covered by passing tests that assert the correct
behaviour.

**If the reproduction points at `poker-server/` instead, stop and say so.** That is a new ticket,
not a widened one (`CLAUDE.md` rule 4), and this ticket is then closed as misfiled.

## Scope

- Find where an `OpponentPresence` frame is lost between the socket and the reducer, and repair it.
- The repair must restore **both** notices: away on the drop and back on the return. A fix that
  lights one is half a fix.
- Add whatever unit test the found cause makes possible, and add its command to `verify:` — see
  the note above.

## Out of scope

- **Any change to the copy.** `presence-text.ts`'s three sentences and `ADR-0046` §2's rules are
  correct and tested. The words are not the defect.
- **Any change to the grace window, to `ADR-0028`'s countdown, or to what the server does for an
  absent seat.** Nothing here is about the timer; it is about a frame that does not arrive.
- **`poker-server/`**, unless the reproduction proves the frame is never sent — in which case see
  `## Files`.
- **`CORE-19` as a separate ticket.** Same cause, same fix, and `qa` passed it earlier in the same
  round; it is not filed twice.

## Tests

No test can be named in advance, because the cause is not known. What the fix must not break is
named instead, and those suites are in `verify:`:

| Suite | Proves |
| --- | --- |
| `web-client` `npm run check` | typecheck, lint, format and every client test, including the three presence suites |
| `:poker-server:test --tests '*DuelSocketDisconnectTest'` | the server still tells the other seat, both as host and as guest |

## Acceptance criteria

**The gate is this reproduction. Run it before, and it must fail.**

1. `node scripts/qa/drive.mjs 9232 forget-room` and the same on `9233`.
2. `node scripts/qa/drive.mjs 9232 open`, `click "Create a duel room"`,
   `wait "Waiting for your rival"`, `link`.
3. `node scripts/qa/drive.mjs 9233 open "<that link>"`, `wait "Blinds"` — both seated, hand dealt.
4. `node scripts/qa/drive.mjs 9232 eval "location.href='about:blank'"`.
5. Read `node scripts/qa/drive.mjs 9233 text` at roughly +5s, +15s and +25s.
6. `node scripts/qa/drive.mjs 9232 open "<that link>"`, then read `9233 text` again.

- [ ] At step 5, B's screen carries `Your rival is away. The duel is paused.` — today it carries
      nothing, which is the failing before-state this ticket must start from.
- [ ] At step 6, B's screen carries `Your rival is back.`
- [ ] `cd web-client && npm run check` is green.
- [ ] `./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'` is green.
- [ ] Every command in `verify:` exits 0.
- [ ] The PR body records the before-run and the after-run of the reproduction, as text.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
