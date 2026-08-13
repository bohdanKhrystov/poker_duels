---
schema: 2
id: TASK-020513
title: The concurrency test races on its own result list
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, test-reliability, concurrency]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*InMemoryPlayerDirectoryTest' --rerun-tasks
  - ./gradlew :poker-server:check
---

## Goal

`InMemoryPlayerDirectoryTest.concurrentResolvesOfOneDeviceCreateOneProfile` launches 100 coroutines
on `Dispatchers.Default` — a real thread pool — and each one does:

```kotlin
results.add(player.id)
```

on a plain `mutableListOf<PlayerId>()`. That is an unsynchronised `ArrayList` written from many
threads at once: a genuine data race, which can drop entries, corrupt the backing array, or throw
`ArrayIndexOutOfBoundsException` inside a job that `join()` then rethrows.

**The class under test is not at fault.** `InMemoryPlayerDirectory` uses
`ConcurrentHashMap.computeIfAbsent` and is correct. The race is in the test's own bookkeeping, which
means the test can fail while the code it exercises is working perfectly.

Observed once during `:poker-server:check` on the `TASK-021013` branch — failed, then passed on
rerun — and diagnosed during that ticket's review.

## Why it matters more than a rerun

A flaky test is worse than a missing one. It fails on unrelated branches, so the failure carries no
information; the cheapest response is always "rerun it", and the habit that builds is ignoring red.
Eventually someone removes the concurrency to make it stable, and the test stops covering the thing
it exists for — proving that concurrent first contact creates exactly one profile.

Every other concurrency test in this repository collects results safely (`awaitAll` returning
values, rather than mutation from inside the jobs), so this one is the outlier.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectoryTest.kt` | modify |

Do not modify `InMemoryPlayerDirectory` — it is correct.

## Scope

- Collect the results without a shared mutable list. Preferred: `async` returning the `PlayerId` and
  `awaitAll()` gathering them, which needs no synchronisation at all because nothing is shared.
- If a collection is genuinely needed, use a thread-safe one (`ConcurrentLinkedQueue`, or
  `Collections.synchronizedList`) — but the `awaitAll` shape is simpler and removes the class of bug
  rather than guarding it.
- **Keep the concurrency real.** Still 100 callers, still `Dispatchers.Default`, still gated so they
  start together. Making this test stable by making it sequential would remove the only reason it
  exists — `TASK-021004` proved by mutation that a sequential version passes against a broken
  check-then-act implementation.
- Keep the assertions: every caller receives the same `PlayerId`, and the directory holds exactly
  one profile.

## Tests

No new test. The existing one keeps its name and its assertions, and stops racing.

Run the suite with `--rerun-tasks` several times and say how many runs were clean. A single green
run does not distinguish "fixed" from "got lucky", which is the same reasoning that made the
original failure easy to dismiss.

## Done

Both `verify:` commands exit 0, the test still runs 100 concurrent callers on a real thread pool,
and no shared mutable collection is written from inside the jobs.
