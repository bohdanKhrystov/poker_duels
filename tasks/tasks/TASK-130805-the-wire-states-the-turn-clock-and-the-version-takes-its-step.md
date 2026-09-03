---
schema: 2
id: TASK-130805
title: TurnClock reaches the wire, the pause leaves it, and PROTOCOL_VERSION takes its step
type: task
status: ready
parent: STORY-1308
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 35
atomic:
  - ProtocolVersionLedgerTest — a wire shape whose fingerprint no ledger row claims fails it, and a bump with no row fails it too
  - ProtocolDocumentationTest — every ServerMessage owes a row, every ProtocolError owes a bullet, and the document states the version
  - ':poker-server:verifyProtocolTypes' — protocol.gen.ts is byte-compared against what the descriptors emit
  - ':poker-server:verifyDuelScript' — scripted-duel.gen.json is byte-compared against what the same tree produces
  - the Kotlin compiler — a removed enum entry, a removed field and a new sealed variant redden call sites and two exhaustive when expressions in one step
  - ktlint standard:no-unused-imports — an import left behind by a deleted branch fails the check task
  - tsc — TS1360 on the frames.ts satisfies table, TS2322 on the ProtocolVersion alias, TS2339/TS2353/TS2367/TS2741 on the removed field
  - ServerMessageHandshakeTest.theErrorSetIsExactlyWhatIsDeclared — a hard-coded golden list of every ProtocolError name
  - ProtocolJsonTest — the server side's single hard-coded protocol-version literal (ADR-0069 §4)
  - prettier --check — a file this ticket edits must still be formatted
