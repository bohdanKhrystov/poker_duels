package duels.poker.engine.log

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.MatchEvent
import duels.poker.engine.duel.MatchFinished
import kotlinx.serialization.Serializable

/** The current schema version for [MatchLog]. Bumping it is a log migration. */
public const val MATCH_LOG_VERSION: Int = 1

/**
 * The replayable record of a whole duel: its format, its hands in order, and its own match-level
 * events. The two logs are separate — a match log references its hands but does not contain their
 * events; each [HandLog] owns its own events per `ADR-0009`.
 *
 * A log of an unfinished duel is legal: the server appends to the log as the duel plays.
 */
@Serializable
public data class MatchLog(
    val format: DuelFormat,
    val buttonSeat: Int,
    val hands: List<HandLog>,
    val events: List<MatchEvent>,
    val version: Int = MATCH_LOG_VERSION,
) {
    init {
        require(version >= 1) { "version must be at least 1, was $version" }
        require(buttonSeat in 0..1) { "buttonSeat must be 0 or 1, was $buttonSeat" }

        // Validate that hands are dense, 1-based, and in order
        hands.forEachIndexed { index, hand ->
            require(hand.handNumber == index + 1) {
                "hands[$index].handNumber must be ${index + 1}, was ${hand.handNumber}"
            }
        }

        // Validate that button alternates correctly
        hands.forEachIndexed { index, hand ->
            val expectedButtonSeat = (buttonSeat + index) % 2
            require(hand.buttonSeat == expectedButtonSeat) {
                "hands[$index].buttonSeat must be $expectedButtonSeat, was ${hand.buttonSeat}"
            }
        }

        // Validate that events have sequential sequence numbers
        events.forEachIndexed { index, event ->
            require(event.sequence == index) {
                "events[$index].sequence must be $index, was ${event.sequence}"
            }
        }

        // Validate at most one MatchFinished
        val matchFinishedCount = events.count { it is MatchFinished }
        require(matchFinishedCount <= 1) {
            "at most one MatchFinished event allowed, found $matchFinishedCount"
        }
    }
}
