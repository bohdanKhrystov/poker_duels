package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.http.StandingsCursor
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

    @Test
    fun tiedPlayersReadTheSameRankAndTheNextDistinctStandingSkips() {
        runBlocking {
            // Creation order (d, f, a, e, c, b) differs from the ladder (a, b, c, d, f, e) --
            // ADR-0064 ranks the summed standing, never the order players were created in.
            val d = playerDirectory.resolve(DeviceId("d"))
            val f = playerDirectory.resolve(DeviceId("f"))
            val a = playerDirectory.resolve(DeviceId("a"))
            val e = playerDirectory.resolve(DeviceId("e"))
            val c = playerDirectory.resolve(DeviceId("c"))
            val b = playerDirectory.resolve(DeviceId("b"))

            val season = Season(2026, 8)
            val base = Instant.parse("2026-08-11T10:00:00Z")

            // Recording order interleaves the four winners instead of grouping by opponent, so
            // it is the SUM the query reads, never the order the duels arrived in.
            duelResultStore.record(won(d, e, base))
            duelResultStore.record(won(a, f, base.plusSeconds(60)))
            duelResultStore.record(won(b, e, base.plusSeconds(120)))
            duelResultStore.record(won(c, e, base.plusSeconds(180)))
            duelResultStore.record(won(b, e, base.plusSeconds(240)))
            duelResultStore.record(won(a, f, base.plusSeconds(300)))
            duelResultStore.record(won(a, f, base.plusSeconds(360)))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 6)

            // a(+3) b(+2) {c,d}(+1) f(-3) e(-4): the tie at +1 shares rank 3 and rank 4 is
            // never read -- the next distinct standing (f, -3) is rank 5, per ADR-0064 §1.
            assertEquals(listOf(1, 2, 3, 3, 5, 6), page.map { it.rank })
            assertEquals(listOf(3, 2, 1, 1, -3, -4), page.map { it.coins })
        }
    }

    @Test
    fun theRankIsNotTheRowsOffset() {
        runBlocking {
            // Creation order (x, t2, t1) differs from the ladder ({t1, t2}, x).
            val x = playerDirectory.resolve(DeviceId("x"))
            val t2 = playerDirectory.resolve(DeviceId("t2"))
            val t1 = playerDirectory.resolve(DeviceId("t1"))

            val season = Season(2026, 8)
            val base = Instant.parse("2026-08-12T10:00:00Z")

            duelResultStore.record(won(t2, x, base))
            duelResultStore.record(won(t1, x, base.plusSeconds(60)))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 3)

            // t1 and t2 both stand at +1 and share rank 1; numbering rows from the page offset
            // instead would return [1, 2, 3] here, on page one, where a whole-ladder defect
            // would otherwise hide until a later page.
            assertEquals(listOf(1, 1, 3), page.map { it.rank })
        }
    }

    @Test
    fun aTieSpanningAPageBoundaryRepeatsTheRankAndEachPlayerOnce() {
        runBlocking {
            // Creation order (f, b, e, d, a, c) differs from the ladder (a, b, c, d, f, e).
            val f = playerDirectory.resolve(DeviceId("f"))
            val b = playerDirectory.resolve(DeviceId("b"))
            val e = playerDirectory.resolve(DeviceId("e"))
            val d = playerDirectory.resolve(DeviceId("d"))
            val a = playerDirectory.resolve(DeviceId("a"))
            val c = playerDirectory.resolve(DeviceId("c"))

            val season = Season(2026, 8)
            val asOf = season.endExclusive
            val base = Instant.parse("2026-08-13T10:00:00Z")

            duelResultStore.record(won(c, e, base))
            duelResultStore.record(won(a, f, base.plusSeconds(60)))
            duelResultStore.record(won(b, e, base.plusSeconds(120)))
            duelResultStore.record(won(a, f, base.plusSeconds(180)))
            duelResultStore.record(won(d, e, base.plusSeconds(240)))
            duelResultStore.record(won(b, e, base.plusSeconds(300)))
            duelResultStore.record(won(a, f, base.plusSeconds(360)))

            val pageOne = standingsReads.standingsPage(season, asOf, limit = 3)
            val lastRow = pageOne.last()
            val after = StandingsCursor(asOf, lastRow.coins, UUID.fromString(lastRow.playerId))
            val pageTwo = standingsReads.standingsPage(season, asOf, limit = 3, after = after)

            // The tie between c and d straddles the boundary: page one's last row and page
            // two's first row both read rank 3. ADR-0064 §2 makes that repeat correct, not a
            // duplicate row, so what is asserted is the two players, never their order.
            assertEquals(3, lastRow.rank)
            assertEquals(3, pageTwo.first().rank)
            assertEquals(setOf(c.id.value, d.id.value), setOf(lastRow.playerId, pageTwo.first().playerId))

            // Totality and disjointness are properties of players, not of rank numbers: the
            // six ids across both pages, taken together, are exactly the six that were seeded.
            val idsAcrossBothPages = (pageOne + pageTwo).map { it.playerId }
            assertEquals(6, idsAcrossBothPages.size)
            assertEquals(
                setOf(a.id.value, b.id.value, c.id.value, d.id.value, e.id.value, f.id.value),
                idsAcrossBothPages.toSet(),
            )
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
