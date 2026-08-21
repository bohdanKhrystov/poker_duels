package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.engine.duel.MatchFinished
import duels.poker.engine.log.MatchLog
import duels.poker.server.duel.DuelResult
import duels.poker.server.duel.DuelResultSink
import duels.poker.server.duel.formatLabel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

class PostgresDuelResultSinkTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory
    private lateinit var store: PostgresDuelResultStore
    private lateinit var alice: duels.poker.server.session.Player
    private lateinit var bob: duels.poker.server.session.Player
    private val systemClock: Clock = Clock.systemUTC()

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
        store = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = directory.resolve(DeviceId("alice"))
            bob = directory.resolve(DeviceId("bob"))
        }
    }

    private fun matchLog(outcome: DuelOutcome, format: DuelFormat = DuelFormat.DEFAULT): MatchLog =
        MatchLog(
            format = format,
            buttonSeat = 0,
            hands = emptyList(),
            events = listOf(MatchFinished(sequence = 0, outcome = outcome)),
        )

    private fun duelResult(winner: Int?, duelId: UUID = UUID.randomUUID()): DuelResult {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        return DuelResult(duelId = duelId, outcome = outcome, seats = listOf(alice.id, bob.id), log = matchLog(outcome))
    }

    private fun duelRowCount(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    private fun duelResultRowCount(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel_result").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    private fun resultDeltaOf(duelId: UUID, playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_delta FROM duel_result WHERE duel_id = ? AND player_id = ?",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    private fun coinBalanceOf(playerId: PlayerId): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT coin_balance FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }

    private fun singleDuelId(): UUID =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT id FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, UUID::class.java)
                }
            }
        }

    private fun duelTimestamps(duelId: UUID): Pair<Instant, Instant> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT started_at, finished_at FROM duel WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getObject(1, java.time.OffsetDateTime::class.java).toInstant() to
                        resultSet.getObject(2, java.time.OffsetDateTime::class.java).toInstant()
                }
            }
        }

    @Test
    fun recordingThroughThePortWritesTheSameRowsAsTheStore() = runBlocking {
        val sink = PostgresDuelResultSink(store, systemClock)

        sink.record(duelResult(winner = 0))

        assertEquals(1, duelRowCount())
        assertEquals(2, duelResultRowCount())
    }

    @Test
    fun theWinnerGainsAndTheLoserLosesThroughThePort() = runBlocking {
        val sink = PostgresDuelResultSink(store, systemClock)

        sink.record(duelResult(winner = 0))

        assertEquals(1, coinBalanceOf(alice.id))
        assertEquals(-1, coinBalanceOf(bob.id))
    }

    @Test
    fun aDrawRecordedThroughThePortWritesTwoZeroRows() = runBlocking {
        val duelId = UUID.randomUUID()
        val sink = PostgresDuelResultSink(store, systemClock)

        sink.record(duelResult(winner = null, duelId = duelId))

        assertEquals(2, duelResultRowCount())
        assertEquals(0, resultDeltaOf(duelId, alice.id))
        assertEquals(0, resultDeltaOf(duelId, bob.id))
        assertEquals(0, coinBalanceOf(alice.id))
        assertEquals(0, coinBalanceOf(bob.id))
    }

    @Test
    fun theStoreIsUsableWhereTheSinkIsExpected() = runBlocking {
        val sink: DuelResultSink = PostgresDuelResultSink(store, systemClock)

        sink.record(duelResult(winner = 1))

        assertEquals(1, duelRowCount())
    }

    @Test
    fun theDuelIdComesFromTheResultAndTimestampsComeFromTheInjectedClock() = runBlocking {
        val fixedId = UUID.randomUUID()
        val fixedInstant = Instant.parse("2026-08-13T09:30:00Z")
        val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
        val sink = PostgresDuelResultSink(store, clock)

        sink.record(duelResult(winner = 0, duelId = fixedId))

        assertEquals(fixedId, singleDuelId())
        val (startedAt, finishedAt) = duelTimestamps(fixedId)
        assertEquals(fixedInstant, startedAt)
        assertEquals(fixedInstant, finishedAt)
    }

    @Test
    fun aRetriedRecordUnderTheSameDuelIdWritesOneRow() = runBlocking {
        val duelId = UUID.randomUUID()
        val sink = PostgresDuelResultSink(store, systemClock)

        sink.record(duelResult(winner = 0, duelId = duelId))
        sink.record(duelResult(winner = 0, duelId = duelId))

        assertEquals(1, duelRowCount())
        assertEquals(2, duelResultRowCount())
        assertEquals(1, coinBalanceOf(alice.id))
        assertEquals(-1, coinBalanceOf(bob.id))
    }

    @Test
    fun theFormatLabelComesFromTheMatchLog() = runBlocking {
        val outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
        val result = DuelResult(
            duelId = UUID.randomUUID(),
            outcome = outcome,
            seats = listOf(alice.id, bob.id),
            log = matchLog(outcome, format = DuelFormat.DEFAULT),
        )
        val sink = PostgresDuelResultSink(store, systemClock)

        sink.record(result)

        val format = dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT format FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
        assertEquals(formatLabel(DuelFormat.DEFAULT), format)
    }
}
