package duels.poker.engine.log

import duels.poker.engine.game.GameEvent
import duels.poker.engine.game.HandStarted
import duels.poker.engine.game.PlayerAction

/** The current schema version for [HandLog]. Bumping it is a log migration. */
public const val HAND_LOG_VERSION: Int = 1

/**
 * The replayable record of one hand: the seed it was dealt from, the parameters it opened with,
 * the actions it was given, and the events it produced — enough by itself to replay the hand
 * card for card.
 *
 * A log of an unfinished hand is legal: the server appends to the log as the hand plays.
 */
public data class HandLog(
    val seed: Long,
    val handNumber: Int,
    val buttonSeat: Int,
    val stacks: List<Int>,
    val smallBlind: Int,
    val bigBlind: Int,
    val actions: List<PlayerAction>,
    val events: List<GameEvent>,
    val version: Int = HAND_LOG_VERSION,
) {
    init {
        require(version >= 1) { "version must be at least 1, was $version" }
        require(handNumber >= 1) { "handNumber must be at least 1, was $handNumber" }
        require(buttonSeat in 0..1) { "buttonSeat must be 0 or 1, was $buttonSeat" }
        require(stacks.size == 2) { "stacks must have exactly 2 entries, had ${stacks.size}" }
        require(stacks.all { it >= 1 }) { "stacks entries must be at least 1, were $stacks" }
        require(smallBlind > 0 && smallBlind < bigBlind) {
            "smallBlind must be greater than 0 and less than bigBlind, was $smallBlind and $bigBlind"
        }
        require(events.isNotEmpty()) { "events must be non-empty" }
        require(events.first() is HandStarted) {
            "first event must be HandStarted, was ${events.first()::class.simpleName}"
        }
        events.forEachIndexed { index, event ->
            require(event.sequence == index) {
                "event sequence at index $index must be $index, was ${event.sequence}"
            }
        }
    }
}
