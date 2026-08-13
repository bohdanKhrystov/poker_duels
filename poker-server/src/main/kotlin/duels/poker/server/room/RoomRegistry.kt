package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.DuelStep
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.SecureHandSeedSource
import duels.poker.server.duel.startDuel
import duels.poker.server.session.PlayerId
import duels.poker.server.time.ServerClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
     * A seated room also starts the duel: [startDuel] draws its opening hand's seed from [seeds]
     * and the resulting runner is attached before the room is written back, so a room a caller can
     * observe as [RoomState.PLAYING] always already carries its runner — inside the same critical
     * section as the seating itself, for the same reason seating is.
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
                when (val result = room.join(player, clock.nowMillis())) {
                    is JoinResult.Seated -> {
                        val seated = withFreshRunner(result.room)
                        Pair(seated, JoinResult.Seated(seated))
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
     * Offer a rematch on this finished room.
     *
     * Applies [Room.offerRematch] under that room's mutex. The offer, decision and any write-back
     * happen inside the one critical section so that two concurrent offers on the same room are
     * serialized and both see a consistent view of what has been offered.
     *
     * An agreed rematch starts a fresh duel exactly as [join] does: a new runner, seeded from
     * [seeds], replaces whatever runner the just-finished duel left behind, attached before the
     * room is written back.
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
                when (val result = room.offerRematch(player, clock.nowMillis())) {
                    is RematchResult.Offered -> Pair(result.room, result)
                    is RematchResult.Agreed -> {
                        val agreed = withFreshRunner(result.room)
                        Pair(agreed, RematchResult.Agreed(agreed))
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
    public suspend fun act(code: RoomCode, step: (Room) -> DuelStep?): DuelStep? {
        var finishedSeats: List<PlayerId>? = null
        var finishedDuelId: UUID? = null
        val result = mutate(
            code,
            absent = { null },
            block = { room ->
                val duelStep = step(room) ?: return@mutate Pair(null, null)
                val newRoom = if (duelStep.runner.outcome != null) {
                    val duelId = checkNotNull(room.duelId) { "a PLAYING room always carries its duel id" }
                    finishedSeats = listOf(room.host, checkNotNull(room.guest) { "a PLAYING room always has a guest" })
                    finishedDuelId = duelId
                    // Claimed here, under this room's own mutex, in the same critical section as
                    // the write-back to FINISHED below it — so no caller of `offerRematch` can
                    // ever observe a FINISHED room whose recording claim is not yet visible.
                    recording[code] = duelId
                    room.finish(clock.nowMillis()).copy(runner = duelStep.runner)
                } else {
                    room.copy(runner = duelStep.runner, lastActivityAt = clock.nowMillis())
                }
                Pair(newRoom, duelStep)
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
     * Start a fresh duel for [room] and attach it, drawing the opening hand's seed from [seeds]
     * and minting the duel's stable id.
     *
     * Shared by [join] and [offerRematch]: both seat a room into [RoomState.PLAYING] with a fresh
     * [duels.poker.server.duel.MatchState] and both need the runner that plays it, drawn under
     * the same lock as the seating so the two never disagree about which duel is live. [Room.duelId]
     * is minted here, once, for the same reason: every later attempt to record this duel — including
     * a retried finishing frame — reads it back off the room rather than inventing a fresh one, so a
     * retry and its original attempt agree on the id a persistence layer keys idempotency on.
     *
     * @param room The room whose match was just (re)started; its [Room.format] and
     *   [Room.openingButtonSeat] configure the new duel.
     * @return [room] with a freshly started [Room.runner] and a freshly minted [Room.duelId] attached.
     */
    private fun withFreshRunner(room: Room): Room {
        val started = startDuel(room.format, room.openingButtonSeat, seeds.newHandSeed())
        return room.copy(runner = started.runner, duelId = UUID.randomUUID())
    }

    /**
     * Remove every room that has been idle past its state's configured limit.
     *
     * `now` is read once from [clock], so every room in this pass is judged against the same
     * instant. A room is reaped when:
     * - [RoomState.WAITING]: `now - lastActivityAt >= timeouts.waitingMillis`;
     * - [RoomState.FINISHED] or [RoomState.ABANDONED]: `now - lastActivityAt >= timeouts.finishedMillis`;
     * - [RoomState.PLAYING]: never, however idle. A silent live duel is `ADR-0013`'s grace period,
     *   which ends by calling [abandon] — that is what makes the room reapable by the rule above,
     *   rather than this method carrying a second timer for the same room.
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
