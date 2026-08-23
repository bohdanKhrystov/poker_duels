---
schema: 2
id: TASK-040502
title: The wire carries a session token, names the player, and PROTOCOL_VERSION takes its step
type: task
status: done
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 27
atomic:
  - ProtocolVersionLedgerTest — a wire shape whose fingerprint no ledger row claims fails it, so the fields and the number cannot land apart
  - ProtocolDocumentationTest — theDocumentListsEveryProtocolError and theDocumentStatesTheCurrentProtocolVersion
  - ServerMessageHandshakeTest.theErrorSetIsExactlyWhatIsDeclared — a golden list of every ProtocolError name
  - the Kotlin compiler — Welcome gains a required playerId, so every construction of it stops compiling
  - ProtocolJsonTest — a golden literal whose subject is the version number itself
  - TypeScriptDeclarationsTest.aVariantCarriesItsDiscriminatorFirst — a golden literal of Hello's generated interface
  - verifyProtocolTypes and verifyDuelScript — byte comparisons that run on every check
  - tsc TS2322/TS2345/TS1360 — the ProtocolVersion alias and every client fixture that builds a Welcome or a Hello
  - vitest — three client suites assert the exact Hello frame this connection sends
depends_on: [TASK-040501]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolVersionLedgerTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolJsonTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.SessionHandshakeFieldsTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
  - cd web-client && npm run check
  - cd web-client && npm run build
---

## Goal

`Hello` can carry a session token, `Welcome` names the player and admits it may name no device,
`ProtocolError` has a value for a session that will not do, and `PROTOCOL_VERSION` takes the next
number free. **No token is read and no session is resolved anywhere:** the fields exist, the
server still resolves a device exactly as it did yesterday, and the null `deviceId` case is
unreachable until `TASK-040518`.

> **The story's design note says the bump is "this story's last ticket". It is this story's second,
> and the gate is why.** `ProtocolVersionLedgerTest.theLastRowsFingerprintEqualsTheComputedFingerprint`
> compares the *live descriptors* against the last ledger row, so the moment a wire field moves the
> version must move with it. "Fields now, number later" fails `check` on every commit in between.
> Every behavioural ticket after this one names `playerId` and `INVALID_SESSION`, so the fields go
> first and carry nothing.

## Files

Twenty-seven: twenty-six the `ADR-0070` probe measured on a tree with `TASK-040501` applied — see
*Notes* — plus this ticket's own new test file, which the probe structurally cannot find because no
merged gate fails for a test nobody has written. Do not reuse `TASK-021402`'s thirteen: that change
added no `ClientMessage` field and no `ProtocolError` value, and both cost files here.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ClientMessage.kt` | modify | `Hello.sessionToken` itself |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify | `Welcome.playerId`, and `deviceId` becoming nullable |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt` | modify | the `INVALID_SESSION` entry — an enum entry is a declaration |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | modify | `PROTOCOL_VERSION` and its KDoc history line |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify | `:poker-server:compileKotlin` — `Welcome` now needs a `playerId`, and this is the only place in `main` that builds one |
| `poker-server/src/test/kotlin/duels/poker/server/SeatDeliveryTest.kt` | modify | `compileTestKotlin` — `Welcome(deviceId = label)` no longer resolves |
| `poker-server/src/test/kotlin/duels/poker/server/duel/ScriptedDuel.kt` | modify | `compileTestKotlin` — same, at `ScriptedDuel.kt:108` |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketHandshakeTest.kt` | modify | `compileTestKotlin` — two `Welcome("…", PROTOCOL_VERSION)` assertions; see *Scope* for what they become |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolCodecTest.kt` | modify | `compileTestKotlin` — `ServerMessage.Welcome("d1")` |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | modify | `compileTestKotlin` on three `Welcome("device-1")`, **and** `theErrorSetIsExactlyWhatIsDeclared`'s golden list of every error name |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | modify | `:poker-server:test` — `theProtocolVersionIsFour` asserts the literal `4`; the version's own golden test, renamed and re-expected, never derived |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/typescript/TypeScriptDeclarationsTest.kt` | modify | `:poker-server:test` — `aVariantCarriesItsDiscriminatorFirst` pins `Hello`'s generated interface line by line |
| `docs/protocol.md` | modify | `:poker-server:test` — `ProtocolDocumentationTest.theDocumentListsEveryProtocolError` and `theDocumentStatesTheCurrentProtocolVersion` |
| `docs/protocol-versions.md` | modify | `:poker-server:test` — `ProtocolVersionLedgerTest`'s last-row version and fingerprint |
| `web-client/src/protocol/protocol.gen.ts` | regenerate | `:poker-server:verifyProtocolTypes` — a byte comparison on every `check` |
| `web-client/src/e2e/scripted-duel.gen.json` | regenerate | `:poker-server:verifyDuelScript` — it embeds a `Welcome` |
| `web-client/src/protocol/version.ts` | modify | `npm run check` — `tsc` TS2322, `Type '4' is not assignable to type '5'` |
| `web-client/src/protocol/connection.ts` | modify | `tsc` TS2345 on the `Hello` it sends, and TS2345/TS2322 on `writeDeviceId(storage, message.deviceId)` and the `ready` status, now that `deviceId` may be null |
| `web-client/src/protocol/connection.test.ts` | modify | `vitest` — *says hello with no device id on a first visit* deep-equals the whole frame |
| `web-client/src/protocol/reconnecting.test.ts` | modify | `tsc` TS1360 on two `Welcome` fixtures |
| `web-client/src/lobby/Lobby.test.tsx` | modify | `tsc` TS2322 on a `Welcome` fixture |
| `web-client/src/store/duel-provider.test.tsx` | modify | `tsc` TS2345 on a `Welcome` fixture |
| `web-client/src/store/duel-state.test.ts` | modify | `tsc` TS2345 on a `Welcome` fixture |
| `web-client/src/store/duel-store.test.ts` | modify | `tsc` TS2345 on two `Welcome` fixtures |
| `web-client/src/store/reconnect.test.tsx` | modify | `vitest` — three assertions deep-equal the exact `Hello` frames a reconnect sends |
| `web-client/src/e2e/whole-duel.test.tsx` | modify | `vitest` — *sends the handshake before it acts* deep-equals the `Hello` frame |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/SessionHandshakeFieldsTest.kt` | create | no gate — the tests the *Tests* section names. `ADR-0070`'s probe reads the paths a merged gate's failure names, and no gate fails for a test that does not exist yet, so a planner has to add this row by hand |

