package duels.poker.server.db

import duels.poker.server.http.SetNameResult
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.sql.SQLException
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for [PostgresProfileWrites], against the container.
 *
 * `ADR-0029` §5 is one `UPDATE`; these tests read its three answers off what the database returns
 * or throws, never off a message string. The pair `sendingTheSameNameAgainSucceeds` and
 * `aDifferentCaseOfOwnNameIsRefused` together pin what "identical" means for the idempotent retry:
 * exact equality of the canonical form, not a case fold — the fold is what makes *another*
 * player's name collide (`NameTaken`), never what excuses this player's own.
 */
class PostgresProfileWritesTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var profileWrites: PostgresProfileWrites

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        profileWrites = PostgresProfileWrites(dataSource)
    }

    @Test
    fun anUnnamedPlayerTakesTheName() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))

            val result = profileWrites.setDisplayName(player.id, "Alice")

            assertIs<SetNameResult.NameSet>(result)
            assertEquals("Alice", result.profile.displayName)
            assertEquals("Alice", storedDisplayNameOf(player.id))
        }
    }

    @Test
    fun theReturnedProfileCarriesTheBalanceToo() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            setCoinBalance(player.id, 7)

            val result = profileWrites.setDisplayName(player.id, "Alice")

            assertIs<SetNameResult.NameSet>(result)
            assertEquals(7, result.profile.coinBalance)
        }
    }

    @Test
    fun aNameHeldByAnotherPlayerIsRefused() {
        runBlocking {
            val holder = playerDirectory.resolve(DeviceId("holder"))
            val challenger = playerDirectory.resolve(DeviceId("challenger"))
            setPlayerDisplayName(holder.id, "Ace")

            val result = profileWrites.setDisplayName(challenger.id, "Ace")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(challenger.id))
        }
    }

    @Test
    fun aNameHeldInAnotherCaseIsRefused() {
        runBlocking {
            val holder = playerDirectory.resolve(DeviceId("holder"))
            val challenger = playerDirectory.resolve(DeviceId("challenger"))
            setPlayerDisplayName(holder.id, "Bob")

            val result = profileWrites.setDisplayName(challenger.id, "bob")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(challenger.id))
        }
    }

    @Test
    fun sendingTheSameNameAgainSucceeds() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            profileWrites.setDisplayName(player.id, "Alice")

            val result = profileWrites.setDisplayName(player.id, "Alice")

            assertIs<SetNameResult.NameSet>(result)
            assertEquals("Alice", result.profile.displayName)
            assertEquals("Alice", storedDisplayNameOf(player.id))
        }
    }

    @Test
    fun aDifferentNameForANamedPlayerIsRefused() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            setPlayerDisplayName(player.id, "Alice")

            val result = profileWrites.setDisplayName(player.id, "Alicia")

            assertEquals(SetNameResult.AlreadyNamed, result)
            assertEquals("Alice", storedDisplayNameOf(player.id))
        }
    }

    @Test
    fun aDifferentCaseOfOwnNameIsRefused() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            setPlayerDisplayName(player.id, "Bob")

            val result = profileWrites.setDisplayName(player.id, "bob")

            assertEquals(SetNameResult.NameTaken, result)
            assertEquals("Bob", storedDisplayNameOf(player.id))
        }
    }

    @Test
    fun noSqlExceptionEscapes() {
        runBlocking {
            val holder = playerDirectory.resolve(DeviceId("holder"))
            val challenger = playerDirectory.resolve(DeviceId("challenger"))
            val named = playerDirectory.resolve(DeviceId("named"))
            setPlayerDisplayName(holder.id, "Taken")
            setPlayerDisplayName(named.id, "Original")

            val takenResult = try {
                profileWrites.setDisplayName(challenger.id, "Taken")
            } catch (failure: SQLException) {
                fail("setDisplayName let SQLSTATE ${failure.sqlState} escape instead of returning NameTaken")
            }
            val alreadyNamedResult = try {
                profileWrites.setDisplayName(named.id, "Different")
            } catch (failure: SQLException) {
                fail("setDisplayName let SQLSTATE ${failure.sqlState} escape instead of returning AlreadyNamed")
            }

            assertEquals(SetNameResult.NameTaken, takenResult)
            assertEquals(SetNameResult.AlreadyNamed, alreadyNamedResult)
        }
    }

    // Not the port's held-transaction race (that is TASK-040112) — this proves the same
    // guarantee the ordinary way: real concurrent callers, real Postgres locking, and a count
    // of who actually won. Precedent: PostgresPlayerDirectoryTest's
    // concurrentFirstContactFromManyConnectionsCreatesOneProfile.
    @Test
    @Timeout(60)
    fun twoPlayersRacingForTheSameNameExactlyOneGetsIt() {
        runBlocking(Dispatchers.Default) {
            val first = playerDirectory.resolve(DeviceId("first"))
            val second = playerDirectory.resolve(DeviceId("second"))
            val gate = CompletableDeferred<Unit>()

            val jobs = listOf(first, second).map { player ->
                async {
                    gate.await()
                    profileWrites.setDisplayName(player.id, "Ace")
                }
            }
            gate.complete(Unit)
            val results = jobs.awaitAll()

            assertEquals(1, results.count { it is SetNameResult.NameSet })
            assertEquals(1, results.count { it == SetNameResult.NameTaken })
        }
    }

    // Same precedent, the other axis: one profile, two racing writers. The row lock (not the
    // unique index) is what serialises this one, and exactly one of them still wins.
    @Test
    @Timeout(60)
    fun twoWritersRacingForTheSameProfileExactlyOneWins() {
        runBlocking(Dispatchers.Default) {
            val player = playerDirectory.resolve(DeviceId("shared"))
            val gate = CompletableDeferred<Unit>()

            val jobs = listOf("Ann", "Anna").map { candidateName ->
                async {
                    gate.await()
                    profileWrites.setDisplayName(player.id, candidateName)
                }
            }
            gate.complete(Unit)
            val results = jobs.awaitAll()

            assertEquals(1, results.count { it is SetNameResult.NameSet })
            assertEquals(1, results.count { it == SetNameResult.AlreadyNamed })
            val stored = storedDisplayNameOf(player.id)
            assertTrue(stored == "Ann" || stored == "Anna")
        }
    }

    // ADR-0051 §2's defect, named directly: a registry row left behind by a refused claim burns
    // a string nobody holds, forever. Without the rollback the count below is 1 and the result
    // is still AlreadyNamed — every other assertion in this file stays green either way.
    @Test
    fun aRefusedSecondNameLeavesNoRegistryRow() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            profileWrites.setDisplayName(player.id, "Ann")

            val result = profileWrites.setDisplayName(player.id, "Bea")

            assertEquals(SetNameResult.AlreadyNamed, result)
            assertEquals(0, registryRowCountFor("Bea"))
        }
    }

    // Pins that the idempotent-retry read runs on a clean transaction. Fails against an
    // implementation that reads on the aborted one (25P02 surfaces as a thrown SQLException, not
    // a result) and against one that answers NameTaken for a retry of one's own name.
    @Test
    fun theSameNameAgainIsStillTheSameProfile() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))

            val first = profileWrites.setDisplayName(player.id, "Ann")
            val second = profileWrites.setDisplayName(player.id, "Ann")

            assertIs<SetNameResult.NameSet>(first)
            assertEquals("Ann", first.profile.displayName)
            assertIs<SetNameResult.NameSet>(second)
            assertEquals("Ann", second.profile.displayName)
            assertEquals(1, totalRegistryRowCount())
        }
    }

    // The final claim is what makes "the loser burns nothing" an assertion rather than a
    // sentence: B is refused Cid, costs the registry nothing, and can still claim Dot after.
    @Test
    fun aNameHeldByAnotherPlayerIsRefusedAndCostsNothing() {
        runBlocking {
            val holder = playerDirectory.resolve(DeviceId("holder"))
            val challenger = playerDirectory.resolve(DeviceId("challenger"))
            profileWrites.setDisplayName(holder.id, "Cid")

            val result = profileWrites.setDisplayName(challenger.id, "Cid")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(challenger.id))
            assertEquals(1, registryRowCountFor("Cid"))
            assertEquals("TAKEN", registryReasonFor("Cid"))

            val secondClaim = profileWrites.setDisplayName(challenger.id, "Dot")

            assertIs<SetNameResult.NameSet>(secondClaim)
            assertEquals("Dot", secondClaim.profile.displayName)
        }
    }

    private fun registryRowCountFor(name: String): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM name_registry WHERE name = ?").use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    private fun registryReasonFor(name: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT reason FROM name_registry WHERE name = ?").use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
            }
        }

    private fun totalRegistryRowCount(): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM name_registry").use { statement ->
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    private fun storedDisplayNameOf(playerId: PlayerId): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT display_name FROM player WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getString(1)
                }
            }
        }

    private fun setPlayerDisplayName(playerId: PlayerId, displayName: String?) {
        dataSource.connection.use { connection ->
            if (displayName != null) {
                val registerName = "INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')"
                connection.prepareStatement(registerName).use { statement ->
                    statement.setString(1, displayName)
                    statement.executeUpdate()
                }
            }
            connection.prepareStatement("UPDATE player SET display_name = ? WHERE id = ?").use { statement ->
                if (displayName == null) {
                    statement.setNull(1, Types.VARCHAR)
                } else {
                    statement.setString(1, displayName)
                }
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.executeUpdate()
            }
        }
    }

    private fun setCoinBalance(playerId: PlayerId, balance: Int) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE player SET coin_balance = ? WHERE id = ?").use { statement ->
                statement.setInt(1, balance)
                statement.setObject(2, UUID.fromString(playerId.value))
                statement.executeUpdate()
            }
        }
    }
}
