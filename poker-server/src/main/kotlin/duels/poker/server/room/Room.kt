package duels.poker.server.room

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchState
import duels.poker.server.duel.DuelRunner
import duels.poker.server.duel.DuelStep
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.protocol.Act
import duels.poker.server.session.PlayerId
import java.util.UUID
import duels.poker.server.duel.act as advanceDuel

/**
 * The lifecycle a room moves through, host to finish, and the invariants each state carries.
 */
public enum class RoomState {
    /** Open, waiting for a second player: no guest, no match. */
    WAITING,

    /** A duel is running: both seats are filled and a match is in progress. */
    PLAYING,

    /** The duel has concluded: both seats are still filled, the match's final state is kept. */
    FINISHED,

    /** The room was abandoned; it carries no further invariant beyond seating. */
    ABANDONED,
}

/**
 * A heads-up room: exactly two seats, host in seat 0 and an optional guest in seat 1.
 *
 * The combinations that make no sense — a [RoomState.WAITING] room holding a match, a
 * [RoomState.PLAYING] room with no guest, a host sitting opposite themselves — cannot be
 * constructed: [init] rejects them.
 *
 * @property code the room's join code.
 * @property host the player who opened the room; always seated in seat 0.
 * @property guest the player who joined, if any; always seated in seat 1 when present.
 * @property state the room's current lifecycle state.
 * @property format the duel's configuration.
 * @property match the running or concluded duel's ledger, present only while [state] is
 *   [RoomState.PLAYING] or [RoomState.FINISHED].
 * @property openingButtonSeat the seat that held the button for the room's first duel.
 * @property rematchOffers the players who have offered a rematch; only meaningful once
 *   [state] is [RoomState.FINISHED].
 * @property lastActivityAt the timestamp, in milliseconds, of the room's most recent activity.
 * @property runner the live or finished duel this room hosts, or `null` before one has started.
 *   `RoomRegistry` attaches it once it draws a hand seed to open the duel [join] or
 *   [offerRematch] just agreed to, which is why neither of those pure methods sets it: this is
 *   the same read-modify-write [RoomRegistry.mutate] already performs, and [act] is the read
 *   half of it. It duplicates what [match] already says about the format and the opening
 *   button — a known cost of keeping the duel in one lock and one type rather than a second
 *   structure (`ADR-0016`).
 * @property duelId the stable identity of the duel this room currently hosts or last hosted, or
 *   `null` before one has started. Minted exactly once, alongside [runner], by the same
 *   `RoomRegistry` critical section that draws the opening hand seed — never re-minted on a
 *   retry — so that every attempt to record the same duel, including a retried finishing frame,
 *   carries the same id. That is what lets a persistence layer keyed on this id treat a repeat
 *   attempt as a no-op instead of a second row (`TASK-021009`). It is also how `RoomRegistry`
 *   tells one duel apart from the next when guarding against the rematch race described on
 *   `RoomRegistry.offerRematch`: a rematch changes this field, so a check keyed to "the duel id
 *   that was current when recording started" stops applying the moment a rematch actually begins.
 * @property gracePeriods each seat currently inside its disconnect grace window (`ADR-0013`),
 *   mapped to the instant, in elapsed milliseconds on the same scale as [lastActivityAt], at
 *   which that window runs out — an absolute deadline, never a remaining duration, because a
 *   value that has to be decremented is a value someone has to remember to decrement.
 * @property absentSeats the seats whose grace window has already run out; see [isPaused] for what
 *   distinguishes them from [gracePeriods].
 */
