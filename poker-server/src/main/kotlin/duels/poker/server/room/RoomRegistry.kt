package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.duel.Addressed
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.DuelStep
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.SecureHandSeedSource
import duels.poker.server.duel.resumeFrames
import duels.poker.server.duel.startDuel
import duels.poker.server.session.PlayerId
import duels.poker.server.time.ServerClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A concurrent registry of live rooms, keyed by [RoomCode].
 *
 * Each room lives behind its own [kotlinx.coroutines.sync.Mutex]: this is the single-writer rule
 * of `STORY-0206`, so that a later frame taking one room's mutex around a read-modify-write never
 * races another frame mutating the same room. `ADR-0016` answers `DEC-013`: the mutex stays, and
 * a room with a live duel is serialised exactly as a room without one — no channel-fed actor.
 *
 * The registry knows no JSON, no `WebSocketSession`, no `ProtocolError` — nothing here reaches
 * outside the engine and the room model. It equally knows nothing of the wire protocol carrying
 * a duel action in: [act] takes a function computing the next step from a [Room], leaving the
 * inbound frame's type to the caller.
 *
 * @param codes The source of new, unique-among-live-rooms codes.
 * @param clock The clock used to stamp a newly opened room's activity.
 * @param timeouts The idle limits a later ticket reaps rooms against. Declared now so that a
 *   constructor growing a parameter later does not break every test that already built a
 *   registry.
 * @param seeds The source a newly started or rematched duel draws its opening hand's seed from.
 * @param sink Where a finished duel is recorded. Called outside every room's lock (`ADR-0016`):
 *   nothing under the lock does I/O, so a slow store never stalls another action in this room.
 */