## Scope

- `Hello(deviceId: String? = null, protocolVersion: Int = PROTOCOL_VERSION, sessionToken: String? = null)`.
  **`sessionToken` goes last, after `protocolVersion`, and that position was chosen by measurement:**
  putting it second breaks every `Hello("d1", PROTOCOL_VERSION)` positional call and cost two more
  files for no wire benefit.
- `Welcome(playerId: String, deviceId: String?, protocolVersion: Int = PROTOCOL_VERSION)`.
  **`deviceId` gets no default.** A default of `null` would let a future call site omit the device
  a connection actually has, which is the `ADR-0027` §5 harm — a client concluding it has no device
  and minting a fresh one — reached by forgetting an argument.
- `ProtocolError.INVALID_SESSION`, placed **immediately before `REMATCH_UNAVAILABLE`** so the
  golden list in `ServerMessageHandshakeTest` and the bullet list in `docs/protocol.md` both take
  one inserted line. KDoc: *the client presented a session token that is invalid, expired or
  unknown.*
- `DuelSocket` passes `playerId = player.id.value, deviceId = deviceId.value`. **One line, no new
  behaviour**: `deviceId` is never null here, because nothing resolves a session yet.
- `DuelSocketHandshakeTest`'s two assertions become
  `ServerMessage.Welcome("player-1", "issued-1", PROTOCOL_VERSION)` and
  `ServerMessage.Welcome("player-1", "d1", PROTOCOL_VERSION)`. `InMemoryPlayerDirectory` mints
  `player-1`, `player-2`, … in resolve order, so `player-1` is a fact about the fixture, not a
  guess. **Nothing else in that file changes and no assertion is weakened** — both tests still
  compare the whole frame with `assertEquals`.
- `connection.ts`: send `sessionToken: null`; guard the write —
  `if (message.deviceId !== null) writeDeviceId(...)` — and widen `ConnectionStatus.ready`'s
  `deviceId` to `string | null`. That guard is `ADR-0030` §8's *write-once* rule arriving as the
  smallest thing `tsc` accepts; the client behaviour it implies is `STORY-0412`'s to finish.
- **Regenerate, never hand-edit**, both generated artifacts:
  `./gradlew :poker-server:generateProtocolTypes` and `./gradlew :poker-server:generateDuelScript`.
