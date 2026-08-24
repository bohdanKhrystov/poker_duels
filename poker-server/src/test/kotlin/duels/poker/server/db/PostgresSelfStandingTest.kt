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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Drives [PostgresStandingsReads.standingOf] against a real PostgreSQL.
 *
 * `standingOf` answers one player's own rank and standing, computed against the *whole* ladder
 * regardless of which page — if any — a caller happens to draw (`ADR-0065`, `ADR-0066` §5), and
 * answers nothing for a player who finished no duel this season (`ADR-0065` §4).
 *
 * Fixture discipline matches `PostgresStandingsReadsTest`: creation order and recording order
 * both differ from the expected page, so a query that only works when the fixture happens to be
 * pre-sorted cannot pass by accident here either.
 */
class PostgresSelfStandingTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var standingsReads: PostgresStandingsReads
    private lateinit var profileReads: PostgresProfileReads

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)
        standingsReads = PostgresStandingsReads(dataSource)
        profileReads = PostgresProfileReads(dataSource)
    }

    @Test
    fun theStandingIsTheWholeLaddersRankForAPlayerOnThePageAndForOneFarBelowIt() {
        runBlocking {
            val ladder = seedLadderOfSix()
            val season = Season(2026, 8)
            val asOf = season.endExclusive

            // limit = 2 draws only a and b onto the page; e is on no page this test ever asks
            // standingsPage for.
            val page = standingsReads.standingsPage(season, asOf, limit = 2)
            assertEquals(listOf(ladder.a.id.value, ladder.b.id.value), page.map { it.playerId })

            val aStanding = assertNotNull(standingsReads.standingOf(ladder.a.id, season, asOf))
            assertEquals(1, aStanding.rank)
            assertEquals(3, aStanding.coins)

            // e's rank and coins are right anyway: a fixture whose player is on the page drawn
            // could not tell a whole-ladder aggregate from an echo of the rows, so e -- on no
            // page this test drew -- is the input that actually exercises the whole ladder.
            val eStanding = assertNotNull(standingsReads.standingOf(ladder.e.id, season, asOf))
            assertEquals(6, eStanding.rank)
            assertEquals(-4, eStanding.coins)
        }
    }

    @Test
    fun aTiedPlayersOwnStandingIsTheSharedRank() {
        runBlocking {
            val ladder = seedLadderOfSix()
            val season = Season(2026, 8)
            val asOf = season.endExclusive

            val cStanding = assertNotNull(standingsReads.standingOf(ladder.c.id, season, asOf))
            val dStanding = assertNotNull(standingsReads.standingOf(ladder.d.id, season, asOf))

            // c and d are tied at +1 and both read rank 3, not 3 and 4 (ADR-0064 §1) --
            // dense_rank() would number the second of the pair read 4.
            assertEquals(3, cStanding.rank)
            assertEquals(3, dStanding.rank)
            assertEquals(1, cStanding.coins)
            assertEquals(1, dStanding.coins)
        }
    }

    @Test
    fun aPlayerWhoFinishedNoDuelThisSeasonHasNoStanding() {
        runBlocking {
            // Two different reasons to have no row this season, so a query that got only one of
            // them right could still pass a fixture that tested just the other: julyPlayer
            // played and finished outside the window, neverPlayed never recorded a duel at all.
            val opponent = playerDirectory.resolve(DeviceId("july-opponent"))
            val julyPlayer = playerDirectory.resolve(DeviceId("july-player"))
            val neverPlayed = playerDirectory.resolve(DeviceId("never-played"))

            duelResultStore.record(won(julyPlayer, opponent, Instant.parse("2026-07-20T10:00:00Z")))

            val season = Season(2026, 8)
            val asOf = season.endExclusive

            // The win is real and on the books -- coin_balance carries it -- so a null standing
            // below is about the season window, never about whether the player has ever played.
            val julyProfile = assertNotNull(profileReads.profileOf(julyPlayer.id))
            assertEquals(1, julyProfile.coinBalance)

            assertNull(standingsReads.standingOf(julyPlayer.id, season, asOf))
            assertNull(standingsReads.standingOf(neverPlayed.id, season, asOf))
        }
    }

    /**
     * TASK-050205's fixture A: a, b, c, d, f, e stand at +3, +2, +1, +1, -3, -4 and rank
     * 1, 2, 3, 3, 5, 6. Creation order (d, f, a, e, c, b) and recording order (interleaved by
     * winner, not grouped by opponent) both differ from the ladder, so a page of two draws only
     * a and b.
     */
    private suspend fun seedLadderOfSix(): LadderOfSix {
        val d = playerDirectory.resolve(DeviceId("d"))
        val f = playerDirectory.resolve(DeviceId("f"))
        val a = playerDirectory.resolve(DeviceId("a"))
        val e = playerDirectory.resolve(DeviceId("e"))
        val c = playerDirectory.resolve(DeviceId("c"))
        val b = playerDirectory.resolve(DeviceId("b"))

        val base = Instant.parse("2026-08-11T10:00:00Z")
        duelResultStore.record(won(d, e, base))
        duelResultStore.record(won(a, f, base.plusSeconds(60)))
        duelResultStore.record(won(b, e, base.plusSeconds(120)))
        duelResultStore.record(won(c, e, base.plusSeconds(180)))
        duelResultStore.record(won(b, e, base.plusSeconds(240)))
        duelResultStore.record(won(a, f, base.plusSeconds(300)))
        duelResultStore.record(won(a, f, base.plusSeconds(360)))

        return LadderOfSix(a = a, b = b, c = c, d = d, f = f, e = e)
    }

    private data class LadderOfSix(
        val a: Player,
        val b: Player,
        val c: Player,
        val d: Player,
        val f: Player,
        val e: Player,
    )

    private fun won(winner: Player, loser: Player, finishedAt: Instant): FinishedDuel =
        FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = finishedAt.minusSeconds(60),
            finishedAt = finishedAt,
            seats = listOf(winner.id, loser.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )
}