public class RoomRegistry(
    private val codes: RoomCodeSource,
    private val clock: ServerClock,
    private val timeouts: RoomTimeouts = RoomTimeouts.DEFAULT,
    private val seeds: HandSeedSource = SecureHandSeedSource(),
    private val sink: DuelResultSink = DuelResultSink { },
) {
    private val rooms = ConcurrentHashMap<RoomCode, Holder>()

    /**
     * Reports a single room's failure inside a sweep so it can be logged without ending the
     * batch. Not a `WebSocketSession` or a `ProtocolError`: this is a plain diagnostic, the one
     * exception this class makes to knowing nothing outside the engine and the room model, and it
     * exists only because [expireTurnClocks] must not let one room's exception silence every
     * other room already committed to in the same pass.
     */
    private val logger = LoggerFactory.getLogger(RoomRegistry::class.java)

    /**
     * The duel id [act] is currently trying to hand to [sink], keyed by room, for exactly as long
     * as that attempt is outstanding.
     *
     * An entry is written inside the same [mutate] critical section that writes a room's
     * [RoomState.FINISHED] back — so the claim and the write-back are never observably out of
     * step — and removed once [DuelResultSink.record] has returned, either normally or by
     * throwing. [offerRematch] reads this, still under the room's own mutex, to refuse a rematch
     * while the previous duel's result is still in flight: without it, a rematch agreed in that
     * window would call [withFreshRunner] and overwrite [Room.runner] and [Room.duelId] out from
     * under a [sink] call that might still fail and need the very frame that finished the duel
     * replayed. Keyed by duel id rather than presence alone so that a stale entry left behind by
     * one duel can never suppress a rematch for a later, unrelated one hosted by the same room.
     */
    private val recording = ConcurrentHashMap<RoomCode, UUID>()

    /**
     * Open a fresh, [RoomState.WAITING] room for [host], under a code no other live room holds.
     *
     * Mints a code from [codes] and stores the new room with `putIfAbsent`, which is what makes
     * the code unique: a `containsKey` check followed by a `put` would race another `create`
     * minting the same code. A non-null result from `putIfAbsent` means the code collided with a
     * room already live; the mint is retried up to [MAX_CODE_ATTEMPTS] times before giving up.
     *
     * @param host The player opening the room.
     * @param format The duel's configuration.
     * @return The newly created, stored room.
     * @throws IllegalStateException if [MAX_CODE_ATTEMPTS] consecutive codes all collided with a
     *   live room.
     */
    public fun create(host: PlayerId, format: DuelFormat = DuelFormat.DEFAULT): Room {
        repeat(MAX_CODE_ATTEMPTS) {
            val code = codes.newRoomCode()
            val room = Room.open(code, host, format, clock.nowMillis())
            if (rooms.putIfAbsent(code, Holder(room)) == null) {
                return room
            }
        }
        throw IllegalStateException("failed to mint a unique room code after $MAX_CODE_ATTEMPTS attempts")
    }

    /**
     * Look up a room by its code.
     *
     * A non-suspending snapshot read: it reports the room as it stood at the moment of the call,
     * taking no lock.
     *
     * @param code The room code to look up.
     * @return The room, or `null` if no room with this code is registered.
     */
    public fun get(code: RoomCode): Room? = rooms[code]?.room

    /**
     * Seat [player] into the room at [code], applying [Room.join] under that room's mutex.
     *
     * The lookup, the call to [Room.join] and the write-back of a [JoinResult.Seated] room all
     * happen inside the one critical section: reading [Holder.room] outside the lock would let two
     * simultaneous joins both decide against the same stale room and seat two guests in one seat.
     * The holder is re-checked against the map once the lock is held, so a room reaped between the
     * lookup and the lock refuses with [RoomRefusal.UNKNOWN_ROOM] instead of seating a player into
     * a room nobody can find.
     *
     * A seated room also starts the duel: [withFreshRunner] draws its opening hand's seed from
     * [seeds], refills both banks and clocks the opening decision, and the resulting runner is
     * attached before the room is written back, so a room a caller can observe as
     * [RoomState.PLAYING] always already carries its runner and a live deadline — inside the same
     * critical section as the seating itself, for the same reason seating is. The frames that
     * opening hand produced, followed by the `TurnClock` frames its fresh deadline owes both
     * seats, travel out on the [JoinResult.Seated] this method returns, in
     * [JoinResult.Seated.outbound].
     *
     * @param code The room to join.
     * @param player The player attempting to join.
     * @return The result of [Room.join]; a [JoinResult.Refused] room is left exactly as it was.
     */
    public suspend fun join(code: RoomCode, player: PlayerId): JoinResult {
        return mutate(
            code,
            absent = { JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM) },
            block = { room ->
                val now = clock.nowMillis()
                when (val result = room.join(player, now)) {
                    is JoinResult.Seated -> {
                        val (seated, outbound) = withFreshRunner(result.room, now)
                        Pair(seated, JoinResult.Seated(seated, outbound))
                    }
                    is JoinResult.Refused -> Pair(null, result)
                }
            },
        )
    }

    /**
     * Finish a [RoomState.PLAYING] room when the duel concludes.
     *
     * Applies [Room.finish] under that room's mutex, storing the resulting [RoomState.FINISHED]
     * room. The finish operation and the write-back happen inside the one critical section so that
     * a concurrent finish request on the same room always finds the authoritative state.
     *
     * **Test-only affordance.** Production code never calls this method; [act] finishes rooms
     * itself as part of detecting that a duel has ended. Tests call this directly to reach
     * [RoomState.FINISHED] for edge case coverage — specifically, `RoomRegistryLifecycleTest`
     * exercises the failure modes: finishing a [RoomState.WAITING] room (which throws), and
     * finishing a non-existent code (which returns null). These paths cannot be tested through
     * [act] since [act] only works on rooms that exist and are [RoomState.PLAYING].
     *
     * @param code The room to finish.
     * @return The room after the transition, or `null` for a code with no live room.
     * @throws IllegalStateException if the room is not [RoomState.PLAYING].
     */
    public suspend fun finish(code: RoomCode): Room? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                val finished = room.finish(clock.nowMillis())
                Pair(finished, finished)
            },
        )
    }

    /**
     * Abandon this room when its players are gone or have given up.
     *
     * Applies [Room.abandon] under that room's mutex, storing the resulting [RoomState.ABANDONED]
     * room. The abandon operation and the write-back happen inside the one critical section so that
     * a concurrent abandon request on the same room always finds the authoritative state.
     *
     * @param code The room to abandon.
     * @return The room after the transition, or `null` for a code with no live room.
     */
    public suspend fun abandon(code: RoomCode): Room? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                val abandoned = room.abandon(clock.nowMillis())
                Pair(abandoned, abandoned)
            },
        )
    }

    /**
     * Record that [player]'s connection to this room is gone, starting their seat's disconnect
     * grace window (`ADR-0013`).
     *
     * Applies [Room.disconnect] under that room's mutex, storing and returning the resulting
     * room. The deadline is computed here, and nowhere else: `clock.nowMillis() +
     * timeouts.disconnectGraceMillis` is the one call site that turns the configured window into
     * an absolute instant — `Room` reads no clock of its own (`TASK-020805`), and the caller
     * reporting the drop knows nothing of the configured window, so neither of them could compute
     * it instead.
     *
     * The same `now` also builds the frame the seat that stayed is owed: `disconnected.presenceOf(
     * seat, now)`, addressed to `1 - seat`, reads `AWAY` with however much of the window remains —
     * which, read from the very instant the deadline above was computed from, is always the
     * configured window exactly (`ADR-0028` §2, §5). Nothing is built when the other seat holds no
     * player: a [RoomState.WAITING] room has no guest, so a host's drop there always answers an
     * empty [Disconnection.outbound] — `ADR-0028` §5's rule, not an optimisation.
     *
     * [Room.seatOf] decides which seat [player] holds; a player this room has not seated leaves
     * the room untouched and this returns `null`, the same idiom [offerRematch] uses for a
     * refusal that carries no new room. A [RoomState.WAITING] room may take a disconnect too —
     * seat 0 is always seated, so the call is legal even before a guest has joined; nothing acts
     * on it yet, and the existing `WAITING` idle timeout still reaps such a room.
     *
     * @param code The room [player] disconnected from.
     * @param player The player whose connection is gone.
     * @return A [Disconnection] naming the room after the transition (with [player]'s seat
     *   counting down in [Room.gracePeriods]) and the frames the call produced; or `null` for a
     *   code with no live room, or for a player this room has not seated.
     */
    public suspend fun disconnect(code: RoomCode, player: PlayerId): Disconnection? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                val seat = room.seatOf(player) ?: return@mutate Pair(null, null)
                val now = clock.nowMillis()
                val disconnected = room.disconnect(seat, now + timeouts.disconnectGraceMillis)
                val otherSeat = 1 - seat
                val outbound = if (otherSeat == 0 || disconnected.guest != null) {
                    listOf(Addressed(otherSeat, disconnected.presenceOf(seat, now)))
                } else {
                    emptyList()
                }
                Pair(disconnected, Disconnection(disconnected, outbound))
            },
        )
    }

    /**
     * Return a resumed session for [player], if [player] holds a seat with something to resume
     * in this room.
     *
     * Applies to a [RoomState.PLAYING] or [RoomState.FINISHED] room only: a [RoomState.WAITING]
     * room has no duel to resume — a self-rejoin there already gets [RoomRefusal.ALREADY_SEATED]
     * from [join], and this method must not race that behaviour — and a [RoomState.ABANDONED]
     * room is dead. Both answer `null`, the same idiom [disconnect] uses for a caller this room
     * has nothing to give.
     *
     * [Room.seatOf] is the only credential checked, and it is checked before anything else: a
     * caller this room has not seated changes nothing at all, not even the other seat's
     * disconnect grace window (`ADR-0013`) — the write-back below only ever names the seat
     * [player] holds, so a stranger's call, or a call for the wrong room's player, leaves both
     * seats exactly as it found them. The lookup, the seat decision, the write-back and the
     * frames handed back all happen inside the one [mutate] critical section, for the reason
     * `TASK-020725` gives: a room state decided outside that lock could describe a room that, by
     * the time this method returns, never existed.
     *
     * A returning seat's window is cleared with [Room.reconnect] and the room is
     * [touched][Room.touch] at [clock]'s current instant: a [RoomState.FINISHED] room whose
     * player just came back is not idle, and must not be reaped out from under them a moment
     * later. The frames owed to [seat] are read off [resumeFrames], which asks the projection
     * layer what that seat is entitled to see right now rather than rebuilding that view here.
     *
     * The same instant also restates [seat]'s live deadline: [turnClockFrame] reads
     * [Room.turnDeadline] off the room as it stands after the write-back above and, when it
     * names a live decision, appends one `TurnClock` addressed to [seat] after [resumeFrames]
     * and the presence frames — nothing when it does not, since a [RoomState.FINISHED] room, or
     * a duel paused between hands, has no deadline left to restate (`ADR-0113` §§1, 3). [seat]
     * is told this even when [player] was never away: a reload has no memory of a clock it never
     * rendered, and every state a client can be in should be reachable from one frame
     * (`ADR-0113` §1).
     *
     * @param code The room to resume in.
     * @param player The player asking for their seat back.
     * @return A [Resumption] naming [player]'s seat and the frames it is owed, or `null` if the
     *   room does not exist, [player] holds no seat in it, or there is nothing to resume.
     */
    public suspend fun resume(code: RoomCode, player: PlayerId): Resumption? {
        return mutate(
            code,
            absent = { null },
            block = { room ->
                when (room.state) {
                    RoomState.PLAYING, RoomState.FINISHED -> {
                        val seat = room.seatOf(player) ?: return@mutate Pair(null, null)
                        val runner = room.runner ?: return@mutate Pair(null, null)
                        val otherSeat = 1 - seat
                        val wasAway = seat in room.gracePeriods || seat in room.absentSeats
                        val now = clock.nowMillis()
                        val returned = room.reconnect(seat).touch(now)
                        val presence = buildList {
                            add(Addressed(seat, returned.presenceOf(otherSeat, now)))
                            if (wasAway) add(Addressed(otherSeat, returned.presenceOf(seat, now)))
                        }
                        val outbound = resumeFrames(runner, seat) + presence + turnClockFrame(returned, seat, now)
                        Pair(returned, Resumption(returned, seat, outbound))
                    }
                    RoomState.WAITING, RoomState.ABANDONED -> Pair(null, null)
                }
            },
        )
    }

    /**
     * Offer a rematch on this finished room.
     *
     * Applies [Room.offerRematch] under that room's mutex. The offer, decision and any write-back
     * happen inside the one critical section so that two concurrent offers on the same room are
     * serialized and both see a consistent view of what has been offered.
     *
     * An agreed rematch starts a fresh duel exactly as [join] does: a new runner, seeded from
     * [seeds], replaces whatever runner the just-finished duel left behind, its bank refilled and
     * its opening decision clocked the same way [withFreshRunner] always does — *a rematch is a
     * new duel and a fresh bank* (`ADR-0108` §1) — attached before the room is written back. The
     * frames that opening hand produced, followed by the `TurnClock` frames its fresh deadline
     * owes both seats, travel out on the [RematchResult.Agreed] this method returns, in
     * [RematchResult.Agreed.outbound].
     *
     * Before any of that, this checks [recording]: if the room's current [Room.duelId] is the one
     * [act] is still trying to hand to [sink], the room is treated exactly as [Room.offerRematch]
     * would treat one that is not yet [RoomState.FINISHED] — refused as
     * [RematchRefusal.NOT_FINISHED] — rather than letting [Room.offerRematch] itself decide from
     * a [state][Room.state] that says `FINISHED` but is not yet settled. [Room] stays pure: it has
     * no way to know a recording attempt is outstanding, so this registry decides for it, in the
     * same critical section that decision has to be safe in.
     *
     * @param code The room to offer a rematch on.
     * @param player The player offering the rematch.
     * @return The result of [Room.offerRematch]; a [RematchResult.Offered] or [RematchResult.Agreed]
     *   room is written back, but a [RematchResult.Refused] room is left exactly as it was.
     */
    public suspend fun offerRematch(code: RoomCode, player: PlayerId): RematchResult {
        return mutate(
            code,
            absent = { RematchResult.Refused(RematchRefusal.UNKNOWN_ROOM) },
            block = { room ->
                val outstanding = recording[code]
                if (outstanding != null && outstanding == room.duelId) {
                    return@mutate Pair(null, RematchResult.Refused(RematchRefusal.NOT_FINISHED))
                }
                val now = clock.nowMillis()
                when (val result = room.offerRematch(player, now)) {
                    is RematchResult.Offered -> Pair(result.room, result)
                    is RematchResult.Agreed -> {
                        val (agreed, outbound) = withFreshRunner(result.room, now)
                        Pair(agreed, RematchResult.Agreed(agreed, outbound))
                    }
                    is RematchResult.Refused -> Pair(null, result)
                }
            },
        )
    }

    /**
     * Apply an inbound duel action to the room at [code], through [step]'s call into
     * [Room.act], entirely inside that room's mutex — no two callers can move one duel at once,
     * for the same reason no two callers can move one room's seating at once.
     *
     * [step] takes the current [Room] rather than a message, so this registry needs no knowledge
     * of the inbound frame's type; the caller supplies the pure computation, this method only
     * supplies the lock and the write-back.
     *
     * Before the write-back, [step]'s result restarts the duel's turn clock: [Room.clocked]
     * rederives [Room.turnDeadline] and [Room.timebankRemainingMillis] from wherever [step] left
     * the runner, reading `now` once and under the same lock as the write-back itself, so no
     * reader can ever observe a runner without the deadline that governs it (`ADR-0113` §3). When
     * that leaves a live decision open, a `ServerMessage.TurnClock` addressed to both seats is
     * appended to the very end of [step]'s own outbound — after every `Events`, `Snapshot` and
     * `YourTurn` it already carries — naming the seat, hand and decision the clock now times, and
     * each seat's own remaining bank. A write-back whose runner did not move at all — a stale or
     * illegal frame [step] itself already refused — states nothing new: nothing here restarted
     * for it to state.
     *
     * When [step] returns a step whose runner has finished, the room is moved to
     * [RoomState.FINISHED] inside this call's lock — that write-back is what makes concurrent
     * finishing frames exactly-once: it is the same claim [Room.act] itself already refuses to
     * hand out twice, so a second racer's frame finds a room that is no longer [RoomState.PLAYING]
     * and never reaches [step] a second time for this duel. [DuelResultSink.record] is then called
     * once, outside this method's lock so a slow store never holds up another action on this room
     * (`ADR-0016`).
     *
     * A claim that can never be given back is today's bug: if [DuelResultSink.record] throws, the
     * write-back above already happened, so nothing else would ever call it again for this duel.
     * This method undoes that claim — moving the room from [RoomState.FINISHED] back to
     * [RoomState.PLAYING] via [Room.unfinish] — before letting the exception propagate, so the
     * very frame that finished the duel is accepted again by a later call. A duplicate recording
     * attempt that follows is absorbed by [Room.duelId], which does not change across the retry;
     * a lost result is unrecoverable. Only one of those two mistakes is one this method can undo,
     * which is why it is the one the ordering favors.
     *
     * The same write-back also records the duel's id in [recording], inside the very critical
     * section that flips the room to [RoomState.FINISHED], and this method removes it again once
     * [DuelResultSink.record] has returned — successfully or not. [offerRematch] refuses while
     * that entry stands, which is the fix for a race [unclaim] alone cannot close: a rematch
     * agreed between the write-back above and [DuelResultSink.record] actually returning would
     * otherwise call [withFreshRunner] and overwrite [Room.runner] and [Room.duelId] out from
     * under a recording attempt that might still need the very frame that finished the duel
     * replayed. Claim, then record, then unclaim on failure: never a rematch in between.
     *
     * @param code The room to act in.
     * @param step Computes the next duel step from the room's current state; typically a call to
     *   [Room.act] closing over the inbound frame.
     * @return The resulting step, or `null` if the room does not exist or has nothing to move.
     */
    public suspend fun act(code: RoomCode, step: (Room) -> DuelStep?): DuelStep? =
        actOn(code) { room -> step(room)?.let { TurnGiveUp(room, it) } }

    /**
     * The shared core [act] and [expireTurnClocks] both write through: applies [step] to the room
     * at [code] and restarts its clock exactly as [act]'s own KDoc describes, except the room the
     * restart is based on — [TurnGiveUp.room] — may differ from the room [step] was called with.
     *
     * [act] always hands the two back equal: an ordinary inbound frame never changes anything about
     * a room besides the duel it hosts, so [TurnGiveUp.room] is always the very room [step] read.
     * [expireTurnClocks] is the one caller where that is not true, because giving up a seat's turn
     * can also latch it into [Room.absentSeats] or abandon the room outright, and the write-back has
     * to carry that the same way it already carries the duel's own move — never a second, separate
     * write outside this one (`TASK-130809`).
     *
     * @param code The room to act in.
     * @param step Computes the duel to give up and the room its write-back should restart from.
     * @return The resulting step, or `null` if the room does not exist or has nothing to move.
     */
    private suspend fun actOn(code: RoomCode, step: (Room) -> TurnGiveUp?): DuelStep? {
        var finishedSeats: List<PlayerId>? = null
        var finishedDuelId: UUID? = null
        val result = mutate(
            code,
            absent = { null },
            block = { room ->
                val turnGiveUp = step(room) ?: return@mutate Pair(null, null)
                val duelStep = turnGiveUp.step
                val now = clock.nowMillis()
                val restarted = turnGiveUp.room.clocked(duelStep.runner, now, timeouts.turnMillis)
                val newRoom = if (duelStep.runner.outcome != null) {
                    val duelId = checkNotNull(room.duelId) { "a PLAYING room always carries its duel id" }
                    finishedSeats = listOf(room.host, checkNotNull(room.guest) { "a PLAYING room always has a guest" })
                    finishedDuelId = duelId
                    // Claimed here, under this room's own mutex, in the same critical section as
                    // the write-back to FINISHED below it — so no caller of `offerRematch` can
                    // ever observe a FINISHED room whose recording claim is not yet visible.
                    recording[code] = duelId
                    restarted.finish(now).copy(runner = duelStep.runner)
                } else {
                    restarted.copy(runner = duelStep.runner, lastActivityAt = now)
                }
                // A frame that moved nothing — a stale replay, an action step itself refused —
                // states no new deadline: the recipient already holds whatever was last sent for
                // this exact decision, and restating it would claim a restart that never happened.
                val outbound = if (duelStep.runner === room.runner) {
                    duelStep.outbound
                } else {
                    duelStep.outbound + turnClockFrames(newRoom, now)
                }
                Pair(newRoom, duelStep.copy(outbound = outbound))
            },
        )

        val seats = finishedSeats
        val duelId = finishedDuelId
        if (result != null && seats != null && duelId != null) {
            try {
                sink.record(DuelResult(duelId, checkNotNull(result.runner.outcome), seats, result.runner.log))
            } catch (failure: Throwable) {
                unclaim(code)
                throw failure
            } finally {
                recording.remove(code, duelId)
            }
        }
        return result
    }

    /**
     * Give back the finishing claim [act] took when [DuelResultSink.record] failed to honor it.
     *
     * Moves the room at [code] from [RoomState.FINISHED] back to [RoomState.PLAYING] via
     * [Room.unfinish], under the same [mutate] critical section every other write-back in this
     * class uses. A no-op if the room is no longer [RoomState.FINISHED] — reaped, abandoned, or
     * already recovered by another call — since there is then nothing left to give back.
     *
     * @param code The room whose claim to give back.
     */
    private suspend fun unclaim(code: RoomCode) {
        mutate(
            code,
            absent = {},
            block = { room ->
                if (room.state == RoomState.FINISHED) Pair(room.unfinish(), Unit) else Pair(null, Unit)
            },
        )
    }

    /**
     * Start a fresh duel for [room] and attach it, drawing the opening hand's seed from [seeds],
     * minting the duel's stable id, refilling both banks and clocking the opening decision.
     *
     * Shared by [join] and [offerRematch]: both seat a room into [RoomState.PLAYING] with a fresh
     * [duels.poker.server.duel.MatchState] and both need the runner that plays it, drawn under
     * the same lock as the seating so the two never disagree about which duel is live. [Room.duelId]
     * is minted here, once, for the same reason: every later attempt to record this duel — including
     * a retried finishing frame — reads it back off the room rather than inventing a fresh one, so a
     * retry and its original attempt agree on the id a persistence layer keys idempotency on.
     *
     * The room's bank is refilled before its opening decision is clocked, never after: the bank
     * an opening deadline's `expiresAt` is carved from must already read as full for both seats,
     * or the deadline would be computed against whatever the room's *previous* duel — if any —
     * left behind. [turnClockFrames] then reads the result exactly as `act`'s own write-back
     * does for every later decision (`TASK-130806`), so the `TurnClock` frames appended to
     * [started]'s own outbound are built the one way this registry ever builds one.
     *
     * @param room The room whose match was just (re)started; its [Room.format] and
     *   [Room.openingButtonSeat] configure the new duel.
     * @param now The instant to clock the opening decision from — the same `now` the caller
     *   already used to decide the seating or the rematch this fresh duel follows.
     * @return [room] with a freshly started [Room.runner], a freshly minted [Room.duelId], both
     *   banks refilled and the opening decision's deadline attached, paired with [startDuel]'s
     *   opening hand frames followed by the `TurnClock` frames that deadline owes both seats.
     *   Both [join] and [offerRematch] hand them back to their own callers, on
     *   [JoinResult.Seated.outbound] and [RematchResult.Agreed.outbound] respectively.
     */
    private fun withFreshRunner(room: Room, now: Long): Pair<Room, List<Addressed>> {
        val started = startDuel(room.format, room.openingButtonSeat, seeds.newHandSeed())
        val fresh = room.withFreshClocks(timeouts.timebankMillis)
            .clocked(started.runner, now, timeouts.turnMillis)
            .copy(runner = started.runner, duelId = UUID.randomUUID())
        return Pair(fresh, started.outbound + turnClockFrames(fresh, now))
    }

    /**
     * The frames this write-back owes both seats for [room]'s open decision, if any — empty when
     * [Room.turnDeadline] is `null`, since there is then no live decision to state (`ADR-0113`
     * §§1, 3).
     *
     * [Room.turnClock] builds the frame itself, the same way [Room.presenceOf] already builds
     * one for this registry's own [disconnect] and [resume] — this registry names no protocol
     * type of its own to send one. Both seats are handed the identical value, wrapped once each:
     * the clock is a fact about the decision now open, not a per-recipient view.
     *
     * @param room The room as it stands after the write-back restarted its clock.
     * @param now The same instant the room was restarted with, passed through unchanged so the
     *   frame and the deadline it describes agree.
     * @return Exactly two frames, [Addressed] to seat `0` and seat `1`, or an empty list.
     */
    private fun turnClockFrames(room: Room, now: Long): List<Addressed> {
        val clock = room.turnClock(now, timeouts.timebankMillis) ?: return emptyList()
        return listOf(Addressed(0, clock), Addressed(1, clock))
    }

    /**
     * The single frame [seat] is owed for [room]'s open decision, if any — the mirror of
     * [turnClockFrames] for the one seat [resume] restates a live deadline to, rather than both.
     *
     * Empty when [Room.turnDeadline] names no live decision — a [RoomState.FINISHED] room, or a
     * duel paused between hands — since there is then nothing to restate (`ADR-0113` §§1, 3).
     *
     * @param room The room as it stands after [resume]'s write-back.
     * @param seat The resuming seat, and so the frame's address.
     * @param now The same instant the presence frames beside it already read, passed through
     *   unchanged so the frame and the deadline it describes agree.
     * @return Exactly one frame, [Addressed] to [seat], or an empty list.
     */
    private fun turnClockFrame(room: Room, seat: Int, now: Long): List<Addressed> {
        val liveClock = room.turnClock(now, timeouts.timebankMillis) ?: return emptyList()
        return listOf(Addressed(seat, liveClock))
    }

    /**
     * End every open decision whose turn clock has run out, as of one instant read from [clock].
     *
     * `now` is read once, exactly as [reap] reads its own `now` once, so every room in this pass
     * is judged against the same instant: an enforced expiry can only ever trail the deadline it
     * enforces, never precede it (`ADR-0108` §6).
     *
     * One pass, through [isTurnClockCandidate] and [actOn]: a room is a candidate only once its
     * unlocked state shows a seat still counting down a disconnect grace window (`ADR-0013`), or an
     * open decision whose own deadline needs attention — the same cheap-before-the-lock idiom [reap]
     * uses for [isReapable], re-decided here again once the room's own lock is held, since a room
     * touched between the scan and the lock — reconnected, resumed, joined — may have nothing left
     * to expire by the time this reaches it.
     *
     * A candidate is judged entirely inside [actOn]'s own lock: [Room.expireGrace] latches whatever
     * seat's window has just run out, and [Room.giveUpTurn] decides what the seat on turn owes for
     * it — a played decision, or, when both seats are now gone, the room [RoomState.ABANDONED]
     * instead. [Room.giveUpTurn] is this pass's sole author of what "both gone" means: nothing here
     * repeats that check on its own, because wiring [TurnGiveUp.room] through [actOn]'s write-back —
     * rather than discarding it the way a single-pass [act] call used to — is what lets the wider
     * question [Room.giveUpTurn] asks decide it (`TASK-130809`). A seat can also latch with nothing
     * to fold — the seat that ran out was not on turn — and that alone still counts as one expiry;
     * [actOn] is handed a synthetic, unplayed [TurnGiveUp] for exactly that case.
     *
     * Before the fold's own frames, this prepends `Addressed(otherSeat, room.presenceOf(expiredSeat,
     * now))` — the same `now` read above — for whichever single seat newly latched into
     * [Room.absentSeats] this pass, so the seat that stayed is told `ABSENT` before the frame
     * explaining why (`ADR-0028` §5, §10). Nothing is prepended for a room [Room.giveUpTurn]
     * abandoned instead — there is no other seat left to receive it — nor for a room where no seat
     * newly latched, such as an already-absent seat's fresh decision coming due again.
     *
     * Every candidate's [actOn] call is isolated in its own `try`/`catch`: a room whose call throws
     * is logged and skipped, so one room's failure — a sink outage while its fold finishes a duel is
     * the obvious cause — never costs any other candidate this same pass its own, independent
     * attempt. `ADR-0025`'s "log and retry next tick" picks up a room this call never reached.
     *
     * No new lock: this writes exclusively through [actOn], the same call site every other mutation
     * that moves a duel in this class already uses.
     *
     * @return One [TurnClockExpiry] per candidate this pass changed, carrying that room as it stands
     *   afterward and its outbound frames: the presence frame naming the seat that latched, if any,
     *   followed by the frames the give-up produced. Both are empty for a room [Room.giveUpTurn]
     *   abandoned instead. A room whose call threw is omitted, not retried by this same call.
     */
    public suspend fun expireTurnClocks(): List<TurnClockExpiry> {
        val now = clock.nowMillis()
        val expiries = mutableListOf<TurnClockExpiry>()
        for ((code, holder) in rooms) {
            if (!isTurnClockCandidate(holder.room, now)) continue
            var readRoom: Room? = null
            val step = try {
                actOn(code) { room ->
                    readRoom = room
                    if (!isTurnClockCandidate(room, now)) return@actOn null
                    val expired = room.expireGrace(now)
                    when (val turnGiveUp = expired.giveUpTurn(now, handSeeds)) {
                        // giveUpTurn answers null both for a room with nothing to give up and for
                        // one not PLAYING at all — a WAITING room's host can disconnect before a
                        // guest ever joins (RoomRegistry.disconnect allows it) and so latch here
                        // with no runner to wrap in a step. Nothing downstream reads such a room's
                        // presence — resume refuses a WAITING room outright — so this room is left
                        // for the WAITING idle timeout in reap() rather than forced through a step
                        // shape that assumes a duel exists.
                        null -> expired.runner?.let { runner ->
                            if (expired !== room) TurnGiveUp(expired, DuelStep(runner, emptyList())) else null
                        }
                        else -> turnGiveUp
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                // One room's failure — a sink outage while this fold finished its duel is the
                // obvious cause — must not cost every other candidate this pass its own, unrelated
                // attempt: see the KDoc above for why a later sweep cannot pick this room back up.
                logger.error("expireTurnClocks: expiring the turn clock failed for room {}", code, failure)
                null
            } ?: continue
            val room = checkNotNull(get(code)) { "a room this pass just wrote back must still be registered" }
            val expiredSeat = (room.absentSeats - readRoom!!.absentSeats).singleOrNull()
            val presence = expiredSeat?.let { seat ->
                val otherSeat = 1 - seat
                if (otherSeat == 0 || room.guest != null) {
                    Addressed(otherSeat, room.presenceOf(seat, now))
                } else {
                    null
                }
            }
            expiries.add(TurnClockExpiry(room, listOfNotNull(presence) + step.outbound))
        }
        return expiries
    }

    /**
     * Whether [room], read without its lock, is worth [expireTurnClocks] taking that lock for: a
     * seat still counting down a disconnect grace window (`ADR-0013`) this pass might latch, or an
     * open decision whose own deadline needs [Room.giveUpTurn]'s attention — because [now] has
     * reached it, or because the seat it names already sits in [Room.absentSeats] and so owns no
     * deadline worth waiting out again, only a fresh one [Room.clocked] keeps handing it.
     *
     * @param room The room to judge; called both before [room]'s lock is taken and again once it is.
     * @param now The instant every candidate in the same [expireTurnClocks] pass is judged against.
     */
    private fun isTurnClockCandidate(room: Room, now: Long): Boolean {
        if (room.gracePeriods.isNotEmpty()) return true
        val deadline = room.turnDeadline ?: return false
        return now >= deadline.expiresAt || deadline.seat in room.absentSeats
    }

    /**
     * Remove every room that has been idle past its state's configured limit.
     *
     * `now` is read once from [clock], so every room in this pass is judged against the same
     * instant. A room is reaped when:
     * - [RoomState.WAITING]: `now - lastActivityAt >= timeouts.waitingMillis`;
     * - [RoomState.FINISHED] or [RoomState.ABANDONED]: `now - lastActivityAt >= timeouts.finishedMillis`;
     * - [RoomState.PLAYING]: never, however idle. A silent live duel is `ADR-0013`'s grace period,
     *   which [expireTurnClocks] ends: a seat whose window runs out has its hand folded as an
     *   ordinary action, and the room only becomes [RoomState.ABANDONED] — which is what makes it
     *   reapable by the rule above — once both seats are gone, rather than this method carrying a
     *   second timer for the same room.
     *
     * A candidate is found by an unlocked scan, then re-checked against [Holder.room] after taking
     * that room's mutex, so a room touched between the scan and the lock — joined, finished,
     * abandoned, offered a rematch — survives on its fresh timestamp instead of being removed on a
     * stale one. This is the other half of the `rooms[code] === holder` re-check [mutate] makes.
     *
     * This does not go through [mutate]: [mutate] always writes a room back, and reaping has no
     * room to write back — it removes the entry outright with `ConcurrentHashMap.remove(key,
     * value)`, which only succeeds while the map still holds the exact [Holder] this call locked.
     * Folding that removal into [mutate]'s contract would mean teaching one shared critical section
     * two different shapes of "done"; a second, narrowly-scoped lock here is safer than that.
     *
     * @return The codes of every room removed by this pass, in no particular order.
     */
    public suspend fun reap(): List<RoomCode> {
        val now = clock.nowMillis()
        val removed = mutableListOf<RoomCode>()
        for ((code, holder) in rooms) {
            if (!isReapable(code, holder.room, now)) continue
            holder.mutex.withLock {
                if (isReapable(code, holder.room, now) && rooms.remove(code, holder)) {
                    removed.add(code)
                }
            }
        }
        return removed
    }

    /**
     * Whether [room] has been idle past its state's configured limit, as of [now].
     *
     * A room whose [recording] entry still names its current [Room.duelId] is never reapable,
     * regardless of state or how long past [RoomTimeouts.finishedMillis] its [Room.lastActivityAt]
     * sits: [act]'s call into [DuelResultSink.record] for that duel is still outstanding, so the
     * room is not idle — it is mid-transaction, on a clock this method cannot see by looking at
     * [Room.lastActivityAt] alone. "Finished and idle for five minutes" reads as obviously
     * collectable, but if this pass removed the room anyway, [act]'s eventual `catch` would call
     * [unclaim] against a room [rooms] no longer holds, [mutate]'s `absent` branch would fire as a
     * silent no-op, and the duel's result and both coin awards would be gone for good — the exact
     * loss this ticket exists to prevent, reintroduced through the reaper instead of through the
     * sink. [Room.abandon] can be called on a [RoomState.FINISHED] room directly, so the same guard
     * covers [RoomState.ABANDONED] too: [Room.duelId] survives that transition unchanged.
     *
     * This check reads [recording] while this room's own mutex is held ([reap]'s second call,
     * inside `holder.mutex.withLock`), the same mutex [act] holds while writing that entry
     * alongside the [RoomState.FINISHED] write-back — so that call sees a value consistent with
     * the room state it is judging, not a stale one read before the lock was taken. [reap]'s first
     * call, before the lock, is only ever a cheap early-continue: every room it lets through gets
     * re-checked here, under the lock, before anything is actually removed.
     *
     * If [DuelResultSink.record] never returns — neither success nor failure — this entry never
     * clears and the room never becomes reapable again: a permanent leak. That trade is deliberate,
     * not an oversight: a leaked in-memory room is recoverable (it costs memory, and a restart
     * clears it); a lost duel result is not. This method does not add a timeout on top of
     * [recording] to reclaim that leak — that is a different ticket's decision to make.
     *
     * @param code The room's code, used to look up any outstanding [recording] entry.
     */
    private fun isReapable(code: RoomCode, room: Room, now: Long): Boolean {
        val outstanding = recording[code]
        if (outstanding != null && outstanding == room.duelId) return false
        return when (room.state) {
            RoomState.WAITING -> now - room.lastActivityAt >= timeouts.waitingMillis
            RoomState.FINISHED, RoomState.ABANDONED -> now - room.lastActivityAt >= timeouts.finishedMillis
            RoomState.PLAYING -> false
        }
    }

    /**
     * Look up, lock, mutate and store a room under a single critical section.
     *
     * This enforces the single-writer rule: a read-modify-write against a room always happens
     * entirely inside the room's mutex, never taking a stale copy outside. The holder is
     * re-checked by reference after taking the lock, so a room reaped between the lookup and
     * the lock returns the absent result instead of mutating a room nobody can find.
     *
     * @param code The room code to look up.
     * @param absent A block to call and return if no live room holds this code, or the holder
     *   identity changed while waiting for the lock.
     * @param block A block to call with the current room; it returns a [Pair] of the new room
     *   (or null to leave it untouched) and a value to return.
     * @return The value from [block], or the result of [absent] if the room was not found.
     */
    private suspend fun <T> mutate(
        code: RoomCode,
        absent: () -> T,
        block: (Room) -> Pair<Room?, T>,
    ): T {
        val holder = rooms[code] ?: return absent()
        return holder.mutex.withLock {
            if (rooms[code] !== holder) {
                return@withLock absent()
            }
            val (updatedRoom, result) = block(holder.room)
            if (updatedRoom != null) {
                holder.room = updatedRoom
            }
            result
        }
    }

    /**
     * The seed source that every hand of a duel this registry hosts draws from.
     *
     * A caller moving a duel forward via [act] must use this source rather than one of its own,
     * to ensure every hand of a duel shares the same seed source — the one that opened it.
     */
    public val handSeeds: HandSeedSource get() = seeds

    /** The number of currently registered rooms. */
    public val size: Int
        get() = rooms.size

    /**
     * One room plus the mutex that serialises every read-modify-write against it.
     *
     * `room` is `@Volatile` so a snapshot read in [get] always sees the latest published value
     * without itself taking [mutex].
     */
    private class Holder(@Volatile var room: Room) {
        val mutex = Mutex()
    }

    public companion object {
        /** The number of times [create] retries a colliding code before giving up. */
        public const val MAX_CODE_ATTEMPTS: Int = 10
    }
}
