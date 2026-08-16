package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.EndCondition
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for reading a player's profile and recent duels list.
 *
 * A drawn duel writes two `duel_result` rows with `coin_delta = 0` (ADR-0015), ensuring
 * every participant has a result row in the read path. If a draw ever wrote no rows, both
 * players' lists would come back empty and these tests would fail loudly — that is what they
 * exist to catch. The tests `aDrawnDuelAppearsInBothPlayersLists` and
 * `aDrawnDuelReadsBackAsDrewWithAZeroDeltaAndAnOpponent` guard this invariant by asserting
 * that a drawn duel is visible to both players with the correct outcome and delta.
 */
class PostgresProfileReadsTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var profileReads: PostgresProfileReads
    private lateinit var duelResultStore: PostgresDuelResultStore
    private lateinit var alice: Player
    private lateinit var bob: Player

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        profileReads = PostgresProfileReads(dataSource)
        duelResultStore = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = playerDirectory.resolve(DeviceId("alice"))
            bob = playerDirectory.resolve(DeviceId("bob"))
        }
    }

    @Test
    fun aKnownDeviceReadsBackItsProfileAtZero() = runBlocking {
        val deviceId = DeviceId("alice")
        val player = playerDirectory.resolve(deviceId)

        val profile = profileReads.profileOf(deviceId)

        assertEquals(player.id.value, profile?.playerId)
        assertEquals(0, profile?.coinBalance)
    }

    @Test
    fun anUnknownDeviceReadsBackNull() = runBlocking {
        val profile = profileReads.profileOf(DeviceId("ghost"))

        assertNull(profile)
    }

    @Test
    fun readingAnUnknownDeviceCreatesNoProfile() = runBlocking {
        val countBefore = playerRowCount()

        profileReads.profileOf(DeviceId("ghost"))

        val countAfter = playerRowCount()
        assertEquals(countBefore, countAfter)
    }

    @Test
    fun twoDevicesReadBackTheirOwnProfiles() = runBlocking {
        val aliceDeviceId = DeviceId("alice")
        val bobDeviceId = DeviceId("bob")
        val alice = playerDirectory.resolve(aliceDeviceId)
        val bob = playerDirectory.resolve(bobDeviceId)

        val aliceProfile = profileReads.profileOf(aliceDeviceId)
        val bobProfile = profileReads.profileOf(bobDeviceId)

        assertEquals(alice.id.value, aliceProfile?.playerId)
        assertEquals(bob.id.value, bobProfile?.playerId)
        assertNotEquals(aliceProfile?.playerId, bobProfile?.playerId)
    }

    @Test
    fun theWinnersBalanceReadsBackAsOne() = runBlocking {
        val duel = finishedDuel(winner = 0)

        duelResultStore.record(duel)

        val profile = profileReads.profileOf(DeviceId("alice"))
        assertEquals(1, profile?.coinBalance)
    }

    @Test
    fun aPlayerWhoseOnlyDuelWasALossReadsBackMinusOne() = runBlocking {
        val duel = finishedDuel(winner = 0)

        duelResultStore.record(duel)

        val profile = profileReads.profileOf(DeviceId("bob"))
        assertEquals(-1, profile?.coinBalance)
    }

    @Test
    fun aRecordedDuelComesBackWithItsOpponentOutcomeAndDelta() = runBlocking {
        val duelId = UUID.randomUUID()
        duelResultStore.record(finishedDuel(winner = 0, id = duelId))

        val duels = profileReads.recentDuelsOf(alice.id, 10)

        assertEquals(1, duels.size)
        val entry = duels.single()
        assertEquals(duelId.toString(), entry.duelId)
        assertEquals(bob.id.value, entry.opponentPlayerId)
        assertEquals(DuelOutcomeLabel.WON, entry.outcome)
        assertEquals(1, entry.coinDelta)
    }

    @Test
    fun theLoserSeesTheSameDuelAsALoss() = runBlocking {
        val duelId = UUID.randomUUID()
        duelResultStore.record(finishedDuel(winner = 0, id = duelId))

        val duels = profileReads.recentDuelsOf(bob.id, 10)

        assertEquals(1, duels.size)
        val entry = duels.single()
        assertEquals(duelId.toString(), entry.duelId)
        assertEquals(alice.id.value, entry.opponentPlayerId)
        assertEquals(DuelOutcomeLabel.LOST, entry.outcome)
        assertEquals(-1, entry.coinDelta)
    }

    @Test
    fun theFinishTimeComesBackAsTheStoredInstant() = runBlocking {
        val finishedAt = Instant.parse("2026-08-13T11:30:00Z")
        duelResultStore.record(finishedDuel(winner = 0, finishedAt = finishedAt))

        val entry = profileReads.recentDuelsOf(alice.id, 10).single()

        assertEquals(finishedAt.toString(), entry.finishedAt)
    }

    @Test
    fun aPlayerWithNoDuelsGetsAnEmptyList() = runBlocking {
        val carol = playerDirectory.resolve(DeviceId("carol"))

        val duels = profileReads.recentDuelsOf(carol.id, 10)

        assertTrue(duels.isEmpty())
    }

    @Test
    fun aResultLineCarriesTheHandCount() = runBlocking {
        val duelId = UUID.randomUUID()
        val handCount = 42
        duelResultStore.record(finishedDuel(winner = 0, id = duelId, handsPlayed = handCount))

        val entry = profileReads.recentDuelsOf(alice.id, 10).single()

        assertEquals(handCount, entry.handsPlayed)
    }

    @Test
    fun duelsComeBackNewestFirst() = runBlocking {
        // Record three duels with distinct, deliberately out-of-order finishedAt values
        // so a query returning insertion order would fail
        val oldestInstant = Instant.parse("2026-08-13T10:00:00Z")
        val middleInstant = Instant.parse("2026-08-13T10:01:00Z")
        val newestInstant = Instant.parse("2026-08-13T10:02:00Z")

        val oldestDuelId = UUID.randomUUID()
        val middleDuelId = UUID.randomUUID()
        val newestDuelId = UUID.randomUUID()

        duelResultStore.record(finishedDuel(winner = 0, id = oldestDuelId, finishedAt = oldestInstant))
        duelResultStore.record(finishedDuel(winner = 0, id = middleDuelId, finishedAt = middleInstant))
        duelResultStore.record(finishedDuel(winner = 0, id = newestDuelId, finishedAt = newestInstant))

        val duels = profileReads.recentDuelsOf(alice.id, 10)

        assertEquals(3, duels.size)
        assertEquals(newestDuelId.toString(), duels[0].duelId)
        assertEquals(middleDuelId.toString(), duels[1].duelId)
        assertEquals(oldestDuelId.toString(), duels[2].duelId)
    }

    @Test
    fun theLimitCapsTheNumberOfDuelsReturned() = runBlocking {
        // Record three duels with distinct instants, oldest to newest
        val oldestInstant = Instant.parse("2026-08-13T10:00:00Z")
        val middleInstant = Instant.parse("2026-08-13T10:01:00Z")
        val newestInstant = Instant.parse("2026-08-13T10:02:00Z")

        val oldestDuelId = UUID.randomUUID()
        val middleDuelId = UUID.randomUUID()
        val newestDuelId = UUID.randomUUID()

        duelResultStore.record(finishedDuel(winner = 0, id = oldestDuelId, finishedAt = oldestInstant))
        duelResultStore.record(finishedDuel(winner = 0, id = middleDuelId, finishedAt = middleInstant))
        duelResultStore.record(finishedDuel(winner = 0, id = newestDuelId, finishedAt = newestInstant))

        // Request only 2 duels
        val duels = profileReads.recentDuelsOf(alice.id, 2)

        // Should get exactly 2, and they should be the newest ones
        assertEquals(2, duels.size)
        assertEquals(newestDuelId.toString(), duels[0].duelId)
        assertEquals(middleDuelId.toString(), duels[1].duelId)
    }

    @Test
    fun anotherPlayersDuelsNeverAppear() = runBlocking {
        val carol = playerDirectory.resolve(DeviceId("carol"))
        val dave = playerDirectory.resolve(DeviceId("dave"))

        // Record one alice/bob duel
        val aliceBobDuelId = UUID.randomUUID()
        duelResultStore.record(finishedDuel(winner = 0, id = aliceBobDuelId, finishedAt = Instant.parse("2026-08-13T10:00:00Z")))

        // Record one carol/dave duel
        val carolDaveDuelId = UUID.randomUUID()
        val carolDaveDuel = FinishedDuel(
            id = carolDaveDuelId,
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:05:00Z"),
            finishedAt = Instant.parse("2026-08-13T10:06:00Z"),
            seats = listOf(carol.id, dave.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )
        duelResultStore.record(carolDaveDuel)

        // Alice should see only her duel with Bob
        val aliceDuels = profileReads.recentDuelsOf(alice.id, 10)
        assertEquals(1, aliceDuels.size)
        assertEquals(aliceBobDuelId.toString(), aliceDuels.single().duelId)

        // Carol should see only her duel with Dave
        val carolDuels = profileReads.recentDuelsOf(carol.id, 10)
        assertEquals(1, carolDuels.size)
        assertEquals(carolDaveDuelId.toString(), carolDuels.single().duelId)
    }

    @Test
    fun aDrawnDuelAppearsInBothPlayersLists() = runBlocking {
        // A FixedHands duel can end level. A Freezeout cannot, so we must test with a format
        // that the engine can actually produce a draw for.
        val drawnDuelId = UUID.randomUUID()
        val drawnDuel = finishedDuel(winner = null, id = drawnDuelId)
            .copy(format = formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))))

        duelResultStore.record(drawnDuel)

        val aliceDuels = profileReads.recentDuelsOf(alice.id, 10)
        val bobDuels = profileReads.recentDuelsOf(bob.id, 10)

        assertEquals(1, aliceDuels.size)
        assertEquals(1, bobDuels.size)
        assertEquals(drawnDuelId.toString(), aliceDuels.single().duelId)
        assertEquals(drawnDuelId.toString(), bobDuels.single().duelId)
    }

    @Test
    fun aDrawnDuelReadsBackAsDrewWithAZeroDeltaAndAnOpponent() = runBlocking {
        // A FixedHands duel can end level. A Freezeout cannot, so we must test with a format
        // that the engine can actually produce a draw for.
        val drawnDuelId = UUID.randomUUID()
        val drawnDuel = finishedDuel(winner = null, id = drawnDuelId)
            .copy(format = formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))))

        duelResultStore.record(drawnDuel)

        val aliceDuels = profileReads.recentDuelsOf(alice.id, 10)
        val bobDuels = profileReads.recentDuelsOf(bob.id, 10)

        val aliceEntry = aliceDuels.single()
        val bobEntry = bobDuels.single()

        assertEquals(DuelOutcomeLabel.DREW, aliceEntry.outcome)
        assertEquals(0, aliceEntry.coinDelta)
        assertEquals(bob.id.value, aliceEntry.opponentPlayerId)

        assertEquals(DuelOutcomeLabel.DREW, bobEntry.outcome)
        assertEquals(0, bobEntry.coinDelta)
        assertEquals(alice.id.value, bobEntry.opponentPlayerId)
    }

    @Test
    fun aProfileReadsBackTheNameItsRowHolds() = runBlocking {
        val carol = playerDirectory.resolve(DeviceId("carol"))
        setPlayerDisplayName(carol.id.value, "bob")

        val profile = profileReads.profileOf(DeviceId("carol"))

        assertEquals("bob", profile?.displayName)
    }

    @Test
    fun aProfileWithNoNameReadsBackNull() = runBlocking {
        val dave = playerDirectory.resolve(DeviceId("dave"))
        setPlayerDisplayName(dave.id.value, null)

        val profile = profileReads.profileOf(DeviceId("dave"))

        assertNull(profile?.displayName)
    }

    @Test
    fun aDuelAgainstANamedOpponentReadsBackThatName() = runBlocking {
        setPlayerDisplayName(bob.id.value, "Ingrid")
        val duelId = UUID.randomUUID()

        duelResultStore.record(finishedDuel(winner = 0, id = duelId))

        val duels = profileReads.recentDuelsOf(alice.id, 10)

        assertEquals(1, duels.size)
        assertEquals("Ingrid", duels.single().opponentDisplayName)
    }

    @Test
    fun anUnnamedOpponentReadsBackNullEvenWhenTheReaderIsNamed() = runBlocking {
        setPlayerDisplayName(alice.id.value, "Ingrid")
        val duelId = UUID.randomUUID()

        duelResultStore.record(finishedDuel(winner = 0, id = duelId))

        val aliceDuels = profileReads.recentDuelsOf(alice.id, 10)
        val bobDuels = profileReads.recentDuelsOf(bob.id, 10)

        assertEquals(1, aliceDuels.size)
        assertEquals(null, aliceDuels.single().opponentDisplayName)

        assertEquals(1, bobDuels.size)
        assertEquals("Ingrid", bobDuels.single().opponentDisplayName)
    }

    @Test
    fun aNameSetAfterTheDuelFinishedAppearsOnItsLine() = runBlocking {
        val duelId = UUID.randomUUID()
        duelResultStore.record(finishedDuel(winner = 0, id = duelId))

        val duelsBefore = profileReads.recentDuelsOf(alice.id, 10)
        assertEquals(1, duelsBefore.size)
        assertEquals(duelId.toString(), duelsBefore.single().duelId)
        assertNull(duelsBefore.single().opponentDisplayName)

        setPlayerDisplayName(bob.id.value, "Torvald")

        val duelsAfter = profileReads.recentDuelsOf(alice.id, 10)
        assertEquals(1, duelsAfter.size)
        assertEquals(duelId.toString(), duelsAfter.single().duelId)
        assertEquals("Torvald", duelsAfter.single().opponentDisplayName)
    }

    @Test
    fun everyDuelReturnsOneRowWhicheverOpponentsAreNamed() = runBlocking {
        val expected = threeDuelsAgainstThreeOpponents()

        val duels = profileReads.recentDuelsOf(alice.id, 10)

        assertEquals(3, duels.size)
        assertEquals(expected.keys.toList(), duels.map { it.duelId })
        assertEquals(expected, duels.associate { it.duelId to it.opponentDisplayName })
    }

    @Test
    fun aListOfThreeDuelsPreparesExactlyOneStatement() = runBlocking {
        threeDuelsAgainstThreeOpponents()
        val countingDataSource = CountingDataSource(dataSource)
        val countingProfileReads = PostgresProfileReads(countingDataSource)

        val duels = countingProfileReads.recentDuelsOf(alice.id, 10)

        assertEquals(3, duels.size)
        assertEquals(1, countingDataSource.statementsPrepared)
    }

    private fun finishedDuel(
        winner: Int?,
        id: UUID = UUID.randomUUID(),
        finishedAt: Instant = Instant.parse("2026-08-13T10:05:00Z"),
        handsPlayed: Int = 1,
        opponent: Player = bob,
    ): FinishedDuel {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = handsPlayed, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = handsPlayed, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = handsPlayed, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        return FinishedDuel(
            id = id,
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:00:00Z"),
            finishedAt = finishedAt,
            seats = listOf(alice.id, opponent.id),
            outcome = outcome,
        )
    }

    /**
     * Records three duels for alice against three distinct opponents — bob named Halvard, carol
     * named Sigrid, dave left unnamed — and returns the newest-first map of duelId to the
     * opponentDisplayName each line must carry. The finishedAt values are chosen so the correct
     * (newest-first) order matches neither the alphabetical order of the opponents' names nor the
     * order the three duels were just recorded in, so an accidental re-sort or a swapped binding
     * would show up as a wrong answer rather than a coincidentally right one.
     */
    private suspend fun threeDuelsAgainstThreeOpponents(): Map<String, String?> {
        val carol = playerDirectory.resolve(DeviceId("carol"))
        val dave = playerDirectory.resolve(DeviceId("dave"))
        setPlayerDisplayName(bob.id.value, "Halvard")
        setPlayerDisplayName(carol.id.value, "Sigrid")

        val bobDuelId = UUID.randomUUID()
        val carolDuelId = UUID.randomUUID()
        val daveDuelId = UUID.randomUUID()

        // Recorded bob, carol, dave — but the newest-first order below is carol, bob, dave.
        // finishedAt must be at or after finishedDuel's default startedAt (10:00:00Z).
        duelResultStore.record(
            finishedDuel(
                winner = 0,
                id = bobDuelId,
                opponent = bob,
                finishedAt = Instant.parse("2026-08-13T10:02:00Z"),
            ),
        )
        duelResultStore.record(
            finishedDuel(
                winner = 0,
                id = carolDuelId,
                opponent = carol,
                finishedAt = Instant.parse("2026-08-13T10:03:00Z"),
            ),
        )
        duelResultStore.record(
            finishedDuel(
                winner = 0,
                id = daveDuelId,
                opponent = dave,
                finishedAt = Instant.parse("2026-08-13T10:01:00Z"),
            ),
        )

        return linkedMapOf(
            carolDuelId.toString() to "Sigrid",
            bobDuelId.toString() to "Halvard",
            daveDuelId.toString() to null,
        )
    }

    private fun playerRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM player").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }
    }

    private fun setPlayerDisplayName(playerId: String, displayName: String?) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE player SET display_name = ? WHERE id = ?::uuid").use { statement ->
                if (displayName == null) {
                    statement.setNull(1, java.sql.Types.VARCHAR)
                } else {
                    statement.setString(1, displayName)
                }
                statement.setString(2, playerId)
                statement.executeUpdate()
            }
        }
    }
}

/**
 * Counts the statements prepared on every connection it hands out, so a test can pin "one round
 * trip" as behaviour rather than as prose about the SQL. Wraps [delegate]'s connections in a
 * dynamic proxy rather than a hand-written decorator, so it tracks [java.sql.Connection] without
 * having to implement every one of its methods.
 */
private class CountingDataSource(private val delegate: DataSource) : DataSource by delegate {
    var statementsPrepared: Int = 0
        private set

    override fun getConnection(): Connection {
        val connection = delegate.connection
        return Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            InvocationHandler { _, method, args ->
                if (method.name == "prepareStatement") statementsPrepared++
                try {
                    method.invoke(connection, *(args ?: emptyArray()))
                } catch (failure: InvocationTargetException) {
                    throw failure.targetException
                }
            },
        ) as Connection
    }
}
