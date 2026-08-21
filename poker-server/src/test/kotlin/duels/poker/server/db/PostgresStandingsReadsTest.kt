package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.http.StandingsCursor
import duels.poker.server.protocol.http.StandingRow
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
import kotlin.test.assertTrue

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
    private lateinit var profileWrites: PostgresProfileWrites
    private lateinit var profileReads: PostgresProfileReads

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)
        standingsReads = PostgresStandingsReads(dataSource)
        profileWrites = PostgresProfileWrites(dataSource)
        profileReads = PostgresProfileReads(dataSource)
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

    @Test
    fun aDrawEarnsARowAtZeroAndAPlayerWhoDidNotPlayHasNone() {
        runBlocking {
            // Two different kinds of absence, so neither a LEFT JOIN player (which would list
            // carol at 0) nor a missing lower bound (which would keep dave and erin) can pass by
            // getting only one of them right. Creation order (dave, carol, alice, erin, bob) and
            // recording order (dave-erin's July duel, then alice-bob's August draw) both differ
            // from the expected page (alice, bob).
            val dave = playerDirectory.resolve(DeviceId("dave"))
            val carol = playerDirectory.resolve(DeviceId("carol"))
            val alice = playerDirectory.resolve(DeviceId("alice"))
            val erin = playerDirectory.resolve(DeviceId("erin"))
            val bob = playerDirectory.resolve(DeviceId("bob"))

            val season = Season(2026, 8)
            duelResultStore.record(won(dave, erin, Instant.parse("2026-07-20T10:00:00Z")))
            duelResultStore.record(drawn(bob, alice, Instant.parse("2026-08-10T10:00:00Z")))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 10)

            assertEquals(2, page.size)
            assertEquals(setOf(alice.id.value, bob.id.value), page.map { it.playerId }.toSet())
            assertEquals(listOf(0, 0), page.map { it.coins })
        }
    }

    @Test
    fun aNamelessPlayerHasARowCarryingNull() {
        runBlocking {
            // Creation order (bob, alice) differs from recording order below, and from the
            // narrative (alice is the one who names herself).
            val bob = playerDirectory.resolve(DeviceId("bob"))
            val alice = playerDirectory.resolve(DeviceId("alice"))
            profileWrites.setDisplayName(alice.id, "Alice")

            val season = Season(2026, 8)
            duelResultStore.record(won(alice, bob, Instant.parse("2026-08-17T10:00:00Z")))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 10)

            assertEquals("Alice", page.first { it.playerId == alice.id.value }.displayName)
            assertNull(page.first { it.playerId == bob.id.value }.displayName)
        }
    }

    @Test
    fun onePlayerWithOneDuelIsOnThePageBesideOneWithSeveral() {
        runBlocking {
            // Frank's only duel this season is the loss below; george holds that same win plus
            // two more against different opponents, so no minimum-duels gate could tell george's
            // rows from frank's by shape alone. Creation order (grace, henry, frank, george) and
            // recording order (frank, then grace, then henry) both differ from each other.
            val grace = playerDirectory.resolve(DeviceId("grace"))
            val henry = playerDirectory.resolve(DeviceId("henry"))
            val frank = playerDirectory.resolve(DeviceId("frank"))
            val george = playerDirectory.resolve(DeviceId("george"))

            val season = Season(2026, 8)
            val base = Instant.parse("2026-08-18T10:00:00Z")
            duelResultStore.record(won(george, frank, base))
            duelResultStore.record(won(george, grace, base.plusSeconds(60)))
            duelResultStore.record(won(george, henry, base.plusSeconds(120)))

            val page = standingsReads.standingsPage(season, season.endExclusive, limit = 10)

            assertEquals(-1, page.first { it.playerId == frank.id.value }.coins)
            assertEquals(3, page.first { it.playerId == george.id.value }.coins)
        }
    }

    @Test
    fun theCoinsAreTheSeasonsWindowAndNotTheAllTimeColumn() {
        runBlocking {
            // Creation order (bob, dave, carol, alice) and recording order (carol's single
            // August win, then alice's August win, then alice's two July wins) both differ from
            // the narrative told in the ticket -- ADR-0061 §4 is exactly what would be missed by
            // a fixture that happened to follow the story order.
            val bob = playerDirectory.resolve(DeviceId("bob"))
            val dave = playerDirectory.resolve(DeviceId("dave"))
            val carol = playerDirectory.resolve(DeviceId("carol"))
            val alice = playerDirectory.resolve(DeviceId("alice"))

            val season = Season(2026, 8)
            val asOf = season.endExclusive

            duelResultStore.record(won(carol, dave, Instant.parse("2026-08-10T10:00:00Z")))
            duelResultStore.record(won(alice, bob, Instant.parse("2026-08-05T10:00:00Z")))
            duelResultStore.record(won(alice, bob, Instant.parse("2026-07-20T10:00:00Z")))
            duelResultStore.record(won(alice, bob, Instant.parse("2026-07-05T10:00:00Z")))

            // The same path the profile strip reads, per the ticket -- not raw SQL, so the
            // comparison is between the two answers the product actually gives.
            val aliceProfile = assertNotNull(profileReads.profileOf(DeviceId("alice")))
            val carolProfile = assertNotNull(profileReads.profileOf(DeviceId("carol")))

            val page = standingsReads.standingsPage(season, asOf, limit = 10)
            val aliceStanding = page.first { it.playerId == alice.id.value }
            val carolStanding = page.first { it.playerId == carol.id.value }

            // Alice's all-time column carries all three of her wins; her season window carries
            // only August's. The two numbers disagree on purpose (ADR-0061 §4), and a query that
            // quietly read the column instead of the window would make them agree here.
            assertEquals(3, aliceProfile.coinBalance)
            assertEquals(1, aliceStanding.coins)

            // Carol's only duel is inside the season, so her column and her window agree. Without
            // her, "coinBalance != ladder coins" would look like the property under test, which
            // is false in general -- this is what stops that wrong reading from also passing.
            assertEquals(1, carolProfile.coinBalance)
            assertEquals(1, carolStanding.coins)
        }
    }

    @Test
    fun theSeasonsStandingsSumToExactlyZero() {
        runBlocking {
            // Creation order (erin, dave, bob, alice, carol) and recording order (the draw
            // first, then carol's win, then alice's win) both differ from the narrative telling
            // of who played whom, the same discipline every fixture above keeps.
            val erin = playerDirectory.resolve(DeviceId("erin"))
            val dave = playerDirectory.resolve(DeviceId("dave"))
            val bob = playerDirectory.resolve(DeviceId("bob"))
            val alice = playerDirectory.resolve(DeviceId("alice"))
            val carol = playerDirectory.resolve(DeviceId("carol"))

            val season = Season(2026, 8)
            val asOf = season.endExclusive

            // One draw (erin vs bob) and two decisive duels (carol beats dave, alice beats bob)
            // among five players -- ADR-0063 §4: every duel writes rows that sum to zero, drawn
            // or decisive alike, so the whole ladder must too.
            duelResultStore.record(drawn(erin, bob, Instant.parse("2026-08-02T09:00:00Z")))
            duelResultStore.record(won(carol, dave, Instant.parse("2026-08-06T09:00:00Z")))
            duelResultStore.record(won(alice, bob, Instant.parse("2026-08-09T09:00:00Z")))

            val rows = mutableListOf<StandingRow>()
            var after: StandingsCursor? = null
            while (true) {
                val page = standingsReads.standingsPage(season, asOf, limit = 2, after = after)
                if (page.isEmpty()) break
                rows += page
                val last = page.last()
                after = StandingsCursor(asOf, last.coins, UUID.fromString(last.playerId))
                if (page.size < 2) break
            }

            // The row count is asserted before the total: an empty ladder also sums to zero, so
            // the count is what proves the walk returned the rows this fixture recorded.
            assertTrue(rows.size >= 5)
            assertEquals(0, rows.sumOf { it.coins })
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