public data class Room(
    val code: RoomCode,
    val host: PlayerId,
    val guest: PlayerId?,
    val state: RoomState,
    val format: DuelFormat,
    val match: MatchState?,
    val openingButtonSeat: Int,
    val rematchOffers: Set<PlayerId>,
    val lastActivityAt: Long,
    val runner: DuelRunner? = null,
    val duelId: UUID? = null,
    val gracePeriods: Map<Int, Long> = emptyMap(),
    val absentSeats: Set<Int> = emptySet(),
) {
    init {
        require(guest == null || guest != host) { "the guest must not be the host" }
        require(state != RoomState.WAITING || (guest == null && match == null)) {
            "a waiting room must have no guest and no match"
        }
        require(
            (state != RoomState.PLAYING && state != RoomState.FINISHED) ||
                (guest != null && match != null),
        ) { "a $state room must have both a guest and a match" }
        require(state == RoomState.FINISHED || rematchOffers.isEmpty()) {
            "rematch offers may only be made in a finished room"
        }
        require(rematchOffers.all { it == host || it == guest }) {
            "rematch offers must come from a seated player"
        }
        require(openingButtonSeat in 0..1) { "openingButtonSeat must be 0 or 1, was $openingButtonSeat" }
        require(lastActivityAt >= 0) { "lastActivityAt must not be negative, was $lastActivityAt" }
        require((runner == null) == (duelId == null)) {
            "duelId must be present exactly when runner is present"
        }
        require((gracePeriods.keys + absentSeats).all { it in 0..1 }) {
            "a seat named in gracePeriods or absentSeats must be 0 or 1"
        }
        require(guest != null || (1 !in gracePeriods && 1 !in absentSeats)) {
            "seat 1 cannot be gone from a room with no guest seated"
        }
        require(gracePeriods.keys.none { it in absentSeats }) {
            "a seat cannot be both inside its grace window and already absent"
        }
        require(gracePeriods.values.all { it >= 0 }) {
            "every grace deadline must not be negative"
        }
    }

    /** The host, plus the guest if seated. */
    public val players: Set<PlayerId>
        get() = if (guest != null) setOf(host, guest) else setOf(host)

    /**
     * Whether this room's duel is paused waiting for a seat to reconnect.
     *
     * This is the asymmetry the whole design rests on: a seat still counting down in
     * [gracePeriods] pauses the duel, because it might still return before its window runs out.
     * A seat whose window has already run out, recorded in [absentSeats] instead, does **not**
     * pause the duel — it has become an ordinary absent player whose hand gets folded rather than
     * a reason to keep waiting. Pausing on it forever is the "hold indefinitely" alternative
     * `ADR-0013` rejected.
     */
    public val isPaused: Boolean get() = gracePeriods.isNotEmpty()

    /**
     * The heads-up seat number of [player]: `0` for the host, `1` for the guest, `null` for
     * anyone else. Matches [MatchState.buttonSeat]'s seat numbering.
     *
     * @param player the player to find.
     * @return the player's seat, or `null` if they are not seated in this room.
     */
    public fun seatOf(player: PlayerId): Int? = when (player) {
        host -> 0
        guest -> 1
        else -> null
    }

    /**
     * Seat [guest] into this room, starting the duel if there is a seat for them.
     *
     * Checked strictly in this order: a dead room refuses as [RoomRefusal.UNKNOWN_ROOM] before
     * anything else, so it is indistinguishable from a room that never existed; a player already
     * seated is refused as [RoomRefusal.ALREADY_SEATED] before fullness is even considered, so a
     * returning player never mistakes their own room for a full one; only then does a genuinely
     * full room refuse as [RoomRefusal.ROOM_FULL].
     *
     * Pure and total: never throws, never mutates. A [JoinResult.Refused] carries no room —
     * the caller already holds the unchanged one.
     *
     * @param guest the player attempting to join.
     * @param now the current time in milliseconds; `Room` reads no clock of its own.
     * @return [JoinResult.Seated] with the duel started, or [JoinResult.Refused] with the reason.
     */
    public fun join(guest: PlayerId, now: Long): JoinResult = when (state) {
        RoomState.FINISHED, RoomState.ABANDONED -> JoinResult.Refused(RoomRefusal.UNKNOWN_ROOM)
        RoomState.PLAYING -> if (seatOf(guest) != null) {
            JoinResult.Refused(RoomRefusal.ALREADY_SEATED)
        } else {
            JoinResult.Refused(RoomRefusal.ROOM_FULL)
        }
        RoomState.WAITING -> if (seatOf(guest) != null) {
            JoinResult.Refused(RoomRefusal.ALREADY_SEATED)
        } else {
            JoinResult.Seated(
                copy(
                    guest = guest,
                    state = RoomState.PLAYING,
                    match = MatchState.start(format, openingButtonSeat),
                    lastActivityAt = now,
                ),
            )
        }
    }

    /**
     * Finish this room when the duel concludes.
     *
     * Transitions a [RoomState.PLAYING] room to [RoomState.FINISHED], keeping the guest,
     * format, match, and opening button seat so that a rematch can be scheduled. Calling this
     * on any room that is not [RoomState.PLAYING] is a server bug and throws
     * [IllegalStateException].
     *
     * @param now the current time in milliseconds; updates [lastActivityAt].
     * @return this room in state [RoomState.FINISHED].
     * @throws IllegalStateException if [state] is not [RoomState.PLAYING].
     */
    public fun finish(now: Long): Room {
        check(state == RoomState.PLAYING) { "can only finish a PLAYING room, not $state" }
        return copy(
            state = RoomState.FINISHED,
            lastActivityAt = now,
        )
    }

    /**
     * Undo [finish] when recording its result failed.
     *
     * Transitions a [RoomState.FINISHED] room back to [RoomState.PLAYING], leaving [runner],
     * [duelId] and every other field untouched — including a [runner] that already carries an
     * outcome. That is deliberate: a room reverted this way still refuses a genuinely new frame
     * the same way a live one would not, but it accepts a replay of the very frame that finished
     * the duel, which is the whole point — a lost result is unrecoverable, so an unrecorded
     * finish must stay replayable rather than stick as [RoomState.FINISHED] forever. Calling this
     * on any room that is not [RoomState.FINISHED] is a server bug and throws
     * [IllegalStateException].
     *
     * @return this room in state [RoomState.PLAYING].
     * @throws IllegalStateException if [state] is not [RoomState.FINISHED].
     */
    public fun unfinish(): Room {
        check(state == RoomState.FINISHED) { "can only unfinish a FINISHED room, not $state" }
        return copy(state = RoomState.PLAYING)
    }

    /**
     * Abandon this room when its players are gone or have given up.
     *
     * Transitions a room from [RoomState.WAITING], [RoomState.PLAYING], or [RoomState.FINISHED]
     * to [RoomState.ABANDONED], clearing any rematch offers. Calling [abandon] on an already
     * [RoomState.ABANDONED] room returns this room unchanged, including its [lastActivityAt] —
     * both players leaving must not reset the reaping clock on a dead room.
     *
     * @param now the current time in milliseconds; updates [lastActivityAt] unless already abandoned.
     * @return this room in state [RoomState.ABANDONED].
     */
    public fun abandon(now: Long): Room {
        return when (state) {
            RoomState.ABANDONED -> this
            RoomState.WAITING, RoomState.PLAYING, RoomState.FINISHED -> copy(
                state = RoomState.ABANDONED,
                rematchOffers = emptySet(),
                lastActivityAt = now,
            )
        }
    }

    /**
     * Offer a rematch on this finished room.
     *
     * Checked strictly in this order: a room that is not [RoomState.FINISHED] refuses as
     * [RematchRefusal.NOT_FINISHED]; a player who is not seated refuses as
     * [RematchRefusal.NOT_A_PLAYER]; a player who already offered refuses as
     * [RematchRefusal.ALREADY_OFFERED] and the recorded offers are left untouched. Otherwise the
     * offer is recorded: if both seated players have now offered, the room agrees a rematch;
     * if only one has, the offer alone changes nothing else.
     *
     * Agreement alternates the button against [openingButtonSeat] — the seat that opened the
     * duel just finished — not against [MatchState.buttonSeat], which has already alternated
     * hand by hand over the course of that duel and by the end says nothing about who opened it.
     *
     * Pure and total: never throws, never mutates. This purity is exactly why the guard against
     * `RoomRegistry`'s rematch race (see `RoomRegistry.offerRematch`) lives in the registry
     * rather than here: this method has no way to know whether some other in-flight call is
     * mid-way through recording the very duel this room just finished, and giving it one would
     * mean threading that knowledge through every other pure `Room` method's tests as well.
     *
     * @param player the player offering the rematch.
     * @param now the current time in milliseconds; `Room` reads no clock of its own.
     * @return [RematchResult.Offered], [RematchResult.Agreed], or [RematchResult.Refused].
     */
    public fun offerRematch(player: PlayerId, now: Long): RematchResult {
        if (state != RoomState.FINISHED) return RematchResult.Refused(RematchRefusal.NOT_FINISHED)
        if (seatOf(player) == null) return RematchResult.Refused(RematchRefusal.NOT_A_PLAYER)
        if (player in rematchOffers) return RematchResult.Refused(RematchRefusal.ALREADY_OFFERED)

        val offers = rematchOffers + player
        return if (offers.containsAll(players)) {
            val nextButtonSeat = 1 - openingButtonSeat
            RematchResult.Agreed(
                copy(
                    state = RoomState.PLAYING,
                    openingButtonSeat = nextButtonSeat,
                    match = MatchState.start(format, nextButtonSeat),
                    rematchOffers = emptySet(),
                    lastActivityAt = now,
                ),
            )
        } else {
            RematchResult.Offered(copy(rematchOffers = offers))
        }
    }

    /**
     * Apply an inbound duel action to this room's live duel.
     *
     * Delegates entirely to `duels.poker.server.duel.act`: legality, turn order and hand
     * advancement are all decided there, never re-decided here — this is a thin, pure adapter
     * from a room to the runner it hosts. Refuses with `null`, never an exception, when there
     * is nothing to move: a room that never started a duel, or one that already finished, looks
     * the same to a caller as one holding a stale frame for a live one — the engine's own
     * `guard` inside `act` is what tells those apart while a duel is running.
     *
     * @param seat the seat the inbound frame arrived on.
     * @param message the inbound attempt to act.
     * @param seeds the source a hand-ending action draws its next seed from.
     * @return the duel after this frame and the frames each seat is entitled to see, or `null`
     *   if this room is not [RoomState.PLAYING] or carries no [runner].
     */
    public fun act(seat: Int, message: Act, seeds: HandSeedSource): DuelStep? {
        if (state != RoomState.PLAYING) return null
        val liveRunner = runner ?: return null
        return advanceDuel(liveRunner, seat, message, seeds)
    }

    /**
     * Touch this room to advance its idle clock.
     *
     * Updates [lastActivityAt] to the given timestamp without changing any other state.
     * This is called on every action inside a live room, making it possible to reap
     * abandoned rooms without reaping a live duel.
     *
     * @param now the current time in milliseconds; updates [lastActivityAt].
     * @return this room with [lastActivityAt] set to [now].
     */
    public fun touch(now: Long): Room = copy(lastActivityAt = now)

    /**
     * Start [seat]'s disconnect grace window (`ADR-0013`), or restart it if one was already
     * running.
     *
     * The timer restarts on every disconnect: a second call for the same seat overwrites the
     * earlier deadline in [gracePeriods] rather than keeping it — the most recent drop is the
     * one that matters. Also clears [seat] from [absentSeats], so a seat whose window had
     * already run out gets a fresh window rather than staying absent.
     *
     * Pure and total for a seat this room holds. Calling it for a seat this room has not
     * seated is a server bug, not a network event — [init] would reject the resulting room
     * anyway, so this throws directly to name the mistake.
     *
     * @param seat the seat that disconnected; must be seated in this room.
     * @param deadline the instant, on the caller's clock, at which [seat]'s grace window runs
     *   out.
     * @return this room with [seat] counting down in [gracePeriods] from [deadline] and absent
     *   from [absentSeats].
     * @throws IllegalArgumentException if [seat] is not seated in this room.
     */
    public fun disconnect(seat: Int, deadline: Long): Room {
        require(seat == 0 || (seat == 1 && guest != null)) {
            "seat $seat is not seated in this room"
        }
        return copy(
            gracePeriods = gracePeriods + (seat to deadline),
            absentSeats = absentSeats - seat,
        )
    }

    /**
     * Clear [seat]'s disconnect grace window (`ADR-0013`), whether it is still counting down or
     * has already run out.
     *
     * Removes [seat] from both [gracePeriods] and [absentSeats]. Pure and total: a reconnect
     * from a seat nobody was waiting for — the room never noticed it was gone, or had already
     * forgotten — is an ordinary event, not an error, so this returns the room unchanged rather
     * than throwing.
     *
     * @param seat the seat that reconnected.
     * @return this room with [seat] present in neither [gracePeriods] nor [absentSeats]; this
     *   room unchanged if [seat] was in neither already.
     */
    public fun reconnect(seat: Int): Room {
        if (seat !in gracePeriods && seat !in absentSeats) return this
        return copy(
            gracePeriods = gracePeriods - seat,
            absentSeats = absentSeats - seat,
        )
    }

    /**
     * Move every seat whose disconnect grace window (`ADR-0013`) has run out by [now] from
     * [gracePeriods] into [absentSeats], leaving every seat still counting down alone.
     *
     * The comparison is `<=`, not `<`: a window whose deadline is exactly [now] has already run
     * out, not one instant left to go. `TASK-020815` asserts this exact boundary instant, so the
     * two must agree.
     *
     * Pure and total, and idempotent for a fixed [now]: once every seat that has run out has
     * moved, a further call finds nothing left to move. Returns this room unchanged (`this`, so
     * a caller can compare by identity or equality) when no seat has run out.
     *
     * @param now the instant, on the caller's clock, to compare every deadline in
     *   [gracePeriods] against.
     * @return this room with every seat whose deadline is `<= now` moved from [gracePeriods]
     *   into [absentSeats].
     */
    public fun expireGrace(now: Long): Room {
        val expiredSeats = gracePeriods.filterValues { it <= now }.keys
        if (expiredSeats.isEmpty()) return this
        return copy(
            gracePeriods = gracePeriods - expiredSeats,
            absentSeats = absentSeats + expiredSeats,
        )
    }

    public companion object {
        /**
         * Open a fresh room: a host waiting for a guest, no match yet, the host holding the
         * opening button.
         *
         * @param code the room's join code.
         * @param host the player opening the room.
         * @param format the duel's configuration.
         * @param now the current time in milliseconds; `Room` reads no clock of its own.
         * @return a new [RoomState.WAITING] room.
         */
        public fun open(code: RoomCode, host: PlayerId, format: DuelFormat, now: Long): Room =
            Room(
                code = code,
                host = host,
                guest = null,
                state = RoomState.WAITING,
                format = format,
                match = null,
                openingButtonSeat = 0,
                rematchOffers = emptySet(),
                lastActivityAt = now,
            )
    }
}
