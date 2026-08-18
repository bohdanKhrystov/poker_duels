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
import java.sql.SQLException
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DisplayNamePermanenceTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
    }

    @Test
    fun anUnnamedProfileCanBeNamed() {
        val playerId = insertPlayerWithoutName()

        updatePlayerName(playerId, "bob")

        assertEquals("bob", readDisplayName(playerId))
    }

    @Test
    fun aNamedProfileCannotBeRenamed() {
        val playerId = insertPlayerWithName("bob")

        val exception = assertFailsWith<SQLException> {
            forceWriteName(playerId, "robert")
        }

        assertEquals("23001", exception.sqlState)
        assertEquals("bob", readDisplayName(playerId))
    }

    @Test
    fun aNamedProfileCannotBeUnnamed() {
        val playerId = insertPlayerWithName("bob")

        val exception = assertFailsWith<SQLException> {
            clearPlayerName(playerId)
        }

        assertEquals("23001", exception.sqlState)
        assertEquals("bob", readDisplayName(playerId))
    }

    @Test
    fun aRenameThatOnlyChangesCaseIsRefused() {
        val playerId = insertPlayerWithName("Bob")

        val exception = assertFailsWith<SQLException> {
            forceWriteName(playerId, "bob")
        }

        assertEquals("23001", exception.sqlState)
        assertEquals("Bob", readDisplayName(playerId))
    }

    @Test
    fun writingTheIdenticalNameChangesNothing() {
        val playerId = insertPlayerWithName("bob")

        forceWriteName(playerId, "bob")

        assertEquals("bob", readDisplayName(playerId))
    }

    @Test
    fun anInsertCarryingANameIsNotAViolation() {
        val playerId = insertPlayerWithName("bob")

        assertEquals("bob", readDisplayName(playerId))
    }

    @Test
    fun theCoinWriteDoesNotFireTheTrigger() {
        runBlocking {
            val directory = PostgresPlayerDirectory(dataSource)
            val store = PostgresDuelResultStore(dataSource)
            val alice = directory.resolve(DeviceId("alice"))
            val bob = directory.resolve(DeviceId("bob"))
            // The player must already hold a name: the coin write is a plain UPDATE of
            // coin_balance, and the trigger must stay silent on it precisely because that
            // UPDATE's SET list never names display_name — not because the row is nameless.
            updatePlayerName(UUID.fromString(alice.id.value), "alice")

            val duel = FinishedDuel(
                id = UUID.randomUUID(),
                format = formatLabel(DuelFormat.DEFAULT),
                startedAt = Instant.parse("2026-08-13T10:00:00Z"),
                finishedAt = Instant.parse("2026-08-13T10:05:00Z"),
                seats = listOf(alice.id, bob.id),
                outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
            )

            store.record(duel)

            assertEquals(1, coinBalanceOf(alice.id))
            assertEquals("alice", readDisplayName(UUID.fromString(alice.id.value)))
        }
    }

    @Test
    fun aRetiredNameMayBeGivenUp() {
        val playerId = insertPlayerWithName("bob")
        retireRegistryRow("bob", playerId)

        clearPlayerName(playerId)

        assertEquals(null, readDisplayName(playerId))
    }

    @Test
    fun aNameThatIsNotRetiredStillCannotBeGivenUp() {
        val playerId = insertPlayerWithName("bob")

        val exception = assertFailsWith<SQLException> {
            clearPlayerName(playerId)
        }

        assertEquals("23001", exception.sqlState)
        assertEquals("bob", readDisplayName(playerId))
    }

    // The permitted transition is name -> NULL when the old name is RETIRED, not "the old name is
    // RETIRED": an exception written the weaker way would let this write through, handing a
    // retired profile a name it never chose -- the one thing ADR-0038 forbids. Registering
    // "robert" first means a buggy trigger that allowed the write would be caught by this
    // assertion, not incidentally saved by an unrelated constraint.
    @Test
    fun aRetiredNameStillCannotBecomeADifferentName() {
        val playerId = insertPlayerWithName("bob")
        retireRegistryRow("bob", playerId)
        registerName("robert")

        val exception = assertFailsWith<SQLException> {
            forceWriteName(playerId, "robert")
        }

        assertEquals("23001", exception.sqlState)
        assertEquals("bob", readDisplayName(playerId))
    }

    private fun insertPlayerWithName(displayName: String): UUID {
        val playerId = UUID.randomUUID()
        val deviceId = "device-${UUID.randomUUID()}"
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance, display_name) VALUES (?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, 100)
                statement.setString(4, displayName)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun insertPlayerWithoutName(): UUID {
        val playerId = UUID.randomUUID()
        val deviceId = "device-${UUID.randomUUID()}"
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO player (id, device_id, coin_balance) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.setString(2, deviceId)
                statement.setInt(3, 100)
                statement.executeUpdate()
            }
        }
        return playerId
    }

    private fun updatePlayerName(playerId: UUID, displayName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "UPDATE player SET display_name = ? WHERE id = ?",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }

    /** Writes display_name without registering it: for the three writes the trigger must refuse. */
    private fun forceWriteName(playerId: UUID, displayName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE player SET display_name = ? WHERE id = ?",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.setObject(2, playerId)
                statement.executeUpdate()
            }
        }
    }

    private fun clearPlayerName(playerId: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE player SET display_name = NULL WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeUpdate()
            }
        }
    }

    /** Registers a name in name_registry without giving it to any player. */
    private fun registerName(displayName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')",
            ).use { statement ->
                statement.setString(1, displayName)
                statement.executeUpdate()
            }
        }
    }

    /**
     * Promotes a registry row to RETIRED by the permitted transition, run directly rather than
     * through retire_display_name: the trigger has to be shown to allow this on its own.
     */
    private fun retireRegistryRow(displayName: String, retiredFrom: UUID) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE name_registry SET reason = 'RETIRED', retired_from = ? WHERE name = ?",
            ).use { statement ->
                statement.setObject(1, retiredFrom)
                statement.setString(2, displayName)
                statement.executeUpdate()
            }
        }
    }

    private fun readDisplayName(playerId: UUID): String? {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT display_name FROM player WHERE id = ?",
            ).use { statement ->
                statement.setObject(1, playerId)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return resultSet.getString(1)
                    }
                }
            }
        }
        return null
    }

    private fun coinBalanceOf(playerId: PlayerId): Int {
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
}
