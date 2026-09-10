---
schema: 2
id: TASK-131012
title: The relay reaches the origin it is pointed at
type: task
status: done
parent: STORY-1310
module: qa
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, harness, refresh]
depends_on: []
verify:
  - node scripts/qa/delay.mjs --selftest
  - node scripts/qa/delay.mjs --selftest-cut
  - node scripts/qa/delay.mjs --selftest-unreachable
  - node scripts/qa/delay.mjs --selftest-dualbind
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`scripts/qa/delay.mjs` reaches the origin a drive points it at, whichever loopback family that
origin bound, and says so at bring-up rather than at cut time.

## Why this exists

`TASK-131006`'s drive on 2026-09-10 reported `cut 0` — no app socket to sever — and a first pass
wrote a `P3` row from it anyway. Every reading in that row (`navigation.type` reads `navigate`,
`__pdFrames` survives, the board ticks 30 → 29 → 28) is exactly what an **undisturbed** page
produces, so the row could not distinguish *reconnected in place* from *never cut*. It was rejected.

The re-drive found the cause, and [`ADR-0156`](../../docs/adr/ADR-0156-the-relay-dials-by-name-and-holds-both-loopback-families.md)
measured it:

| Where | What |
| --- | --- |
| `scripts/qa/delay.mjs:66` | `connect(targetPort, "127.0.0.1")` — the target dial is IPv4, hard-coded |
| `web-client/vite.config.ts:7` | `preview: { port: 4173, strictPort: true }` — **no `host`**, so Vite binds `localhost` → `::1` |

The relay accepts the browser and cannot forward. Worse, and the reason this is a silence rather
than an error: **`[::1]:P` and `127.0.0.1:P` do not collide**, so a browser pointed at a relay that
never forwards still reaches a preview left on the same port *directly* — and the drive proceeds,
measuring a page nothing is cutting.

`ADR-0156` §1–4 decides the repair: the target is dialled **by name** with
`autoSelectFamily: true`; the relay holds **both** loopback families on its listen port or refuses
to start; and it proves it can reach the target at bring-up.

## Scope

Exactly what `ADR-0156` §6 lists. Read §§1–5 before starting; they are the specification and this
ticket does not restate them.

| File | Change |
| --- | --- |
| `scripts/qa/delay.mjs` | §§1–4 — dial the target by name with `autoSelectFamily: true`; bind both loopback families on the listen port, a failed second bind fatal; a reachability probe at bring-up; the `--selftest-unreachable` mode the gates run |
| `scripts/qa/drive.mjs` | `ADR-0117` §3 **as already merged** — `APP` (line 19) and the two `url.includes("localhost:5173")` tab filters (lines 39, 58) |

The four other `connect(..., "127.0.0.1")` sites (105, 127, 208, 241) **keep** `127.0.0.1` —
`ADR-0156` §2 gives the rule and the reason. Changing them is out of scope and a reviewer should
reject it.

## Out of scope

- **`web-client/vite.config.ts`.** `ADR-0156` moves the instrument, not the origin, so `ADR-0117`
  §1 is neither amended nor superseded and its "exactly this, and nothing else" stands.
- **`scripts/qa/stack.sh`.** `ADR-0117` §3's `stack.sh` half — `build-web`, `web-origin`,
  `wait-web [built|dev]`, `status`'s third mode — is still owed and is a separate ticket. Until it
  lands, `stack.sh`'s `WEB` and its `served`/`status` probes still name `5173`, and a drive on the
  built origin must not use them.
- **Driving `P3`.** That is `TASK-131006`, which this unblocks.

## Files

| File | Change |
| --- | --- |
| `scripts/qa/delay.mjs` | the dial, the dual bind, the bring-up probe, `--selftest-unreachable` |
| `scripts/qa/drive.mjs` | `APP` and the two tab filters move off `5173` |

## Tests

Three self-test modes, run as gates. `--selftest` and `--selftest-cut` exist and must keep passing;
`--selftest-unreachable` is new and is the one with teeth.

**The mutation this ticket's gate must be shown red against**, named by `ADR-0156` §6 rather than
chosen by the coder: put the target dial back to `connect(port, "127.0.0.1")` and run
`--selftest-unreachable` against a `::1`-bound echo server. It **must** fail. Restore with `cp`
from a backup made first, never with `git checkout --`, and report what the red run printed.

A second probe worth running, because it is the silence that produced the bad row: with the relay
listening on one family only, a client dialling the other family reaches **nothing** rather than
erroring usefully. `ADR-0156` §3 makes the failed second bind fatal for exactly this reason — check
that a single-family bind refuses to start rather than starting half-deaf.

No browser appears in any `verify:` command. `ADR-0089` §§2b–2c still hold: a browser drives a QA
round, never a gate.

## What this unblocks

`TASK-131006` — EPIC-13's last ticket, and the last of that epic's 88. It stays `ready`; no decision
blocks it any more. It gains this ticket in its `depends_on` and may not be driven until this lands.
Its stack paragraph becomes `ADR-0156` §5's three commands: preview on `4273`, the relay on `4173`,
browsers at the relay.
