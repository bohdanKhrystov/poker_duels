package duels.poker.server.season

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * A calendar month identified in wire form per ADR-0061 §1 as the only identifier a season has.
 * The human-readable form "August 2026" is STORY-0503's string, not this type's.
 */
public data class Season(val year: Int, val month: Int) {
    init {
        require(month in 1..12) { "month must be between 1 and 12, got $month" }
    }

    /**
     * The first instant of this month in UTC.
     * Public because STORY-0502's standings query uses it as the SQL window lower bound.
     */
    public val start: Instant by lazy {
        YearMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    }

    /**
     * The first instant of the next month in UTC, exclusive upper bound of this season.
     * Public because STORY-0502's standings query uses it as the SQL window upper bound.
     * December correctly carries into January of the following year via YearMonth.plusMonths.
     */
    public val endExclusive: Instant by lazy {
        YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
    }

    /**
     * True if the instant falls within this season's bounds [start, endExclusive).
     * Inclusive at the start, exclusive at the end — the half-open interval per ADR-0061 §1,
     * ensuring consecutive seasons neither gap nor overlap.
     */
    public fun contains(instant: Instant): Boolean =
        !instant.isBefore(start) && instant.isBefore(endExclusive)

    override fun toString(): String = "%04d-%02d".format(year, month)
}
