package duels.poker.server.season

import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.session.PlayerId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals

/**
 * Proves that the season functions added in STORY-0501 neither read nor write coin balances,
 * nor do they alter duel_result rows. Since `Season` and `seasonOf` are pure functions,
 * the only way to break this test is to add database mutations to the season code — which would
 * be a defect in the ticket that introduced them, not in this test.
 */
class SeasonMovesNoCoinTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun theFixtureItComparesIsNotEmpty() {
        val playerId1 = insertPlayer("device-1", 3)
        val playerId2 = insertPlayer("device-2", -1)
        val duelId = insertDuel()
        insertDuelResult(duelId, playerId1, 1)
        insertDuelResult(duelId, playerId2, -1)

        val playerSnapshot = readPlayerSnapshot()
        val duelResultSnapshot = readDuelResultSnapshot()

        assertEquals(2, playerSnapshot.size, "fixture must have two player rows")
        assertEquals(2, duelResultSnapshot.size, "fixture must have two duel_result rows")
    }

    @Test
    fun noSeasonFunctionMovesACoin() {
        // Insert fixture: two players and one duel with its results.
        val playerId1 = insertPlayer("device-1", 3)
        val playerId2 = insertPlayer("device-2", -1)
        val duelId = insertDuel()
        insertDuelResult(duelId, playerId1, 1)
        insertDuelResult(duelId, playerId2, -1)

        // First snapshot: before any season functions are called.
        val playerSnapshotBefore = readPlayerSnapshot()
        val duelResultSnapshotBefore = readDuelResultSnapshot()

        // Exercise all seven paths added by STORY-0501.
        // Season constructor and toString.
        val season = Season(2026, 8)
        season.toString()

        // Read start and endExclusive properties.
        season.start
        season.endExclusive

        // Call contains on both sides of a boundary.
        val beforeStart = Instant.parse("2026-07-31T23:59:59.999Z")
        val duringMonth = Instant.parse("2026-08-15T12:00:00Z")
        season.contains(beforeStart)
        season.contains(duringMonth)

        // Call seasonOf with the instant and duel.
        val instant = Instant.parse("2026-08-20T12:00:00Z")
        seasonOf(instant)

        val duel = FinishedDuel(
            id = duelId,
            format = "FREEZEOUT",
            startedAt = Instant.parse("2026-08-20T11:00:00Z"),
            finishedAt = Instant.parse("2026-08-20T12:00:00Z"),
            seats = listOf(PlayerId("player1"), PlayerId("player2")),
            outcome = DuelOutcome(
                winner = 0,
                handsPlayed = 5,
                finalStacks = listOf(1500, 500),
            ),
        )
        seasonOf(duel)

        // Second snapshot: after all season functions have been called.
        val playerSnapshotAfter = readPlayerSnapshot()
        val duelResultSnapshotAfter = readDuelResultSnapshot()

        // Assert no coin moved.
        assertEquals(playerSnapshotBefore, playerSnapshotAfter, "player balances changed")
        assertEquals(duelResultSnapshotBefore, duelResultSnapshotAfter, "duel_result rows changed")
    }

    @Test
    fun theSnapshotComparisonDetectsMovedCoins() {
        // Insert fixture: two players and one duel with its results.
        val playerId1 = insertPlayer("device-1", 3)
        val playerId2 = insertPlayer("device-2", -1)
        val duelId = insertDuel()
        insertDuelResult(duelId, playerId1, 1)
        insertDuelResult(duelId, playerId2, -1)

        // First snapshot: before any coin moves.
        val playerSnapshotBefore = readPlayerSnapshot()
        val duelResultSnapshotBefore = readDuelResultSnapshot()

        // Deliberately move a coin to test the detector.
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE player SET coin_balance = coin_balance + 1",
            ).use { statement ->
                statement.executeUpdate()
            }
        }

        // Second snapshot: after moving a coin.
        val playerSnapshotAfter = readPlayerSnapshot()
        val duelResultSnapshotAfter = readDuelResultSnapshot()

        // Assert the comparison detects the coin movement.
        // If this assertion passes, the comparison mechanism is working and would catch
        // any unintended coin movement in noSeasonFunctionMovesACoin.
        assertEquals(
            false,
            playerSnapshotBefore == playerSnapshotAfter,
            "snapshot comparison must detect the coin movement",
        )
    }

    private fun insertPlayer(deviceId: String, coinBalance: Int): UUID {
        val playerId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, coinBalance)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun insertDuel(): UUID {
        val duelId = UUID.randomUUID()
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel (id, format, started_at, finished_at, hands_played) VALUES (?, 'FREEZEOUT', '2026-08-20T11:00:00Z', '2026-08-20T12:00:00Z', 1)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.executeUpdate()
            }
        }
        return duelId
    }

    private fun insertDuelResult(duelId: UUID, playerId: UUID, coinDelta: Int) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, playerId)
                statement.setInt(3, coinDelta)
                statement.executeUpdate()
            }
        }
    }

    private fun readPlayerSnapshot(): List<PlayerRow> {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT id, coin_balance FROM player ORDER BY id",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<PlayerRow>()
                    while (resultSet.next()) {
                        rows.add(
                            PlayerRow(
                                id = resultSet.getObject(1, UUID::class.java),
                                coinBalance = resultSet.getInt(2),
                            ),
                        )
                    }
                    rows
                }
            }
        }
    }

    private fun readDuelResultSnapshot(): List<DuelResultRow> {
        return dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT duel_id, player_id, coin_delta FROM duel_result ORDER BY duel_id, player_id",
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<DuelResultRow>()
                    while (resultSet.next()) {
                        rows.add(
                            DuelResultRow(
                                duelId = resultSet.getObject(1, UUID::class.java),
                                playerId = resultSet.getObject(2, UUID::class.java),
                                coinDelta = resultSet.getInt(3),
                            ),
                        )
                    }
                    rows
                }
            }
        }
    }

    private data class PlayerRow(val id: UUID, val coinBalance: Int)

    private data class DuelResultRow(val duelId: UUID, val playerId: UUID, val coinDelta: Int)
}
