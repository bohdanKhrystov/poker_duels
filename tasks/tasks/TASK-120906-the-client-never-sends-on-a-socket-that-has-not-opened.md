---
schema: 2
id: TASK-120906
title: The client never sends on a socket that has not opened
type: task
status: backlog
parent: STORY-1209
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/protocol/reconnecting.test.ts 2>&1 | grep -qF "a message sent before the socket opens is not sent, and does not throw"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/protocol/reconnecting.test.ts 2>&1 | grep -qF "a message sent after the socket opens reaches it"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/protocol/reconnecting.test.ts 2>&1 | grep -qF "a retry attempt does not accept a send until its own socket opens"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/protocol/reconnecting.test.ts
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`openReconnectingConnection` accepts a send only while a socket is genuinely open, so a press made
during a handshake is refused the way the module already intends rather than thrown away as an
uncaught `InvalidStateError`.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`. Reported against `SMK-04`'s
route as *"a silent no-op on the product's primary call to action"*, with no diagnosis; diagnosed
at triage.

**Press *Create a duel room* inside the socket handshake and the click is lost for good** — no room
code, no link, no error text, no loading indicator, and nothing that changes ten seconds later.

**The mechanism.** `web-client/src/protocol/reconnecting.ts:63–84`, `attach()`, sets `live = true`
on the line **after** the socket is constructed and **before** it has opened:

    const socket = options.openSocket();
    current = openConnection({ socket, storage: options.storage, onMessage: forward });
    live = true;

so the module's own guard —

    send(message: ClientMessage): void {
      // An action taken while disconnected is not an action.
      if (!live) return;
      current.send(message);
    },

passes for the whole handshake. `connection.send` guards only `status.kind === "outdated"`, reaches
`options.socket.send(...)`, and the browser throws. `Lobby.tsx:300` is
`onClick={() => send({ type: "CreateRoom" })}` with no pending state, so React swallows the
exception and the screen says nothing at all.

**The comment states the intent this ticket completes.** *An action taken while disconnected is not
an action* is the designed behaviour; `live` simply does not mean what the guard reads it as. The
repair is to make `live` true when the socket **opens** and false again when it closes — not to add
a second guard beside a field that lies.

## The reproduction, by hand (`ADR-0089` §4)

Run at `c05ee695` on the round's live stack. **The mechanism was varied, not just the operator**: a
trusted CDP `Input.dispatchMouseEvent` press/release at the control's own measured coordinates,
never `drive.mjs`'s in-page `.click()`, because a hand-check that reuses the harness's broken step
inherits its fault.

| run | button painted | click dispatched | outcome |
| --- | --- | --- | --- |
| 1 | +24 ms | +39 ms | *Waiting for your rival* at +142 ms |
| 2 | +18 ms | +22 ms | **nothing, ever** — screen unchanged after 10 s |
| 3 | +17 ms | +19 ms | **nothing, ever** |
| 4 | +17 ms | +18 ms | **nothing, ever** |

With `Runtime`/`Log` enabled, each failing run prints:

    InvalidStateError: Failed to execute 'send' on 'WebSocket': Still in CONNECTING state.

**It reproduces. It is a product defect, not a harness defect**, and no part of `scripts/qa/` is
touched by its repair.

## Why `medium` and not `high`

`uat` reported `high`. **Severity lowered, and the reasoning is written out because the count it
feeds decided the round's verdict.**

- **No promise in `EPIC-12`'s `high` row is broken.** Hole cards stay secret, the winner is right,
  the coins are right, rematch works. `STORY-1206` fixed the reading of that row — *a named list of
  product-integrity properties plus regressions, not a synonym for serious-feeling*.
- **Not a regression.** Nothing closed came back; this path has never been filed.
- **Nothing is lost but the click, and the product's own control resolves it**: press it again. That
  is a real defect with a workaround, which is `medium`.
- **The measured window is 4–20 ms wide on the machine that measured it.** No claim is made about
  its width elsewhere; a handshake is longer over a network than over a loopback, and that is a
  reason to fix it, not a measurement.
- **The downgrade cost the cycle work rather than saving it.** Round 1 is exempt from rule 4, so
  `high` here would have meant `PROCEED` and `medium` alone would have meant `PASS`. The severity
  did not move with the arithmetic.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/reconnecting.ts` | modify |
| `web-client/src/protocol/reconnecting.test.ts` | modify |

## Scope

- **`live` becomes true when the socket opens**, not when it is constructed, and false again on
  close — so the existing guard means what its comment says.
- **Every attempt gets the same treatment.** `attach()` runs once per retry, so a reconnect's own
  handshake is the same window; closing it only for the first attempt leaves the more common case
  open.
- **A send during a handshake is still swallowed, deliberately and without throwing.** This ticket
  does not queue, buffer or replay it — `ADR-0056` §3 keeps the client from retrying by itself, and
  a queued action replayed after a reconnect is an action the player did not take now.

## Out of scope

- **`Lobby.tsx`, and any pending or disabled state on the button.** Telling the player that the
  product is not ready yet is a screen affordance with words in it, and words need a merged source;
  it is not this ticket's, and it is not filed — the round story records it as the thing this repair
  deliberately does not do.
- **`web-client/src/protocol/connection.ts`.** Its `outdated` guard is correct and its `send` is
  right to trust its caller; the lying field is one layer up.
- **Every other unguarded `send` call site.** They all reach this module, so they are all repaired
  by this change and none of them is edited.

## Tests

`reconnecting.test.ts`

| Test | Proves |
| --- | --- |
| `a message sent before the socket opens is not sent, and does not throw` | with a socket held in `CONNECTING`, `send` reaches neither `socket.send` nor an exception — the fake socket records zero sends and the call returns normally |
| `a message sent after the socket opens reaches it` | the same connection, after `onopen`, passes the message through — so the fix is not "refuse everything", which the first test alone would accept |
| `a retry attempt does not accept a send until its own socket opens` | after a close and a scheduled re-`attach`, the window is closed on the **second** socket too |

The second test is not decoration: with only the first, `send() { return; }` passes.

## Acceptance criteria

- [ ] `reconnecting.test.ts > a message sent before the socket opens is not sent, and does not throw` passes
- [ ] `reconnecting.test.ts > a message sent after the socket opens reaches it` passes
- [ ] `reconnecting.test.ts > a retry attempt does not accept a send until its own socket opens` passes
- [ ] Reverting `reconnecting.ts` alone reddens all three
- [ ] **By hand, on a live stack**: navigate to `/` and dispatch a click on *Create a duel room* within
      ~20 ms of it painting; the console carries no `InvalidStateError`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
