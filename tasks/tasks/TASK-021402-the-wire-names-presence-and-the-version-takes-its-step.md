---
schema: 2
id: TASK-021402
title: OpponentPresence and ActedForAbsent reach the wire, and PROTOCOL_VERSION takes its step
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: standard
labels: [server, protocol, presence, version-bump]
files_touched: 13
atomic:
  - ProtocolVersionLedgerTest — a wire shape whose fingerprint no ledger row claims fails it
  - ProtocolDocumentationTest — a live type with no row, and a row with no live type, both fail
  - the Kotlin compiler — the exhaustive when over ServerMessage in DuelSocket.kt
  - the Kotlin compiler again — two more in test sources, SocketDuel.kt and SocketSecrecyTest.kt
  - ProtocolJsonTest — a golden literal whose subject is the version number itself
  - verifyProtocolTypes and verifyDuelScript — byte comparisons run on every check
  - tsc TS1360 — the satisfies table in frames.ts; tsc TS2322 — the ProtocolVersion alias in version.ts
depends_on: [TASK-021401]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDocumentationTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolVersionLedgerTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolPayloadTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolDiscriminatorTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.ProtocolJsonTest'
  - ./gradlew :poker-server:test --tests 'duels.poker.server.protocol.PresenceFramesTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
  - cd web-client && npm run check
  - cd web-client && npm run build
---

## Goal

`SeatPresence`, `OpponentPresence` and `ActedForAbsent` exist on the wire, `PROTOCOL_VERSION`
takes the next number free, and both documents and both generated client artifacts move in the same
commit. No frame is emitted anywhere: the types exist and nothing builds one.

> **`DEC-066` is answered by [`ADR-0071`](../../docs/adr/ADR-0071-a-discriminator-is-its-kotlin-type-name.md).**
> `ADR-0028` §1's `ActedForAbsentSeat` is 18 characters and
> `ProtocolDiscriminatorTest.everyDiscriminatorIsShortAndUnique` — merged since `TASK-020210` —
> fails the build over 16. **The type is renamed `ActedForAbsent`** (14), Kotlin name and
> `@SerialName` together and identical to each other (`ADR-0071` §1); nothing else in `ADR-0028` §1
> moves. **The gate is not edited**: `ProtocolDiscriminatorTest.kt` is *not* a fourteenth *Files*
> row, `files_touched` stays 13, and `ProtocolDiscriminatorTest` stays in `verify:` as a gate this
> ticket satisfies. Write `ActedForAbsent` exactly; do not restore the longer name and do not
> shorten only the annotation.

## Files

Thirteen, and each is held by a merged gate. **This count was measured, not remembered** — see
*Notes*. `TASK-021301`'s seventeen is a fact about that ticket and is not a starting point: this
story adds no `ProtocolError` value and no `ClientMessage` variant, so the two golden tests that
cost `TASK-021301` its third stall are untouched here, and so is `connection.test.ts`.

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/SeatPresence.kt` | create | the enum itself; ktlint's filename rule gives a single top-level declaration its own file, as `ProtocolError.kt` already has |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify | the two subtypes themselves |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | modify | `PROTOCOL_VERSION` and its KDoc history line |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify | `:poker-server:compileKotlin` — `'when' expression must be exhaustive` at `DuelSocket.kt:114` |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify | `:poker-server:compileTestKotlin` — the same error at `SocketDuel.kt:232` |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketSecrecyTest.kt` | modify | `:poker-server:compileTestKotlin` — the same error at `SocketSecrecyTest.kt:54` |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | modify | `:poker-server:test` — `theProtocolVersionIsThree` asserts the literal 3; the version's own golden test, renamed and re-expected, never derived |
| `docs/protocol.md` | modify | `:poker-server:test` — `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient` and `theDocumentStatesTheCurrentProtocolVersion` |
| `docs/protocol-versions.md` | modify | `:poker-server:test` — `ProtocolVersionLedgerTest.theLastRowsVersionEqualsProtocolVersion` and `theLastRowsFingerprintEqualsTheComputedFingerprint` |
| `web-client/src/protocol/protocol.gen.ts` | regenerate | `:poker-server:verifyProtocolTypes` — a byte comparison on every `check` |
| `web-client/src/e2e/scripted-duel.gen.json` | regenerate | `:poker-server:verifyDuelScript` — it embeds a `Welcome`'s `protocolVersion` |
| `web-client/src/protocol/frames.ts` | modify | `npm run check` — `tsc` TS1360 at `frames.ts:17`, the `satisfies Record<ServerMessage["type"], true>` table; its header comment names the pre-`ADR-0071` type and is corrected in the same edit |
| `web-client/src/protocol/version.ts` | modify | `npm run check` — `tsc` TS2322 at `version.ts:11`, `Type '3' is not assignable to type '4'` |