labels: [server, client, protocol, wire, atomic]
depends_on: [TASK-130804]
verify:
  - ./gradlew check -PrequireDocker=true
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - sh -c 'test -f web-client/src/protocol/protocol.gen.ts && grep -q "\"TurnClock\"" web-client/src/protocol/protocol.gen.ts'
  - sh -c '! grep -rq "DUEL_PAUSED" poker-server/src web-client/src docs/protocol.md'
  - sh -c '! grep -rq "graceRemainingMillis" poker-server/src web-client/src docs/protocol.md'
  - sh -c 'test "$(grep -cE "^\| [0-9]+ \| " docs/protocol-versions.md)" -eq 5'
  - sh -c 'test ! -e poker-server/src/test/kotlin/duels/poker/server/room/RoomPausedTest.kt'
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.protocol.PresenceFramesTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==6 else 1)"
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.protocol.ServerMessageHandshakeTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==5 else 1)"
  - python3 -c "import xml.etree.ElementTree as E,sys;n=int(E.parse('poker-server/build/test-results/test/TEST-duels.poker.server.DuelSocketDisconnectTest.xml').getroot().get('tests'));print(n);sys.exit(0 if n==11 else 1)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx 2>&1 | grep -qE "Tests +74 passed \(74\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/store/duel-state.test.ts 2>&1 | grep -qE "Tests +75 passed \(75\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/PresenceNotice.test.tsx 2>&1 | grep -qE "Tests +4 passed \(4\)"
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent test -- src/table/ActionBar.test.tsx 2>&1 | grep -qE "Tests +35 passed \(35\)"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ServerMessage.TurnClock` exists on the wire, `OpponentPresence.graceRemainingMillis` and
`ProtocolError.DUEL_PAUSED` do not, the duel no longer refuses a present player's action, and
`PROTOCOL_VERSION` takes one step with its ledger row claimed.

## Why this is `atomic:`, and how the count was reached

**Sized by `ADR-0070`'s probe, run to green on 2026-09-03** against `develop` at `360bcacf`, on
this machine, with Docker (Colima, Engine 29.5.2) up so `-PrequireDocker=true` skipped nothing.
The stub was the four declarations `ADR-0113` §9 names — the new variant **with its five fields**,
the removal of `OpponentPresence.graceRemainingMillis`, the removal of the `ProtocolError`
**enum entry**, and the moved constant — applied together in one tree. The loop ran **seven**
Gradle rounds and **eleven** client rounds; each red run named a prefix and nothing more, and two
paths (`web-client/src/e2e/scripted-duel.gen.json` and `web-client/src/table/PresenceNotice.tsx`)
appeared only after an earlier failure had been cleared. `./gradlew check -PrequireDocker=true`
then exited 0, and `npm ci && npm run check && npm run build` in `web-client/` exited 0. The
thirty-three rows that run produced are the first thirty-three below.

**Two more were added by the driver before dispatch, and the gap is worth naming.** The probe
loop ran the *build* gates — `./gradlew check` and `npm ci && npm run check && npm run build` —
until they exited 0. It never ran this ticket's own **refusal gates**, and
`! grep -rq "graceRemainingMillis" poker-server/src web-client/src docs/protocol.md` exits **1**
on `develop` because `account/sign-in.ts` and `account/sign-out.ts` cite the field in KDoc. Prose
compiles, so no build round could ever have named them. By `ADR-0070`'s own definition — a file
is in the blast radius exactly when some merged gate exits non-zero until it changes — both are
in it, and the count is **thirty-five**. A probe is only as complete as the gate set it runs:
green on the build is not green on the block.

**This number is a fact about this ticket and about nothing else** (`ADR-0070` §5).

## Files

| File | Action | Why it cannot be fewer |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt` | modify | The wire change itself: the variant, and the field's removal |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/ProtocolError.kt` | modify | The enum entry's removal; an entry no server can send must not stay in a set branched on exhaustively |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/Protocol.kt` | modify | `ProtocolVersionLedgerTest` rule 4 — a moved wire shape with no bump fails |
| `docs/protocol-versions.md` | modify | `ProtocolVersionLedgerTest.theLastRowsVersionEqualsProtocolVersion` and `…FingerprintEqualsTheComputedFingerprint` |
| `docs/protocol.md` | modify | `ProtocolDocumentationTest.everyServerMessageHasARowSayingServerToClient`, `theDocumentStatesTheCurrentProtocolVersion`, `theDocumentListsEveryProtocolError` |
| `poker-server/src/main/kotlin/duels/poker/server/room/Room.kt` | modify | Kotlin compiler: `DUEL_PAUSED` unresolved in `act`, `graceRemainingMillis` unknown in `presenceOf`; then ktlint `no-unused-imports` on the `Addressed` import the deleted branch left behind |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPausedTest.kt` | delete | Kotlin compiler: every test in the file names `DUEL_PAUSED`; the file's whole subject is the refusal being removed |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify | Kotlin compiler: `DUEL_PAUSED` ×2 and `graceRemainingMillis` ×4 |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketReconnectTest.kt` | modify | Kotlin compiler: `DUEL_PAUSED` |
| `poker-server/src/test/kotlin/duels/poker/server/SeatDeliveryTest.kt` | modify | Kotlin compiler: `OpponentPresence` constructor arity |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify | Kotlin compiler: `'when' expression must be exhaustive. Add the 'is TurnClock' branch` |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketSecrecyTest.kt` | modify | Kotlin compiler: the same exhaustive `when`, in a second file |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/PresenceFramesTest.kt` | modify | Kotlin compiler: five tests construct `OpponentPresence` with the removed argument |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ProtocolJsonTest.kt` | modify | Its own literal is the server side's single version pin (`ADR-0069` §4) |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/ServerMessageHandshakeTest.kt` | modify | `theErrorSetIsExactlyWhatIsDeclared` — the golden list of error names |
| `poker-server/src/test/kotlin/duels/poker/server/room/GraceExpiryTest.kt` | modify | Kotlin compiler: `OpponentPresence(…, null)` ×3 |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomDisconnectTest.kt` | modify | Kotlin compiler: `OpponentPresence(…, millis)` ×4 |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomPresenceProjectionTest.kt` | modify | Kotlin compiler: `OpponentPresence(…, graceRemainingMillis = …)` ×6 |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomResumeTest.kt` | modify | Kotlin compiler: `OpponentPresence(…, graceRemainingMillis = …)` |
| `web-client/src/protocol/protocol.gen.ts` | regenerate | `:poker-server:verifyProtocolTypes` byte-compares it against the descriptors |
| `web-client/src/e2e/scripted-duel.gen.json` | regenerate | `:poker-server:verifyDuelScript` byte-compares it; it carries the version |
| `web-client/src/protocol/version.ts` | modify | `tsc` TS2322 — the generated `ProtocolVersion` alias is a different literal |
| `web-client/src/protocol/frames.ts` | modify | `tsc` TS1360 — `satisfies Record<ServerMessage["type"], true>` demands the new key |
| `web-client/src/store/duel-state.ts` | modify | `tsc` TS2339 on `message.graceRemainingMillis`; the store's own field then has no source |
| `web-client/src/store/duel-state.test.ts` | modify | `tsc` TS2353/TS2322 ×27, then TS2339 ×11 once the store field goes |
| `web-client/src/lobby/Lobby.tsx` | modify | `tsc` TS2339 on `state.graceRemainingMillis`, then TS2741 on the prop it stops passing |
| `web-client/src/lobby/Lobby.test.tsx` | modify | `tsc` TS2353 ×10 and TS2322; then six countdown tests fail behaviourally |
| `web-client/src/lobby/presence-copy.test.tsx` | modify | `tsc` TS2353 ×3 |
| `web-client/src/result/RematchControl.test.tsx` | modify | `tsc` TS2322 on the removed `ProtocolError` value |
| `web-client/src/table/ActionBar.tsx` | modify | `tsc` TS2367 — comparing `ProtocolError` with a value that no longer exists |
| `web-client/src/table/ActionBar.test.tsx` | modify | `tsc` TS2322, then the paused-copy test fails behaviourally |
| `web-client/src/table/PresenceNotice.tsx` | modify | `tsc` TS2741 — `Lobby.tsx` can no longer supply the required prop |
| `web-client/src/table/PresenceNotice.test.tsx` | modify | `tsc` TS2322 ×8, then four countdown tests fail behaviourally, then `prettier --check` |
| `web-client/src/account/sign-in.ts` | modify | the ticket's own refusal gate — `! grep -rq "graceRemainingMillis" … web-client/src` — exits 1 while this KDoc cites the removed field |
| `web-client/src/account/sign-out.ts` | modify | the same refusal gate, same cause: a KDoc naming `graceRemainingMillis` |

## Scope

- **The frame.** `ServerMessage.TurnClock(seat, handNumber, actionSequence, turnRemainingMillis,
  bankRemainingMillis)` exactly as `ADR-0113` §1 declares it, with `init` requiring
  `seat in 0..1`, `turnRemainingMillis >= 0`, `bankRemainingMillis.size == 2` and every bank
  `>= 0`. `@SerialName("TurnClock")`, per `ADR-0071`.
- **The removals.** `OpponentPresence` loses `graceRemainingMillis` and both of its `require`
  blocks; `ProtocolError` loses `DUEL_PAUSED`; `Room.act` loses the `isPaused` branch that built
  the refusal — so an action sent while the rival's socket is down is **applied**.
- **The bump.** Rebase on `develop` immediately before this commit, move `PROTOCOL_VERSION`,
  run `./gradlew :poker-server:generateProtocolTypes` and `:poker-server:generateDuelScript`, then
  run `ProtocolVersionLedgerTest` **expecting red** and paste the row its failure message hands you
  into `docs/protocol-versions.md`, claimed by `STORY-1308`. Neither the number nor the fingerprint
  is written anywhere in advance (`ADR-0047` §6) — read both off the failure.
- **`docs/protocol.md`**: the version line, `OpponentPresence`'s payload column, a new `TurnClock`
  row saying `server → client`, and the `DUEL_PAUSED` bullet removed from the error list.
- **The client stores nothing new here.** `frames.ts` gains the key so the union is proved; the
  store gains no case. `TASK-130811` is where a `TurnClock` reaches `duel-state.ts`.

## Out of scope

- **Sending the frame.** Nothing emits a `TurnClock` after this ticket — `TASK-130806`,
  `TASK-130807` and `TASK-130809` do.
- **`Room.isPaused`, `gracePeriods`, `expireGrace` and `disconnectGraceMillis`.** They survive this
  ticket: the grace window still runs, still latches `ABSENT` and still folds. `TASK-130810`
  retires them. Only the **refusal** goes here.
- **Renaming `web-client/src/table/presence-countdown.ts` to `countdown.ts`** (`ADR-0113` §7). No
  gate holds it, so it is not this ticket's (`ADR-0069` §5's precedent); `STORY-1309` takes it with
  the countdown it moves to the clock. After this ticket the module is imported only by its own
  test, which is a stated intermediate and not a defect.
- **`PresenceNotice.test.tsx`'s two surviving countdown-shaped test names.** They pass because the
  countdown is gone; renaming them belongs with `STORY-1309`'s move.
- `docs/test-plan.md` — `TASK-130812`.

## Tests

**`PresenceFramesTest`** — 8 today, **6** after. Six `graceRemainingMillis` tests go —
`awayCarriesARemainingDuration`, `awayWithZeroRemainingIsLegal`, `awayWithoutARemainingIsRefused`,
`presentWithARemainingIsRefused`, `aNegativeRemainingIsRefused` and `presentAndAbsentCarryNothing`,
every one of them about a field the wire no longer has — and four are added. Both `ActedForAbsent`
tests stand unchanged.

| Test | Proves |
| --- | --- |
| `awayEncodesAndDecodes` | `OpponentPresence(AWAY)` round-trips through `protocolJson` with no second field |
| `aTurnClockNamesASeatAtTheTable` | `TurnClock(seat = 2, …)` and `seat = -1` both throw |
| `aTurnClockRefusesANegativeDuration` | `turnRemainingMillis = -1`, and a bank of `-1`, each throw |
| `aTurnClockNamesBothBanks` | A one-entry and a three-entry `bankRemainingMillis` each throw; a two-entry one constructs and round-trips |

**`ServerMessageHandshakeTest`** — 5, unchanged in count. `theErrorSetIsExactlyWhatIsDeclared`'s
golden list loses one string; **the expectation is updated, never derived from the enum**
(`ADR-0070` §4).

**`ProtocolJsonTest`** — 3, unchanged in count. The version test's method name and its literal both
move to the number the bump reached.

**`DuelSocketDisconnectTest`** — 13 today, **11** after. `anActAfterTheCountdownWouldHaveEndedIsStillRefused`
and `theRoomIsStillPausedAfterTheWindowHasElapsed` are removed — both assert a pause this ticket
deletes. The remaining refusal test is **replaced by its inverse**:

| Test | Proves |
| --- | --- |
| `anActIsAppliedWhileTheRivalsSocketIsDown` | The guest closes, the host acts, and the host receives a `Snapshot` and **no** `Failure` — the runner is not the one it was before |

**Client counts, measured, per file:** `Lobby.test.tsx` 80 → **74**, `PresenceNotice.test.tsx`
8 → **4**, `ActionBar.test.tsx` 36 → **35**, `duel-state.test.ts` **75** (unchanged — no test is
removed there; the `graceRemainingMillis` assertions inside them are). The named removals are:

- `Lobby.test.tsx`: *shows the presence beside the table it is about*, *explains a paused action
  with the presence it already holds*, *starts a second window fresh, though it carries the same
  remaining*, *the countdown reaching zero sends nothing and changes nothing*, *a window with
  nothing left of it renders as waiting*, *renders the pause a resume came back to*.
- `PresenceNotice.test.tsx`: *says the duel is paused and starts from the number the frame
  carried*, *counts down as time passes*, *holds at zero, and says nothing new there*, *the
  countdown carries the numeral shape ADR-0046 fixes*.
- `ActionBar.test.tsx`: *says a paused duel did not apply the action*.

No other assertion in any of those four files changes, and **no assertion is weakened**: every
removal is of a test whose subject — the away countdown, or the pause's copy — leaves the product
with the field that fed it.

## Acceptance criteria

- [ ] `PresenceFramesTest.aTurnClockNamesASeatAtTheTable`, `…RefusesANegativeDuration` and
      `…NamesBothBanks` pass, and `PresenceFramesTest` reports exactly **6** tests
- [ ] `ServerMessageHandshakeTest` reports exactly **5** tests and is green with one fewer name in
      its golden list
- [ ] `DuelSocketDisconnectTest.anActIsAppliedWhileTheRivalsSocketIsDown` passes, and the class
      reports exactly **11** tests
- [ ] `Lobby.test.tsx` reports **74**, `duel-state.test.ts` **75**, `PresenceNotice.test.tsx` **4**
      and `ActionBar.test.tsx` **35**
- [ ] `DUEL_PAUSED` appears nowhere under `poker-server/src`, `web-client/src` or in
      `docs/protocol.md`
- [ ] `graceRemainingMillis` appears nowhere under `poker-server/src`, `web-client/src` or in
      `docs/protocol.md`
- [ ] `poker-server/src/test/kotlin/duels/poker/server/room/RoomPausedTest.kt` does not exist
- [ ] `web-client/src/protocol/protocol.gen.ts` exists and names `"TurnClock"`
- [ ] `docs/protocol-versions.md` holds exactly **5** version rows — one more than it holds today,
      appended, with no earlier row rewritten
- [ ] `./gradlew check -PrequireDocker=true` exits 0
- [ ] `npm ci`, `npm run check` and `npm run build` in `web-client/` each exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
