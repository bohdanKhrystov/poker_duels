# ADR-0025 — One ticker coroutine on the application scope drives both sweeps

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-019`
- **Unblocks:** `TASK-021212`

## Context

`RoomRegistry` has two time-driven sweeps and production calls neither. `reap()` (`TASK-020612`)
removes rooms idle past `RoomTimeouts`; `expireGracePeriods()` (specified as
`public suspend fun expireGracePeriods(): List<GraceExpiry>` by `TASK-020812`, not yet landed)
ends every disconnect grace window that has run out, folding the absent seat through the same
`act` path a played frame takes. Both read the injected `ServerClock`, both are `suspend`, and
both take each room's own mutex internally, one room at a time (`ADR-0016`). Every call site
today is a test, so a shipped server stalls a disconnected player's duel forever and accumulates
dead rooms without bound.

The forces:

- The registry is process-local memory, so the driver must run in the same process; anything
  external still ends in a call into this process and buys no isolation.
- A driver that outlives the Ktor application is a leak — a late pass would touch a closed
  connection pool. A driver that dies on its first exception is worse than none: one bad pass (a
  sink outage, one pathological room) becomes a permanently silent server that still answers
  `/health`.
- `expireGracePeriods()` returns per-seat frames (`GraceExpiry.outbound`) that must reach live
  sockets, or the present player learns of the fold only on their next round trip.
- Existing tests drive both sweeps directly against a `MutableClock` and must keep doing so, with
  no scheduler running.

## Decision

`Application.module()` launches **one ticker coroutine** on the `Application`'s own
`CoroutineScope`, immediately after the composition root builds `ServerComponents`. The loop body
is a named function in `Application.kt` whose KDoc cites this ADR. Each iteration, in order:

1. `delay(sweepPeriodMillis)` — delay first, so shutdown cancellation normally lands here and a
   just-started server does not sweep an empty registry.
2. `expireGracePeriods()`; for each returned `GraceExpiry`, hand its `outbound` to the existing
   `deliver(frames, room, connections)`. The frames are already per-seat projections — delivery
   routes them and adds nothing.
3. `reap()`, discarding or logging the removed codes.

Steps 2 and 3 are guarded **independently**: each catches every `Throwable` except
`CancellationException`, logs it through the application's `log`, and continues. A failing grace
pass does not skip reaping, a failing pass is retried on the next tick, and nothing but
cancellation ends the loop. `CancellationException` always rethrows, so shutdown works.

**One period for both sweeps.** `ServerConfig` gains `sweepPeriodMillis` — key
`server.sweepPeriodMillis`, env `SWEEP_PERIOD_MILLIS`, default `1_000`, required positive — read
once at startup like every other tunable. Scheduling is **fixed-delay, not fixed-rate**: the next
delay starts when the previous pass ends, so passes never overlap and an overrun stretches the
interval instead of piling up.

The scheduler is a caller, not a lock site: it never holds a room's mutex itself, and
`RoomRegistry.kt` keeps exactly its two existing `withLock` call sites. Lifecycle is structured
concurrency and nothing else: the coroutine is a child of the application's job, so stopping the
engine cancels it — no `Job` handle to keep, no plugin, no executor to shut down. Both sweeps stay
public methods on `RoomRegistry`; deterministic tests keep calling them directly on a test clock,
and only `SweepScheduleTest` boots the module and observes the loop on real time under shrunk
config.

## Consequences

**What it buys.** A shipped server that reaps and expires by itself, with the whole decision —
period, order, failure policy, lifetime — visible in one short function next to the composition
root that owns its collaborators. Shutdown is free and provable: cancellation of the application
scope is the only exit, so no sweep can outlive the pool it writes to. No-overlap comes from the
loop's shape rather than a lock. The choice is also the cheapest to reverse: a plugin wrapper,
a second period, or per-deadline timers are all strict additions later.

**What it costs.** Deadline precision is period precision: a grace window ends up to
`sweepPeriodMillis` plus the previous pass's duration late — at defaults, about one second on a
sixty-second window. The catch-and-continue guard turns a systematically failing pass into a
repeating error log instead of a crash, so an unwatched log can hide it; that is the accepted
price for never silently stopping, and a metrics ticket may later count failed passes. One period
couples the two sweeps — reap cannot be slowed without slowing expiry — accepted because the
unlocked pre-checks make an idle pass nearly free. Cancellation is cooperative: a pass mid
`sink.record` finishes its blocking JDBC call inside the engine's stop grace period, so a hung
store can stretch shutdown to the engine's hard timeout.

**What it forecloses.** Nothing structural. Sub-period deadline precision would need per-deadline
timers and a superseding ADR; everything else layers on. Sequencing note: the loop's grace half
needs `TASK-020812` to have landed `expireGracePeriods()`; ordering those tickets is the
planner's, not this ADR's.

Deciding now is nearly free because `TASK-021202` is assembling `module()` as this is written;
retrofitting a driver after a release ships means a production server whose duels stall while
every unit test passes — the reason `DEC-019` carried a deadline.

## Alternatives considered

**A Ktor plugin (`createApplicationPlugin` with lifecycle hooks).** The idiomatic packaging for
background work: declarative install, reusable across applications, hooks on monitoring events.
Rejected: there is one application and one install site, the plugin would launch on the very same
application scope, and it adds an indirection layer plus a second place to look for the period
while changing no behaviour. Wrapping this loop in a plugin later is mechanical if a second
consumer ever appears.

**An external trigger — an admin endpoint hit by cron or a platform scheduler.** Sweeping becomes
operationally visible and re-tunable without redeploy, and the process carries no timer.
Rejected: the registry is in-process memory, so the trigger still terminates in a call into this
same process — no isolation is gained — while adding an authenticated admin surface and making a
duel's grace expiry depend on infrastructure outside the shipped artifact. The server alone must
be sufficient to run its own duels.

**A `ScheduledExecutorService`.** Battle-tested fixed-rate scheduling with no coroutine
subtleties. Rejected: both sweeps are `suspend` functions, so every tick would bridge via
`runBlocking` on an executor thread — a second concurrency domain and a second shutdown path to
get wrong, replacing the structured cancellation Ktor already provides.

**Two loops with two periods.** Reap's precision need is minutes and expiry's is seconds, so each
could be tuned independently. Rejected: the cheap unlocked pre-checks make an idle pass nearly
free, so the saving is unmeasurable, while the cost is a second config key, a second loop, a
second failure path, and the loss of the guarantee that the two sweeps never interleave.

**A timer per deadline — a coroutine armed per grace window and per room timeout.** Exact
deadlines, no scanning at all. Rejected: this resurrects the per-room task lifecycle `ADR-0016`
declined — every reconnect, reap and rematch must find and cancel the right timer — and the
sweeps were built scan-shaped precisely to avoid that. Within-one-period precision is enough at
these timescales; if it ever is not, that is the superseding ADR.
