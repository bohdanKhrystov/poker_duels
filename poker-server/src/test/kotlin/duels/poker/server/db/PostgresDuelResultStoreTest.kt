package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

class PostgresDuelResultStoreTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory
    private lateinit var store: PostgresDuelResultStore
    private lateinit var alice: duels.poker.server.session.Player
    private lateinit var bob: duels.poker.server.session.Player

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

    @Test
    fun recordingAFinishedDuelWritesOneDuelRow() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(1, duelRowCount())
        val format = duelFormatOf(duel.id)
        assertEquals("FREEZEOUT", format)
    }

    @Test
    fun recordingAFinishedDuelWritesOneResultRowPerSeat() = runBlocking {
        val duel = finishedDuel(winner = 0)

        store.record(duel)

        assertEquals(2, duelResultRowCount())
        assertEquals(1, resultDeltaOf(duel.id, alice.id))
        assertEquals(-1, resultDeltaOf(duel.id, bob.id))
    }

    private fun duelRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun duelResultRowCount(): Int {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM duel_result").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
    }

    private fun duelFormatOf(duelId: UUID): String {
        return dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT format FROM duel WHERE id = ?").use { statement ->
                statement.setObject(1, duelId)
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getString(1)
                }
            }
        }
    }

    private fun resultDeltaOf(duelId: UUID, playerId: PlayerId): Int {
        return dataSource.connection.use { connection ->
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
    }

    private fun finishedDuel(winner: Int?, id: UUID = UUID.randomUUID()): FinishedDuel {
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
            finishedAt = Instant.parse("2026-08-13T10:05:00Z"),
            seats = listOf(alice.id, bob.id),
            outcome = outcome,
        )
    }
}
