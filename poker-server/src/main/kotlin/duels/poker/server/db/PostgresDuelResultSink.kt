package duels.poker.server.db

import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import java.time.Clock
import java.time.Instant

/**
 * Adapts [PostgresDuelResultStore] to the [DuelResultSink] port so the duel runner can be handed
 * a store it never has to know is Postgres.
 *
 * The adapter is deliberately thin: it does not open a connection, run SQL, or compute a coin
 * delta. All of that is [PostgresDuelResultStore]'s transaction (`TASK-021006`) and
 * `duels.poker.server.duel.coinDeltas` (`ADR-0014`); this class only builds the [FinishedDuel]
 * that store expects from the [DuelResult] the port receives, filling in the two things
 * `DuelResult` does not itself carry in [FinishedDuel]'s shape:
 *
 * - `startedAt` / `finishedAt`, from the injected [clock], never `Instant.now()` directly, so a
 *   test can pin the wall-clock value stamped on a row. This is [java.time.Clock], not
 *   `duels.poker.server.time.ServerClock`: `ServerClock` measures elapsed time from an arbitrary
 *   epoch (`System.nanoTime()`) for timeouts and grace periods, and its own KDoc says never to
 *   use it to stamp a database row with a date — a `java.time.Clock` is the instrument that
 *   actually reports a wall-clock date, and it is fixable in tests via `Clock.fixed`.
 * - the format label, read from the result's own [duels.poker.engine.log.MatchLog.format] via
 *   [formatLabel] — the log already carries the format, so there is nothing to invent here.
 *
 * The duel id is not one of those two: it comes straight off [DuelResult.duelId], the same id
 * the room that hosted the duel minted once and carries across any retry (`TASK-020717`). This
 * class used to mint its own id per call via an injected `DuelIdSource`; that let a retried
 * `record` insert a second row and double-award the coin, because the store's `ON CONFLICT (id)`
 * idempotency (`TASK-021009`) only engages when every attempt to record the same duel carries the
 * same id. There is now exactly one place a duel id is minted — the room — so that bug has
 * nowhere left to come back from.
 *
 * `seats` and `outcome` map straight across unchanged.
 *
 * @param store the store this sink delegates every write to.
 * @param clock the source of the wall-clock timestamps stamped on each duel row. Supplied by the
 *   composition root to ensure exactly one place mints a wall clock.
 */
public class PostgresDuelResultSink(
    private val store: PostgresDuelResultStore,
    private val clock: Clock,
) : DuelResultSink {
    override suspend fun record(result: DuelResult) {
        // DuelResult carries no notion of when the duel began — only that it is now finished —
        // so both timestamps are the one instant the sink actually knows: the moment it was
        // asked to record. A future ticket that threads a real start time through DuelResult
        // can split these; inventing one here would be exactly the kind of fabricated value
        // this adapter exists to avoid.
        val recordedAt = Instant.now(clock)
        val duel = FinishedDuel(
            id = result.duelId,
            format = formatLabel(result.log.format),
            startedAt = recordedAt,
            finishedAt = recordedAt,
            seats = result.seats,
            outcome = result.outcome,
        )
        store.record(duel)
    }
}
