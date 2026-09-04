---
schema: 2
id: TASK-130905
title: The store arms the second hand, and boot names both the clock and the period
type: task
status: backlog
parent: STORY-1309
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, store, clock]
depends_on: [TASK-130904]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-store.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 14) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/duel-store.test.ts -t "keeps exactly one clock tick pending" 2>&1 | awk '/^ *Tests +[0-9]+ passed/ { n = $2 } END { exit !(n >= 1) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/boot.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 22) }'
  - sh -c 'grep -qF "applyServerMessage(state, message, now())" web-client/src/store/duel-store.ts'
  - sh -c 'grep -qF "CLOCK_TICK_MS = 1000" web-client/src/store/boot.ts'
  - sh -c 'grep -qF "now: () => performance.now()" web-client/src/store/boot.ts'
  - sh -c '! grep -qF "setInterval" web-client/src/store/duel-store.ts'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The store re-arms `ADR-0102` §4's existing `schedule` seam once a second while a turn clock is
live, reads its monotonic clock through a parameter `boot.ts` supplies rather than reaching for
`performance.now()` itself, and stops the moment the countdown can no longer move.

## Why the store, and why this seam

`ADR-0113` §6 names both halves: *"The clock source is monotonic and injected. The countdown reads
`performance.now()`, not `Date.now()` … and it reaches the store as a parameter at
`web-client/src/store/boot.ts`, the seam `ADR-0102` §4 already established for `REVEAL_STEP_MS`. So
timer tests state time instead of sleeping."* `EPIC-13`'s own gloss says the same of the ticking:
*"`ADR-0102` licensed a client-owned clock for pacing, and a ticking countdown is the same shape
applied to a server-stated deadline."*

So the ticker is the one `duel-store.ts` already has. `Schedule` is documented there as *"the
store's only door onto a clock"*, `armTick` already re-arms it while a reveal stands, and this is
that idiom a second time. **No component owns a timer**, which keeps `ADR-0032` §3 intact: screens
read through `useDuelState()` and hold no effect that pushes state.

`TASK-130811` left the socket seam reading `performance.now()` inline and said so — *"`STORY-1309`
takes the explicit `boot.ts` injection seam `ADR-0113` §6 names so a test can state time instead of
measuring it."* This is that ticket.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-store.ts` | modify |
| `web-client/src/store/boot.ts` | modify |
| `web-client/src/store/duel-store.test.ts` | modify |
| `web-client/src/store/duel-state.ts` | read — `tickClock`'s contract |

## Scope

- **`DuelStoreOptions` gains two:**
  - `readonly now?: () => number` — the monotonic reading, defaulting to `() => performance.now()`
    so the dozens of merged `createDuelStore()` call sites are untouched.
  - `readonly tickMillis?: number` — the countdown's period. **Absent or `0` means the store never
    arms a clock tick at all**, which is today's behaviour for every caller that does not ask.
    This is *not* `stepMillis`' meaning of `0`, and the KDoc must say so: a reveal at `0` releases
    synchronously, a clock at `0` does not run.
- **The socket seam uses the injected clock:** `applyServerMessage(state, message, now())`, gated
  as a fixed string. `Date.now` stays out.
- **One repeating tick, in `armTick`'s own shape:**

  ```ts
  const clockTick = (): void => {
    clockPending = false;
    const next = tickClock(state, now());
    if (next === state) return;
    state = next;
    notify();
    armClockTick();
  };

  const armClockTick = (): void => {
    if (tickMillis === 0 || clockPending || state.turnClock === null) return;
    clockPending = true;
    schedule(clockTick, tickMillis);
  };
  ```

  `armClockTick()` is called from `apply` after `notify()`, and from the reveal's own `tick` after
  its `notify()` — a `TurnClock` that arrived behind a paced runout only reaches `state.turnClock`
  when the queue drains, so the reveal's tick is the other place a clock can become live.
- **`clockPending` is what makes *exactly one* pending**, the same guarantee `armTick`'s
  `hadReveal` check gives the reveal. Two frames in a row must not stack two timers.
- **The stop is `tickClock`'s**, not a second condition here: it hands back the identical state once
  its held reading is past `expiresAt`, and `clockTick` returns without re-arming.
- **`setInterval` is not used and is gated absent.** One-shot re-arming is what makes a missed or
  delayed tick cost an update rather than compound.
- **`boot.ts`** exports `export const CLOCK_TICK_MS = 1000;` beside `REVEAL_STEP_MS`, with a KDoc
  citing `ADR-0108` §5's *"ticking once per second"*, and passes both
  `tickMillis: options.tickMillis ?? CLOCK_TICK_MS` and `now: () => performance.now()` into
  `createDuelStore`. `BootOptions` gains `readonly tickMillis?: number` alongside `stepMillis`, in
  the same words.

## Out of scope

- **Drawing.** No component reads anything this adds until `TASK-130907`.
- **`tickClock` itself.** Merged in `TASK-130904`; called, not edited.
- **`boot.test.ts`.** Both new options are defaulted, so no merged boot test changes; it is pinned
  at its present 22 to prove that rather than assume it.
- **`reconnecting.ts`'s own `setTimeout`.** A different clock for a different job; untouched.

## Tests

`duel-store.test.ts` — **5** added to the 9 it has, so the file reports **14**. Every one drives a
fake `schedule` that captures `(run, delay)` and a fake `now` the test states, so nothing sleeps
and no real timer is installed — the file stays outside `virtual-time.test.ts`'s reach.

| Test | Proves |
| --- | --- |
| `reads the clock it was given, never the host's own` | with `now` returning 1 000 then 5 500, a `TurnClock` of 30 000 ms leaves `turnEndsAt` 31 000, and a second one leaves 35 500 — two readings, because one cannot tell an injected clock from a constant |
| `arms no clock tick before a TurnClock has arrived` | after `RoomJoined` and a `Snapshot`, `schedule` has been called zero times for a clock |
| `ticks once every tickMillis while a clock is live` | with `tickMillis: 1000`, the captured delay is `1000`, and running the captured callback twice against two stated readings moves `nowMillis` twice |
| `keeps exactly one clock tick pending` | two frames applied back to back arm the clock tick **once**, not twice — counted, not inspected |
| `stops arming once the countdown has reached zero` | driving the captured callback past `expiresAt` produces one final update and then no further `schedule` call, so a finished duel does not re-render the tab once a second forever |

`boot.test.ts` (22) is pinned unmoved.

## Acceptance criteria

- [ ] `duel-store.test.ts` reports at least **14** passing tests and none failing
- [ ] `keeps exactly one clock tick pending` passes when run alone by name
- [ ] Each of the other four tests above passes, by name
- [ ] `boot.test.ts` still reports at least **22** passing tests and none failing
- [ ] `duel-store.ts` contains `applyServerMessage(state, message, now())` and no `setInterval`
- [ ] `boot.ts` declares `CLOCK_TICK_MS = 1000` and passes `now: () => performance.now()`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
