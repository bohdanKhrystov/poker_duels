---
schema: 2
id: TASK-031202
title: A whole duel written down as each seat's own session of frames
type: task
status: done
parent: STORY-0312
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, testing, fixture, secrecy]
depends_on: [TASK-031201]
verify:
  - ./gradlew :poker-server:test --tests '*ScriptedDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

One deterministic duel becomes two ordered scripts — one per seat — of exactly the frames that seat
would have received and the exactly `Act` frames it would have sent, every one of them encoded by
the server's own `ProtocolCodec`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/ScriptedDuel.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/ScriptedDuelTest.kt` | create |

Read, do not modify: `duel/PlayedDuel.kt` (`playDuel`, `PlayedDuel.acts`),
`protocol/ProtocolCodec.kt` (`encode`), `protocol/ServerMessage.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt`.

## Scope

`ScriptedDuel.kt` is **at the S ceiling**. Build exactly the shape below and nothing beside it.

- Four `@Serializable` types, `internal`, in package `duels.poker.server.duel`:

  ```kotlin
  @Serializable internal data class ScriptStep(val from: String, val frame: String)
  @Serializable internal data class RivalHand(val handNumber: Int, val cards: List<String>)
  @Serializable internal data class ScriptedSeat(
      val viewerSeat: Int,
      val steps: List<ScriptStep>,
      val rivalHoleCards: List<RivalHand>,
  )
  @Serializable internal data class ScriptedDuel(val roomCode: String, val seats: List<ScriptedSeat>)
  ```

  `from` is `"server"` or `"client"`. `frame` is the **exact string** `ProtocolCodec.encode` returned
  — never re-parsed, never re-indented, never re-ordered. That is what makes the client's replay a
  replay of the wire rather than of somebody's idea of it.

- Three constants with KDoc:

  ```kotlin
  internal const val SCRIPT_ROOM_CODE: String = "SCRIPT01"
  internal const val SCRIPT_SEED: Long = 0x5C81_000000000001L
  internal val SCRIPT_FORMAT: DuelFormat = DuelFormat(
      startingStack = 1_500,
      blinds = BlindSchedule(listOf(BlindLevel(50, 100), BlindLevel(100, 200)), handsPerLevel = 5),
      endCondition = EndCondition.Freezeout,
  )
  ```

  The end condition is the shipping one (`ADR-0035`). The stack and the ladder are smaller than
  `DuelFormat.DEFAULT`'s for one stated reason: the committed fixture and the client's replay of it
  both grow linearly in frames, and a 10,000-chip duel is several times longer than this proof needs.
  Say that in the KDoc, so the next reader does not "fix" it back to the default.

- One entry point, `internal fun scriptedDuel(seed: Long = SCRIPT_SEED): ScriptedDuel`, which calls
  `playDuel(seed, SCRIPT_FORMAT)` once and derives both seats from that one result.

- **A seat's steps**, in this order and no other:
  1. `ServerMessage.Welcome(deviceId = "device-seat-$viewer", protocolVersion = PROTOCOL_VERSION)`
  2. `ServerMessage.RoomJoined(SCRIPT_ROOM_CODE, viewer)`
  3. then, walking `played.outbound.filter { it.seat == viewer }` in order, one `"server"` step per
     frame — and, **immediately after every `ServerMessage.YourTurn`**, one `"client"` step carrying
     the next of `played.acts.filter { it.seat == viewer }`.

  That placement is a rule, not a guess: `playDuel` answers each `YourTurn` before it asks for the
  next frame, so the *n*th turn addressed to a seat and the *n*th act that seat sent are the same
  decision point. Both handshake frames are constructed here rather than taken from the runner
  because the runner does not produce them — they are still `ProtocolCodec` output, not hand-typed
  JSON.

- **`rivalHoleCards`** is what seat `1 - viewer` was actually holding, hand by hand, taken from that
  rival's *own* `Snapshot` frames: for each `handNumber`, the first non-empty
  `view.seats[1 - viewer].holeCards`. It is the secret the client must be proven not to have shown,
  so it is read from the rival's stream and never from the viewer's.

## Out of scope

- Writing a file, choosing a path, or any Gradle task — `TASK-031203`.
- Any `require` or `check` inside `ScriptedDuel.kt`. The properties the script must have are
  assertions in `ScriptedDuelTest`, not guards in the builder.
- Changing `PlayedDuel.kt`, `SocketDuel.kt` or any existing test.
- A rematch or a presence frame. Neither exists on the wire (`ADR-0044`, `ADR-0045`), so neither can
  appear in a script the server's own encoder produced.

## Tests

`ScriptedDuelTest`, JUnit 5, package `duels.poker.server.duel`, `@Timeout(120)`.

| Test | Proves |
| --- | --- |
| `bothSeatsGetTheirOwnWholeSession` | each seat's `steps` begins `Welcome`, `RoomJoined`, has more than 40 steps, and its last step is a `"server"` step whose frame decodes to a `DuelFinished` |
| `everyTurnIsFollowedByTheActThatAnsweredIt` | in each seat's `steps`, the `"client"` steps number the same as the `YourTurn` frames, every `"client"` step is directly preceded by a `YourTurn`, and each act's `handNumber` and `actionSequence` equal that turn's |
| `theRivalsCardsComeFromTheRivalsOwnFrames` | seat 0's `rivalHoleCards` equals seat 1's own hole cards hand for hand, and vice versa; each entry has exactly two cards; the hand numbers run `1..handsPlayed` with no gap |
| `theScriptedDuelHasAShowdownAndAFoldWin` | for **each** seat, at least one hand whose frames carry a `HandRevealed` naming the rival, and at least one hand whose frames carry no `HandRevealed` at all — both lists named in the failure message |
| `theDuelIsLongEnoughAndSmallEnough` | the duel played more than 5 hands, ended with a non-null winner, and neither seat's `steps` exceeds 1,200 — the ceiling that keeps the committed fixture and the client's replay affordable |
| `theSameSeedWritesTheSameScript` | `scriptedDuel()` twice gives an equal `ScriptedDuel`, byte for byte in every `frame` |

**If `SCRIPT_SEED` fails any of these, change the constant until it passes** — that is the intended
way to satisfy them, exactly as `TASK-021206` chose its `POLICY_SEED`. Record in `SCRIPT_SEED`'s
KDoc the hand count, the winner and the step count per seat that the chosen seed produces.

## Acceptance criteria

- [ ] `ScriptedDuelTest.bothSeatsGetTheirOwnWholeSession` passes
- [ ] `ScriptedDuelTest.everyTurnIsFollowedByTheActThatAnsweredIt` passes
- [ ] `ScriptedDuelTest.theRivalsCardsComeFromTheRivalsOwnFrames` passes
- [ ] `ScriptedDuelTest.theScriptedDuelHasAShowdownAndAFoldWin` passes
- [ ] `ScriptedDuelTest.theDuelIsLongEnoughAndSmallEnough` passes
- [ ] `ScriptedDuelTest.theSameSeedWritesTheSameScript` passes
- [ ] `SCRIPT_SEED`'s KDoc names the hand count, the winner and both step counts it produces
- [ ] `ScriptedDuel.kt` names no `kotlin.random.Random`, no clock, no file and no network, and every
      `frame` string is a `ProtocolCodec.encode` return value assigned without modification
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
