package duels.poker.server.season

import duels.poker.server.duel.FinishedDuel
import java.time.Clock
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

/**
 * The season containing [instant], the calendar month read in UTC and nothing else.
 *
 * ADR-0061 fixes the boundary in UTC so standings are one ordering, not one per reader's clock.
 * The cost, named in ADR-0061's *What it costs*, lives here: the client renders instants in the
 * reader's locale (`finishedAtText`), so a player far enough east can read a duel as finishing on
 * 1 September and find it counted in August. That mismatch is intended, not a defect —
 * localising the boundary per player would make the standings stop being one ordering, and
 * printing UTC everywhere is worse for everything else.
 */
public fun seasonOf(instant: Instant): Season {
    val yearMonth = YearMonth.from(instant.atOffset(ZoneOffset.UTC))
    return Season(yearMonth.year, yearMonth.monthValue)
}

/**
 * The season to which a finished duel belongs, read from its finish time.
 *
 * A duel pays its coin exactly once, at the end ([finishedAt]), so it belongs entirely to one
 * season per ADR-0061 §2 and ADR-0017. A duel that began on 31 August and finished on 1 September
 * is a September duel in full, never an August one — [startedAt] is deliberately unread.
 */
public fun seasonOf(duel: FinishedDuel): Season = seasonOf(duel.finishedAt)

/**
 * The season it is now, read from [clock] and nothing else.
 *
 * [clock] has no default per ADR-0062 §3: a defaulted pure function can be called with no clock at
 * all, and that call would compile anywhere with nothing in a diff to notice. Production's one
 * `Clock.systemUTC()` belongs at the composition root, not here.
 *
 * No zone is named here: [seasonOf] already reads UTC, and [clock]'s own zone is never consulted,
 * so a clock fixed in another zone cannot change the answer.
 */
public fun currentSeason(clock: Clock): Season = seasonOf(clock.instant())
