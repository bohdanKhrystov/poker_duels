---
schema: 2
id: TASK-021113
title: The HTTP routes encode with the same Json the tests assert against
type: task
status: dropped
parent: STORY-0211
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, protocol, serialization, correctness]
depends_on: [TASK-021101]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*ServerPluginsTest'
  - ./gradlew :poker-server:check
---

## Dropped — the premise was false

**Ktor's no-argument `json()` does not produce a plain kotlinx `Json`.** It installs Ktor's own
`DefaultJson`, which sets `encodeDefaults = true`. Verified directly against the dependency on the
test classpath:

```
Ktor DefaultJson.encodeDefaults = true
```

So the HTTP path and `protocolJson` already agreed, and the failure this ticket described — a new
player's zero balance silently dropped from a response — could not happen.

Confirmed empirically before that: with the ticket's fix reverted to bare `json()`, the new
`theHttpPathEncodesDefaults` test still **passed**, because the default field was written either
way. That result is what prompted checking the premise rather than the test.

The claim entered the backlog from a review of `TASK-021101` which stated that `json()` yields
"a default `kotlinx.serialization.json.Json` whose `encodeDefaults` is `false` (the library
default)". True of kotlinx's own default; not true of Ktor's helper. The scheduler repeated it into
this ticket without checking, and `TASK-021101`'s merge commit message carries the same error.

Nothing is wrong in `develop`, so nothing is changed. The `TASK-021101` DTOs still declare no
default values, which remains good design — it makes the encoding safe irrespective of which `Json`
serialises them, and that argument never depended on this ticket being real.

**What stands from the episode:** `TASK-021101`'s two tests asserting a zero balance and a zero
delta survive encoding are worth keeping on their own merits, and the `encodeDefaults` trap is
still real in kotlinx generally — it has bitten this project three times. It simply is not present
on this transport.

---

## Goal

There are two `Json` instances in this server, and the tests use the safer one while the wire uses
the other.

- `Application.kt` installs `ContentNegotiation { json() }` — Ktor's no-argument helper, which
  builds a default `Json` with `encodeDefaults = false`.
- `protocolJson` in `protocol/Protocol.kt` sets `encodeDefaults = true`, and its KDoc explains why.
- `ProfileDtosTest` asserts encoded output using **`protocolJson`**.

So a test can prove a field appears on the wire while the actual HTTP response omits it.

Today nothing breaks, because no response DTO declares a default value and kotlinx only omits a
property that has one. That is a real protection and `TASK-021101` locked it with two tests. But it
is a property of the DTOs, not of the transport, and it holds only as long as nobody writes
`coinBalance: Int = 0`. The day someone does, the response silently loses the field **for a new
player** — the most common case in v0.1 — and the test suite stays green, because the tests encode
with the forgiving instance.

Found during the `TASK-021101` review, while resolving a contradiction in two reports about which
`Json` was in play. This is the fourth time `encodeDefaults` has cost this project time.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/ServerPluginsTest.kt` | modify |

Read `protocol/Protocol.kt`. Do not modify it, and do not change any DTO.

## Scope

- Install `ContentNegotiation` with **`protocolJson`** rather than a bare `json()`, so one `Json`
  serves both the WebSocket protocol and the HTTP responses, and a test that asserts on encoded
  output is asserting against what the wire will actually carry.
- Change nothing else in `Application.kt`. In particular `module()` still installs no duel socket
  and no profile routes — that stays `STORY-0212`'s, and this ticket must not pull a `DataSource`
  into `module()`.
- Add a comment saying why the shared instance matters, naming the failure it prevents. "Use the
  same Json" reads as tidiness; "otherwise a new player's zero balance is dropped from the response
  and no test notices" reads as the reason.

## Tests

| Name | Asserts |
| --- | --- |
| `theHttpPathEncodesDefaults` | the `Json` behind `ContentNegotiation` has `encodeDefaults` set — so a defaulted field would still be written, whatever a future DTO declares |

Assert on the configured instance rather than on a round trip through the route. A round trip
proves the current DTOs encode, which is already true and is not what this ticket changes.

If reaching the installed configuration is awkward, asserting that the same `protocolJson` value is
the one passed to `json(...)` is acceptable — the point is that the two paths cannot drift apart
silently.

## Done

All three `verify:` commands exit 0, `ProfileDtosTest` passes unedited, and the HTTP path and the
tests encode with one and the same `Json`.
