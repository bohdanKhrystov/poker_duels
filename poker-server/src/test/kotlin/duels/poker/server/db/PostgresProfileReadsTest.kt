package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun handsPlayedIsNullWhileTheColumnDoesNotExist() = runBlocking {
        duelResultStore.record(finishedDuel(winner = 0))

        val entry = profileReads.recentDuelsOf(alice.id, 10).single()

        assertNull(entry.handsPlayed)
    }

    private fun finishedDuel(
        winner: Int?,
        id: UUID = UUID.randomUUID(),
        finishedAt: Instant = Instant.parse("2026-08-13T10:05:00Z"),
    ): FinishedDuel {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        return FinishedDuel(
            id = id,
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:00:00Z"),
            finishedAt = finishedAt,
            seats = listOf(alice.id, bob.id),
            outcome = outcome,
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
}