Read `protocol/ProtocolError.kt` (the enum style to copy), `protocol/ServerMessage.kt`,
`docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §§1–2 and §8, and
`docs/adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md` §§1–2. Nothing else.

## Scope

- `public enum class SeatPresence { PRESENT, AWAY, ABSENT }`, `@Serializable`, its own file,
  KDoc'd. The three values are the three states `Room` already distinguishes.
- `ServerMessage.OpponentPresence(presence: SeatPresence, graceRemainingMillis: Long? = null)`,
  with `ADR-0028` §1's two `require`s verbatim: the nullable field is present **exactly when**
  `AWAY`, and never negative. **No seat field** — it is recipient-relative, like `YourTurn`.
- `ServerMessage.ActedForAbsent(seat, handNumber, actionSequence, action: ActionType)`, with
  `require(seat in 0..1)` and a `require` refusing any action but `FOLD` or `CHECK`.
  `@SerialName("ActedForAbsent")` — the same string as the Kotlin type name, per `ADR-0071` §1.
- **Regenerate, never hand-edit**, both generated artifacts:
  `./gradlew :poker-server:generateProtocolTypes` and `./gradlew :poker-server:generateDuelScript`.
- **`frames.ts` carries a stale comment.** Its header names `ActedForAbsentSeat` as the type
  `ADR-0028` will add; correct it to `ActedForAbsent` in the same edit that adds the two
  `SERVER_MESSAGE_TABLE` rows. No new file — that row is already in the table.
- **The version is read, not named.** Rebase on `develop` immediately before the commit and set
  `PROTOCOL_VERSION` to whatever it says **plus one** (`ADR-0045` §4). Append **one** ledger row to
  `docs/protocol-versions.md`; `ProtocolVersionLedgerTest`'s failure message prints the exact row,
  fingerprint included. Claimed by `STORY-0214`.
- The four propagations are **one line each and add no behaviour**: two branches in each exhaustive
  `when`'s no-op arm, and the version literal. `ProtocolJsonTest`'s method is renamed to match the
  number it asserts, exactly as `TASK-020732` requires — the expectation is **updated, never
  derived**, because that test's subject *is* the number.

## Out of scope

- **Emitting either frame anywhere.** No `Room`, `RoomRegistry`, `foldAbsent` or `DuelSocket`
  behaviour: `TASK-021403`–`TASK-021409`.
- `Room.presenceOf` — `TASK-021403`.
- Anything in `poker-engine`: no clock, no absence, no event, no `EVENT_SCHEMA_VERSION`.
- Any `ClientMessage` field, and any new `ProtocolError` value. Neither exists in `ADR-0028`.
- Rendering: `STORY-0313`.

## Tests

`PresenceFramesTest` — a new file under
`poker-server/src/test/kotlin/duels/poker/server/protocol/`. It constructs frames only; it emits
nothing. The two existing suites named in `verify` cover the rest without being told to:
`ProtocolPayloadTest`'s descriptor walk reaches both new types the day they are added, and
`ProtocolDocumentationTest` pins the document against the live set.

| Test | Proves |
| --- | --- |
| `awayCarriesARemainingDuration` | `OpponentPresence(SeatPresence.AWAY, 45_000L)` constructs, and round-trips through `protocolJson` to an equal value |
| `awayWithZeroRemainingIsLegal` | `OpponentPresence(SeatPresence.AWAY, 0L)` constructs — `ADR-0028` §2's window that ran out before the sweep landed |
| `awayWithoutARemainingIsRefused` | `OpponentPresence(SeatPresence.AWAY, null)` throws `IllegalArgumentException` |
| `presentWithARemainingIsRefused` | `OpponentPresence(SeatPresence.PRESENT, 1L)` throws; and so does `ABSENT` with one — both values, so the `require` cannot be an `AWAY`-only check |
| `aNegativeRemainingIsRefused` | `OpponentPresence(SeatPresence.AWAY, -1L)` throws |
| `presentAndAbsentCarryNothing` | `OpponentPresence(SeatPresence.PRESENT)` and `OpponentPresence(SeatPresence.ABSENT)` both construct with the default |
| `theServerOnlyEverFoldsOrChecksForAnAbsentSeat` | `ActedForAbsent(0, 1, 0, ActionType.FOLD)` and `(1, 1, 0, ActionType.CHECK)` construct; `CALL`, `BET`, `RAISE` and `ALL_IN` each throw — all four, not one |
| `aMarkNamesASeatAtTheTable` | `ActedForAbsent(2, 1, 0, ActionType.FOLD)` and seat `-1` both throw |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `ProtocolDocumentationTest`, `ProtocolVersionLedgerTest`, `ProtocolPayloadTest`,
      `ProtocolDiscriminatorTest` and `ProtocolJsonTest` all pass
- [ ] `web-client/src/protocol/protocol.gen.ts` names `SeatPresence`, `OpponentPresence` and
      `ActedForAbsent`, and was produced by `./gradlew :poker-server:generateProtocolTypes`
      rather than by hand
- [ ] `docs/protocol-versions.md` has exactly one new row, whose version equals `PROTOCOL_VERSION`
      and whose fingerprint equals the one `ProtocolVersionLedgerTest` computes
- [ ] No frame is constructed anywhere in `main` outside the two new `data class` declarations —
      `git diff` touches no file outside the *Files* table
- [ ] Every command in `verify:` exits 0

## Notes

**Thirteen was measured by the `ADR-0070` §2 probe, not read off a design.** On clean `develop`, a
throwaway enum and two throwaway `ServerMessage` variants carrying the fields `ADR-0028` §1 states
were added and `PROTOCOL_VERSION` was re-valued; the commands `.github/workflows/build.yml` runs —
`./gradlew check -PrequireDocker=true`, then `npm ci`, `npm run check` and `npm run build` in
`web-client/` — were run in full; every path a failure named got the minimal propagation and the set
was run again. Seven iterations. The loop stopped on **exit 0** at these thirteen files, with 1285
tests run and **none skipped**, Testcontainers suites included. The probe was then reverted. A red
run names a prefix; this one is green, so an eighteenth gate-held file would have to fail while the
gate set passes.

`ProtocolDiscriminatorTest` failed on the eighteen-character name and that was `DEC-066`, now
answered by `ADR-0071`: the type is `ActedForAbsent`, fourteen characters, and the gate is unedited.
The probe had continued past the failure under a provisional 14-character discriminator, which is why
the other twelve rows were a green measurement then and why thirteen is still the count now — the
answer chose a name of exactly that length, so no row moves.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
