---
schema: 2
id: TASK-040309
title: Four verifications at a time, and no more
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, concurrency, security]
depends_on: [TASK-040308]
verify:
  - ./gradlew :poker-server:test --tests '*Argon2ConcurrencyTest'
  - sh -c 'grep -q "argon2Dispatcher" poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt && ! grep -q "withContext(Dispatchers.IO)" poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Argon2 work runs on `Dispatchers.IO.limitedParallelism(4)`, so peak memory is bounded at roughly
4 × 19 MiB and a burst of sign-in attempts cannot turn a memory-hard hash into a self-service denial
of service.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt` | modify — the dispatcher, and the two `withContext` calls that use it |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2ConcurrencyTest.kt` | create |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1's last bullet, the bound and the reason for it |

## Scope

- `internal const val ARGON2_MAX_PARALLEL = 4` and
  `internal val argon2Dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(ARGON2_MAX_PARALLEL)`,
  declared once at file scope in `Argon2Hasher.kt`. One instance for the process: a dispatcher built
  per call bounds nothing.
- `limitedParallelism` is `@ExperimentalCoroutinesApi` in the coroutines version this project pins,
  so the declaration carries `@OptIn(ExperimentalCoroutinesApi::class)`. That opt-in is the whole
  extent of the experimental surface used.
- Both `hash` and `matches` change from `withContext(Dispatchers.IO)` to
  `withContext(argon2Dispatcher)`. **No `Dispatchers.IO` remains in the file** except inside the
  `limitedParallelism` call that derives from it.
- A comment saying what the number buys — 4 × 19 MiB, not 19 MiB × however many requests arrive —
  because the number looks arbitrary and is not.

## Out of scope

- Rate limiting failed sign-ins by remote address. `ADR-0027` §6 puts that with the endpoint that
  needs it, in `STORY-0405`.
- Bounding anything else in the server. This dispatcher is Argon2's, and nothing else uses it.
- Making the bound configurable. It is a constant until something measures a reason.

## Tests

`Argon2ConcurrencyTest`. Both tests are deterministic: **nothing asserts on elapsed wall-clock
time**, which is how a concurrency test becomes a flaky test.

| Test | Proves |
| --- | --- |
| `exactlyFourCoroutinesRunAtOnceOnTheArgon2Dispatcher` | eight coroutines launched on `argon2Dispatcher` each increment an `AtomicInteger`, record the running peak, count down a `CountDownLatch(4)` and then await it with a five-second timeout. With a bound of four the latch reaches zero and every task proceeds; with a smaller bound the await times out and the test fails; with a larger one the recorded peak exceeds four. The assertion is `assertEquals(4, peak)` |
| `eightConcurrentVerificationsAllSucceed` | eight real `matches` calls against one hashed secret, launched together, all return `true` — the bound queues work rather than starving or deadlocking it, which a blocking call inside a limited dispatcher can do |

## Acceptance criteria

- [ ] `Argon2ConcurrencyTest.exactlyFourCoroutinesRunAtOnceOnTheArgon2Dispatcher` passes and asserts
      the peak is **exactly** four — `assertTrue(peak <= 4)` would also pass with a bound of one,
      which is a different defect and an equally broken server
- [ ] The latch await has an explicit timeout and its failure message says a bound smaller than four
      was observed; a test that hangs forever is not a test
- [ ] `Argon2ConcurrencyTest.eightConcurrentVerificationsAllSucceed` passes, asserting all eight
      results, not just that the calls returned
- [ ] `Argon2Hasher.kt` mentions `argon2Dispatcher` and no longer contains
      `withContext(Dispatchers.IO)` — the second `verify` command checks both
- [ ] The dispatcher is a single file-scope `val`, not built inside `hash` or `matches`
- [ ] `Argon2HasherTest`'s eight tests still pass unchanged — the dispatcher swap changes where the
      work runs, never what it computes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
