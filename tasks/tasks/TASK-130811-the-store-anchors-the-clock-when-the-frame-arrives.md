---
schema: 2
id: TASK-130811
title: The store anchors a TurnClock when the frame arrives and holds it as two deadlines
type: task
status: backlog
parent: STORY-1308
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, store, clock]
depends_on: [TASK-130810]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/duel-state.test.ts 2>&1 | grep -qE "Tests +82 passed \(82\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - sh -c '! grep -q "Date.now" web-client/src/store/duel-state.ts'
  - sh -c 'test -f web-client/src/store/duel-state.ts && grep -q "turnClock" web-client/src/store/duel-state.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The store keeps the turn clock the server stated, anchored with its own monotonic reading **at the
instant the frame arrived** rather than at the instant it is painted — so a clock queued behind a
paced runout is already reduced by the queue's dwell when the table finally draws it
(`ADR-0113` §6).

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-store.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

## Scope

- `applyServerMessage(state, message, arrivedAt: number = performance.now())` — a **third,
  optional** parameter, so no existing call site changes. It is read **before** the
  `state.reveal !== null` queue branch: the anchor is taken on arrival, and a queued frame carries
  its own `arrivedAt` with it in `Reveal.queued`.
- `DuelState` gains
  `turnClock: { seat: number; handNumber: number; actionSequence: number; turnEndsAt: number; expiresAt: number } | null`,
  `null` in `initialState()`. On a `TurnClock` frame the store derives
  `turnEndsAt = arrivedAt + turnRemainingMillis` and
  `expiresAt = turnEndsAt + bankRemainingMillis[seat]`, and stores **both** banks alongside as
  `bankRemainingMillis: readonly number[]`.
- `duel-store.ts` passes a monotonic reading into `applyServerMessage` at the socket seam —
  `performance.now()`, never `Date.now()`, for the reason `ServerClock`'s own KDoc gives: a host
  time correction must not stretch or collapse a countdown.
- A `RoomJoined` naming a **different** room clears `turnClock`, exactly as it already clears the
  rest of the room's facts (`ADR-0104`, `TASK-121406`).
- The store **states nothing**: reaching either deadline enables no control, sends nothing and
  changes no other field. Zero is not an event.

## Out of scope

- **Drawing anything.** No component reads `turnClock` after this ticket — `STORY-1309` draws the
  countdown, moves `presence-countdown.ts` to `countdown.ts`, and takes the explicit
  `boot.ts` injection seam `ADR-0113` §6 names so a test can state time instead of measuring it.
- Ticking. Nothing sets an interval here.
- The server — `TASK-130805` through `TASK-130810`, merged.

## Tests

`duel-state.test.ts` — 75 today, **82** after. **No existing test is edited or removed**; seven are
added, each passing an explicit `arrivedAt` so nothing depends on a real clock.

| Test | Proves |
| --- | --- |
| `starts with no turn clock` | `initialState().turnClock` is `null` |
| `anchors the clock at the instant the frame arrived` | With `arrivedAt = 1_000` and `turnRemainingMillis = 30_000`, `turnEndsAt === 31_000` |
| `a second arrival anchors at its own instant` | The same frame at `arrivedAt = 5_500` gives `turnEndsAt === 35_500` — two inputs, because one cannot tell an anchor from a constant |
| `the expiry is the allowance plus that seat's bank` | `expiresAt === turnEndsAt + bankRemainingMillis[seat]`, asserted for seat 0 **and** seat 1 with different banks |
| `holds both banks the server stated` | `bankRemainingMillis` is the two-element list from the frame, in seat order |
| `a clock queued behind a runout keeps its arrival anchor` | A `TurnClock` delivered while a reveal stands, then drained by `advanceReveal`, carries `turnEndsAt` derived from its **arrival** `arrivedAt` and not from the drain — asserted at two different dwells |
| `a RoomJoined naming a different room clears the clock` | `turnClock` is `null` again |

## Acceptance criteria

- [ ] Each of the seven tests above passes, by name
- [ ] `duel-state.test.ts` reports exactly **82** tests, so nothing merged was dropped
- [ ] `duel-state.ts` names `turnClock` and does **not** name `Date.now`
- [ ] `npm run check` and `npm run build` in `web-client/` both exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
