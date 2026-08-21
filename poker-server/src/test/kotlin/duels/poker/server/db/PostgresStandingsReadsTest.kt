package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.season.Season
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

/**
 * Drives [PostgresStandingsReads] against a real PostgreSQL: the page comes back in coin order
 * within the window, and the window excludes neighbouring seasons in both directions.
 *
 * Every test records its own duels — the tickets that follow this one (`TASK-050205`,
 * `TASK-050206`, `TASK-050207`) each need a differently shaped ladder, and a shared fixture
 * would have to be rewritten by all three.
 */
class PostgresStandingsReadsTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var standingsReads: PostgresStandingsReads

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)
        standingsReads = PostgresStandingsReads(dataSource)
    }

    @Test
    fun playersComeBackInCoinOrder() {
        runBlocking {
            // Creation order (bob, carol, dave, alice) and recording order (the draw, then
            // alice's two wins) both differ from the expected coin order (alice, then carol
            // and dave tied, then bob). A fixture already sorted either way could not tell a
            // working ORDER BY from a broken one.
            val bob = playerDirectory.resolve(DeviceId("bob"))
            val carol = playerDirectory.resolve(DeviceId("carol"))
            val dave = playerDirectory.resolve(DeviceId("dave"))
            val alice = playerDirectory.resolve(DeviceId("alice"))

            val season = Season(2026, 8)
            val base = Instant.parse("2026-08-10T10:00:00Z")

            duelResultStore.record(drawn(carol, dave, base))
            duelResultStore.record(won(alice, bob, base.plusSeconds(60)))
            duelResultStore.record(won(alice, bob, base.plusSeconds(120)))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 10)

            // The two players tied at 0 are alice's and bob's opponents, in no particular
            // order -- ADR-0064 §3 puts pinning tie order out of this test's business, so only
            // the coins sequence is asserted, never which of carol or dave comes first.
            assertEquals(listOf(2, 0, 0, -2), page.map { it.coins })
            assertEquals(alice.id.value, page.first().playerId)
            assertEquals(bob.id.value, page.last().playerId)
        }
    }

    @Test
    fun theWindowExcludesTheNeighbouringSeasonInBothDirections() {
        runBlocking {
            val julyWinner = playerDirectory.resolve(DeviceId("july-winner"))
            val julyLoser = playerDirectory.resolve(DeviceId("july-loser"))
            val augustWinner = playerDirectory.resolve(DeviceId("august-winner"))
            val augustLoser = playerDirectory.resolve(DeviceId("august-loser"))
            val septemberWinner = playerDirectory.resolve(DeviceId("september-winner"))
            val septemberLoser = playerDirectory.resolve(DeviceId("september-loser"))

            duelResultStore.record(won(julyWinner, julyLoser, Instant.parse("2026-07-31T23:59:59.999Z")))
            duelResultStore.record(won(augustWinner, augustLoser, Instant.parse("2026-08-15T12:00:00Z")))
            duelResultStore.record(won(septemberWinner, septemberLoser, Instant.parse("2026-09-01T00:00:00Z")))

            val season = Season(2026, 8)
            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 10)

            assertEquals(2, page.size)
            assertEquals(setOf(augustWinner.id.value, augustLoser.id.value), page.map { it.playerId }.toSet())
        }
    }

    private fun won(winner: Player, loser: Player, finishedAt: Instant): FinishedDuel =
        FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(winner.id, loser.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )

    // ADR-0015: a draw writes one duel_result row per seat, both coin_delta = 0.
    private fun drawn(first: Player, second: Player, finishedAt: Instant): FinishedDuel =
        FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(first.id, second.id),
            outcome = DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000)),
        )
}
