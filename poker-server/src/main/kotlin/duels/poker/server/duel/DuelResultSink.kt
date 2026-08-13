package duels.poker.server.duel

import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.log.MatchLog
import duels.poker.server.session.PlayerId

/**
 * The result of a finished duel, ready to be persisted.
 *
 * The player identities are indexed by seat, so [seats][0] is seat 0 — the same numbering as
 * [MatchLog.buttonSeat] and [DuelOutcome.winner]. Off-by-one errors here award the coin to the
 * loser, so seat indexing is load-bearing.
 *
 * @param outcome the end condition of the duel (who won, hand count, final stacks).
 * @param seats the players occupying seats 0 and 1, indexed by seat number.
 * @param log the replayable record of the whole duel.
 */
public data class DuelResult(
    val outcome: DuelOutcome,
    val seats: List<PlayerId>,
    val log: MatchLog,
) {
    init {
        require(seats.size == 2) {
            "seats must contain exactly 2 players, got ${seats.size}"
        }
        require(seats[0] != seats[1]) {
            "seats[0] and seats[1] must be different players, got ${seats[0]} in both"
        }
        require(log.events.filterIsInstance<MatchFinished>().singleOrNull()?.outcome == outcome) {
            "log must record the same outcome: log has ${log.events.filterIsInstance<MatchFinished>().singleOrNull()?.outcome}, but result has $outcome"
        }
    }

    /**
     * The player who won, indexed from [seats], or null if the duel was a draw.
     *
     * Seat indices are load-bearing: the winner sits in seat [DuelOutcome.winner], and the player
     * in that seat is [seats][[DuelOutcome.winner]] if someone won.
     */
    public val winner: PlayerId?
        get() = outcome.winner?.let { seats[it] }
}

/**
 * A port that records a finished duel for persistence.
 *
 * Each real implementation handles I/O differently — Postgres, in-memory, test doubles, etc. —
 * so this is a port defined at its consumer (the duel package) rather than in a persistence
 * module. Whether the whole [MatchLog] is persisted, and where, is undecided (`DEC-008`);
 * carrying it in this value neither answers nor prejudges that.
 */
public fun interface DuelResultSink {
    /**
     * Record the finished duel for later retrieval.
     *
     * @param result the duel to record. The result must satisfy all invariants ([DuelResult.init]).
     */
    public suspend fun record(result: DuelResult)
}
