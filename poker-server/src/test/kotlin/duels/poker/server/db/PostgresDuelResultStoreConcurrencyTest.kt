package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.session.DeviceId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

class PostgresDuelResultStoreConcurrencyTest {
    private lateinit var dataSource: DataSource
    private lateinit var directory: PostgresPlayerDirectory
    private lateinit var store: PostgresDuelResultStore
    private lateinit var alice: duels.poker.server.session.Player
    private lateinit var bob: duels.poker.server.session.Player
    private lateinit var charlie: duels.poker.server.session.Player

    @BeforeEach
    fun setupDatabase() {
        PostgresTestSupport.requireDocker()
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        directory = PostgresPlayerDirectory(dataSource)
        store = PostgresDuelResultStore(dataSource)

        runBlocking {
            alice = directory.resolve(DeviceId("alice"))
            bob = directory.resolve(DeviceId("bob"))
            charlie = directory.resolve(DeviceId("charlie"))
        }
    }

    @Test
    @Timeout(60)
    fun twoDuelsFinishingAtOnceForOnePlayerBothLand() = runBlocking(Dispatchers.Default) {
        // Three profiles: alice (shared winner), bob and charlie (losers)
        val duel1 = finishedDuel(winner = 0, seat0 = alice.id, seat1 = bob.id)
        val duel2 = finishedDuel(winner = 0, seat0 = alice.id, seat1 = charlie.id)

        val gate = CompletableDeferred<Unit>()
        val jobs = listOf(
            async {
                gate.await()
                store.record(duel1)
            },
            async {
                gate.await()
                store.record(duel2)
            },
        )
        gate.complete(Unit)
        jobs.awaitAll()

        // Both duels landed: alice won both, so balance is +2
        assertEquals(2, coinBalanceOf(alice.id))
        assertEquals(-1, coinBalanceOf(bob.id))
        assertEquals(-1, coinBalanceOf(charlie.id))
        assertEquals(2, duelRowCount())
        assertEquals(4, duelResultRowCount())
    }

    @Test
    @Timeout(60)
    fun twentyConcurrentDuelsApplyEveryDelta() = runBlocking(Dispatchers.Default) {
        // Twenty duels between alice (shared winner) and bob (shared loser),
        // alternating which seat alice occupies. This exercises the store's
        // fixed player-id lock order: an implementation that locked the two
        // player rows in seat order would have two transactions taking the
        // same two rows in opposite orders, and PostgreSQL would deadlock.
        val duels = (0 until 20).map { iteration ->
            if (iteration % 2 == 0) {
                // Even: alice in seat 0, bob in seat 1, alice wins
                finishedDuel(winner = 0, seat0 = alice.id, seat1 = bob.id)
            } else {
                // Odd: bob in seat 0, alice in seat 1, alice in seat 1 wins (winner = 1)
                finishedDuel(winner = 1, seat0 = bob.id, seat1 = alice.id)
            }
        }

        val gate = CompletableDeferred<Unit>()
        val jobs = duels.map { duel ->
            async {
                gate.await()
                store.record(duel)
            }
        }
        gate.complete(Unit)
        jobs.awaitAll()

        // All twenty duels landed: alice won all of them
        assertEquals(20, coinBalanceOf(alice.id))
        assertEquals(-20, coinBalanceOf(bob.id))
        assertEquals(20, duelRowCount())
        assertEquals(40, duelResultRowCount())
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

    private fun coinBalanceOf(playerId: duels.poker.server.session.PlayerId): Int {
        return dataSource.connection.use { connection ->
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
    }

    private fun finishedDuel(
        winner: Int?,
        seat0: duels.poker.server.session.PlayerId,
        seat1: duels.poker.server.session.PlayerId,
    ): FinishedDuel {
        val outcome = when (winner) {
            0 -> DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000))
            1 -> DuelOutcome(winner = 1, handsPlayed = 1, finalStacks = listOf(9_000, 11_000))
            null -> DuelOutcome(winner = null, handsPlayed = 1, finalStacks = listOf(10_000, 10_000))
            else -> throw IllegalArgumentException("winner must be 0, 1, or null, got $winner")
        }
        return FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:00:00Z"),
            finishedAt = Instant.parse("2026-08-13T10:05:00Z"),
            seats = listOf(seat0, seat1),
            outcome = outcome,
        )
    }
}