- **The version is read, not named.** Rebase on `develop` immediately before committing and set
  `PROTOCOL_VERSION` to what it says **plus one**. Append exactly one row to
  `docs/protocol-versions.md`, claimed by `STORY-0405`;
  `ProtocolVersionLedgerTest`'s failure message prints the row, fingerprint included. At the time of
  writing `develop` is at `4`, so the row is `5` — check, do not assume.

## Out of scope

- Reading `Hello.sessionToken`, resolving it, or emitting `INVALID_SESSION` — `TASK-040518`.
- A null `Welcome.deviceId` ever being sent — nothing can produce one until `TASK-040518`.
- `AuthSessions`, `IdentityResolver`, sign-in, sign-out — later in this story.
- Any client behaviour beyond what `tsc` and `vitest` force: no storage rule, no screen.

## Tests

`SessionHandshakeFieldsTest` — a new file under
`poker-server/src/test/kotlin/duels/poker/server/protocol/`. It constructs and round-trips frames;
it emits nothing.

| Test | Proves |
| --- | --- |
| `aHelloWithNoTokenRoundTripsWithANullToken` | `Hello(deviceId = "d1")` encodes and decodes back to an equal value whose `sessionToken` is `null` |
| `aHelloCarriesItsTokenAcrossTheWire` | `Hello(deviceId = "d1", sessionToken = "t")` round-trips to an equal value — **the pair matters: with only the null case, a field the encoder drops still passes** |
| `aWelcomeNamesThePlayerAndTheDevice` | `Welcome("p1", "d1")` round-trips equal, and its encoding contains both `"playerId":"p1"` and `"deviceId":"d1"` |
| `aWelcomeMayNameNoDevice` | `Welcome("p1", null)` round-trips equal and encodes `"deviceId":null` — present and null, not absent |
| `theTwoWelcomesAreNotEqual` | `Welcome("p1", "d1") != Welcome("p2", "d1")` and `Welcome("p1", "d1") != Welcome("p1", "d2")` — **two fixtures differing in one field each, so neither field can be a constant the equality ignores** |
| `invalidSessionIsOnTheWire` | `Failure(ProtocolError.INVALID_SESSION)` round-trips equal and encodes `"error":"INVALID_SESSION"` |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `ProtocolVersionLedgerTest`, `ProtocolDocumentationTest`, `ProtocolJsonTest`,
      `ServerMessageHandshakeTest` and `TypeScriptDeclarationsTest` all pass
- [ ] `docs/protocol-versions.md` has exactly one new row, whose version equals `PROTOCOL_VERSION`
      and whose fingerprint equals the one `ProtocolVersionLedgerTest` computes
- [ ] `web-client/src/protocol/protocol.gen.ts` and `web-client/src/e2e/scripted-duel.gen.json` were
      produced by their Gradle tasks, not by hand
- [ ] `DuelSocketHandshakeTest`'s two `Welcome` assertions name both a player and a device, and no
      other assertion in that file changed
- [ ] `git diff --name-only` lists exactly the twenty-seven rows of the *Files* table, no more
- [ ] Every command in `verify:` exits 0

## Proof

Delete `sessionToken` from the `Hello` this ticket sends in `connection.ts` and
`connection.test.ts` goes red on a deep-equal — run it. Give `Welcome.deviceId` a default of
`null` and nothing goes red, which is exactly why the ticket forbids the default rather than
testing for it.

## Notes

**Twenty-six was measured, not remembered** (`ADR-0069`, `ADR-0070`). On a clean tree with
`TASK-040501` applied, the four declarations above were stubbed together — the new `Hello` field,
the two `Welcome` fields, the new enum entry and `PROTOCOL_VERSION + 1` — and the commands
`.github/workflows/build.yml` runs on a pull request were run in full: `./gradlew check
-PrequireDocker=true`, then `npm ci`, `npm run check` and `npm run build` in `web-client/`. Every
path a failure named got the minimal propagation and the set was run again. Six iterations. The
loop stopped on **exit 0** at these twenty-six files, with 1337 Kotlin tests and 571 client tests
run and **none skipped** — Docker was present and the Testcontainers suites ran. The probe was
then reverted.

**Two failures only appeared after the compiler was green**, which is why a red run names a prefix
and not a set: `TypeScriptDeclarationsTest.aVariantCarriesItsDiscriminatorFirst` and the three
`vitest` suites asserting the literal `Hello` frame are invisible to `tsc` and to `compileKotlin`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
