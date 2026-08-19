package duels.poker.server.season

/**
 * A calendar month identified in wire form per ADR-0061 §1 as the only identifier a season has.
 * The human-readable form "August 2026" is STORY-0503's string, not this type's.
 */
public data class Season(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "month must be between 1 and 12, got $month" }
    }

    override fun toString(): String = "%04d-%02d".format(year, month)
}
