---
schema: 2
id: TASK-040509
title: The directory can find a profile without creating one
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 5
atomic:
  - the Kotlin compiler — a new abstract member on PlayerDirectory is not implemented by PostgresPlayerDirectory or InMemoryPlayerDirectory, and neither class can be left out
  - the Kotlin compiler again — PlayerDirectory stops being a `fun interface` the moment it declares two members, and that is one edit, not two
labels: [server, identity, db]
depends_on: [TASK-040508]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPlayerDirectoryTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.session.InMemoryPlayerDirectoryTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PlayerDirectory` can answer *which player owns this device* without minting one, which is what
lets an HTTP route resolve a device id and still refuse an unknown one — the rule `ADR-0030` §4
moves up out of `ProfileReads`.

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` | modify | the member itself, and `fun interface` → `interface` |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt` | modify | `:poker-server:compileKotlin` — *not abstract and does not implement abstract member* |
| `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectory.kt` | modify | `:poker-server:compileTestKotlin` — the same error |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryTest.kt` | modify | the tests this ticket's *Tests* section names against the real schema |
| `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectoryTest.kt` | modify | the tests that pin the double's answer to the same contract |

Read `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §4 and
`docs/adr/ADR-0012-device-bound-anonymous-profiles.md` (why an HTTP path may not create). Nothing
else.

## Scope

- `public suspend fun findOrNull(deviceId: DeviceId): Player?` on the port, KDoc'd with the reason
  it exists rather than the shape it has: `resolve` creates, and **a crawler hitting an HTTP route
  must not mint rows**, so identity resolution outside the socket needs a read that cannot.
- `PlayerDirectory` stops being a `fun interface`. No SAM conversion of it exists anywhere —
  `grep -rn "PlayerDirectory {" poker-server/src` finds only the two implementing classes and the
  declaration — so this costs nothing beyond the keyword.
- `PostgresPlayerDirectory`: `SELECT id FROM player WHERE device_id = ?`, in the same
  `withContext(Dispatchers.IO) { dataSource.connection.use { … } }` shape `resolve` already uses.
  **No `INSERT`, no `ON CONFLICT`, no `RETURNING`.**
- `InMemoryPlayerDirectory`: a map read. It must **not** go through `computeIfAbsent`.

## Out of scope

- `IdentityResolver`, which is the first caller — `TASK-040510`.
- Any change to `resolve`, its upsert, or `ADR-0030` §2's count of statements that write `player`.
- `ProfileReads` — `TASK-040511`.

## Tests

`PostgresPlayerDirectoryTest` and `InMemoryPlayerDirectoryTest` — new methods only, nothing
existing edited.

`PostgresPlayerDirectoryTest`

| Test | Proves |
| --- | --- |
| `findingAKnownDeviceAnswersTheSamePlayerResolveDid` | `resolve` then `findOrNull` on the same device answer the same `PlayerId`, and a **second** device resolved in the same test answers a different one — one device alone agrees with a query that ignores its argument |
| `findingAnUnknownDeviceIsNull` | `findOrNull(DeviceId("ghost"))` is `null` |
| `findingAnUnknownDeviceCreatesNothing` | `SELECT count(*) FROM player` is unchanged across the call — the whole point |

`InMemoryPlayerDirectoryTest`

| Test | Proves |
| --- | --- |
| `theDoubleFindsWhatItResolved` | the same two-device shape as above |
| `theDoubleCreatesNothingOnAMiss` | `profileCount` is unchanged after `findOrNull` on an unknown device |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `PostgresPlayerDirectory.findOrNull`'s statement contains no `INSERT`
- [ ] `InMemoryPlayerDirectory.findOrNull` does not call `computeIfAbsent`
- [ ] Every test that was in either file before this ticket still passes, unedited
- [ ] Every command in `verify:` exits 0

## Proof

Implement `findOrNull` as `resolve(deviceId)` and only the two *creates nothing* tests go red —
which is why a bare *finds what it resolved* pair would not be enough on its own.

## Notes

**Five is the compiler's three plus this ticket's own two test files.** The three source files were
measured: on a clean tree, adding the member and both bodies compiled with no other failure, and
`./gradlew check -PrequireDocker=true` stayed green — the change is additive and nothing calls it
yet. The two test files are named in *Tests* and a gate cannot name a test nobody has written.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
