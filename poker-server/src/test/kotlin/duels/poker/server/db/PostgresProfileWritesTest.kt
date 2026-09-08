package duels.poker.server.db

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
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
 * Setting a name is four statements under one row lock (`ADR-0134` §2), and these tests read
 * their answers off what the database returns or throws, never off a message string. The pair
 * `sendingTheSameNameAgainSucceeds` and `aDifferentCaseOfOwnNameIsRefused` together pin what
 * "identical" means for the idempotent retry: **exact equality of the canonical form, not a case
 * fold**. A case variant of a player's own name is refused, and since `TASK-041004` it is refused
 * as `NameTaken` — the registry's fold index raises `23505` on the second statement, before the
 * `UPDATE` that hands a name over is ever reached. The fold does not excuse a name for belonging
 * to the player who already holds it.
 */
class PostgresProfileWritesTest {
    private lateinit var dataSource: DataSource
    private lateinit var playerDirectory: PostgresPlayerDirectory
    private lateinit var profileWrites: PostgresProfileWrites
    private lateinit var credentials: PostgresCredentials

    @BeforeEach
    fun setupDatabase() {
        dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        playerDirectory = PostgresPlayerDirectory(dataSource)
        profileWrites = PostgresProfileWrites(dataSource)
        credentials = PostgresCredentials(dataSource)
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

    // ADR-0134 §2: a rename is no longer refused. The holder of Alice writing Alicia gets the new
    // string, and Alice's own registry row is the one statement 3 moves to REPLACED — the
    // assertEquals on the reason, not a weaker check on the column alone, is what catches a
    // mechanism that wrote RETIRED instead (What would still pass if the coder got it wrong).
    @Test
    fun aDifferentNameForANamedPlayerReplacesTheOne() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            setPlayerDisplayName(player.id, "Alice")

            val result = profileWrites.setDisplayName(player.id, "Alicia")

            assertIs<SetNameResult.NameSet>(result)
            assertEquals("Alicia", result.profile.displayName)
            assertEquals("Alicia", storedDisplayNameOf(player.id))
            assertEquals("REPLACED", registryReasonFor("Alice"))
        }
    }

    // theReplacedRowRecordsThePlayerThatLeftIt uses two players who disagree, which is what makes
    // it a correlation rather than a constant (What would still pass if the coder got it wrong):
    // a hard-coded retired_from that happens to equal the first player's id would satisfy one row
    // and fail the second.
    @Test
    fun theReplacedRowRecordsThePlayerThatLeftIt() {
        runBlocking {
            val first = playerDirectory.resolve(DeviceId("first"))
            val second = playerDirectory.resolve(DeviceId("second"))
            setPlayerDisplayName(first.id, "Alice")
            setPlayerDisplayName(second.id, "Bea")

            profileWrites.setDisplayName(first.id, "Alicia")
            profileWrites.setDisplayName(second.id, "Bianca")

            assertEquals(first.id.value, retiredFromFor("Alice"))
            assertEquals(second.id.value, retiredFromFor("Bea"))
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
            // ADR-0134 §2: this is now a rename, and it succeeds.
            val renameResult = try {
                profileWrites.setDisplayName(named.id, "Different")
            } catch (failure: SQLException) {
                fail("setDisplayName let SQLSTATE ${failure.sqlState} escape instead of returning NameSet")
            }

            assertEquals(SetNameResult.NameTaken, takenResult)
            assertIs<SetNameResult.NameSet>(renameResult)
            assertEquals("Different", renameResult.profile.displayName)
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

    // Same precedent, the other axis: one profile, two racing writers. ADR-0134 §2: two concurrent
    // PUTs from one player are both legal now. Statement 1's FOR UPDATE (not the unique index) is
    // what serialises this one — the row lock forces the second writer to wait for the first's
    // commit, then read the name the first left and replace that, rather than racing statement 3
    // against a row the first writer already moved off TAKEN. Without the lock this would raise
    // 23001, which is exactly what this test would catch escaping (What would still pass if the
    // coder got it wrong).
    @Test
    @Timeout(60)
    fun twoWritersRacingForTheSameProfileBothSucceedAndOneStringIsLeft() {
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

            assertEquals(2, results.count { it is SetNameResult.NameSet })
            val stored = requireNotNull(storedDisplayNameOf(player.id))
            assertTrue(stored == "Ann" || stored == "Anna")
            val leftBehind = if (stored == "Ann") "Anna" else "Ann"
            assertEquals("TAKEN", registryReasonFor(stored))
            assertEquals("REPLACED", registryReasonFor(leftBehind))
        }
    }

    // ADR-0051 §2's defect, named directly: a registry row left behind by a refused claim burns
    // a string nobody holds, forever. Without the rollback, Ann's own row would have been left
    // REPLACED by the attempt even though the rename failed.
    @Test
    fun aRefusedRenameLeavesBothStringsWhereTheyWere() {
        runBlocking {
            val holder = playerDirectory.resolve(DeviceId("holder"))
            val player = playerDirectory.resolve(DeviceId("alice"))
            setPlayerDisplayName(holder.id, "Bea")
            profileWrites.setDisplayName(player.id, "Ann")

            val result = profileWrites.setDisplayName(player.id, "Bea")

            assertEquals(SetNameResult.NameTaken, result)
            assertEquals("Ann", storedDisplayNameOf(player.id))
            assertEquals("TAKEN", registryReasonFor("Ann"))
            assertEquals(1, registryRowCountFor("Bea"))
            assertEquals("TAKEN", registryReasonFor("Bea"))
        }
    }

    // ADR-0134 §2's rollback, more load-bearing than ADR-0051 §2's: a rename that spends the old
    // string (statement 3) and then fails to land the new one (statement 4) must not strand the
    // player holding a name the registry says is spent — ADR-0051 §9 refuses the un-retire that
    // would repair it. player_display_name_unique is forced by a raw row that already holds the
    // arriving string outside the registry, so statement 2 spends "Bea" cleanly (the registry
    // knows nothing about it) and statement 4 is the one that fails.
    @Test
    fun aRenameThatFailsAfterTheRetirementRollsTheRetirementBack() {
        runBlocking {
            val player = playerDirectory.resolve(DeviceId("alice"))
            val other = playerDirectory.resolve(DeviceId("other"))
            profileWrites.setDisplayName(player.id, "Ann")
            setRawDisplayNameBypassingRegistry(other.id, "Bea")

            val result = try {
                profileWrites.setDisplayName(player.id, "Bea")
            } catch (failure: SQLException) {
                fail("setDisplayName let SQLSTATE ${failure.sqlState} escape instead of returning a refusal")
            }

            assertEquals(SetNameResult.NameTaken, result)
            assertEquals("Ann", storedDisplayNameOf(player.id))
            assertEquals("TAKEN", registryReasonFor("Ann"))
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

    @Test
    fun aNameWriteAnswersTrueForAPlayerHoldingAPassword() {
        runBlocking {
            val withPassword = playerDirectory.resolve(DeviceId("withPassword"))
            val withoutPassword = playerDirectory.resolve(DeviceId("withoutPassword"))

            // Create a password credential for the first player
            credentials.create(
                withPassword.id,
                CredentialKind.PASSWORD,
                "alice@example.com",
                PresentedSecret("password"),
            )

            // Both players set names for the first time
            val resultWithPassword = profileWrites.setDisplayName(withPassword.id, "Alice")
            val resultWithoutPassword = profileWrites.setDisplayName(withoutPassword.id, "Bob")

            // Verify the results
            assertIs<SetNameResult.NameSet>(resultWithPassword)
            assertTrue(resultWithPassword.profile.hasPassword)
            assertIs<SetNameResult.NameSet>(resultWithoutPassword)
            assertEquals(false, resultWithoutPassword.profile.hasPassword)
        }
    }

    @Test
    fun theIdempotentRetryAnswersWithThePasswordFlagToo() {
        runBlocking {
            val withPassword = playerDirectory.resolve(DeviceId("withPassword"))
            val withoutPassword = playerDirectory.resolve(DeviceId("withoutPassword"))

            // Create a password credential for the first player
            credentials.create(
                withPassword.id,
                CredentialKind.PASSWORD,
                "alice@example.com",
                PresentedSecret("password"),
            )

            // Both players set names for the first time
            profileWrites.setDisplayName(withPassword.id, "Alice")
            profileWrites.setDisplayName(withoutPassword.id, "Bob")

            // Both players send the same name again (idempotent retry)
            val retryWithPassword = profileWrites.setDisplayName(withPassword.id, "Alice")
            val retryWithoutPassword = profileWrites.setDisplayName(withoutPassword.id, "Bob")

            // Verify the retry results
            assertIs<SetNameResult.NameSet>(retryWithPassword)
            assertTrue(retryWithPassword.profile.hasPassword)
            assertIs<SetNameResult.NameSet>(retryWithoutPassword)
            assertEquals(false, retryWithoutPassword.profile.hasPassword)
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

    private fun retiredFromFor(name: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT retired_from FROM name_registry WHERE name = ?").use { statement ->
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

    // A raw write, bypassing the registry entirely — the only way to make a player's column hold
    // a string the registry has never spent, which is what forces statement 4 to be the one that
    // fails in aRenameThatFailsAfterTheRetirementRollsTheRetirementBack (ADR-0134 §8's named hole:
    // the trigger governs the string being left, not the one arriving). V6's
    // player_display_name_registered foreign key refuses this directly, so the constraint is
    // dropped first on this test's own, disposable database — the folded player_display_name_unique
    // index this test exercises is untouched by that, so statement 4 still meets it.
    private fun setRawDisplayNameBypassingRegistry(playerId: PlayerId, displayName: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE player DROP CONSTRAINT player_display_name_registered")
            }
            connection.prepareStatement("UPDATE player SET display_name = ? WHERE id = ?").use { statement ->
                statement.setString(1, displayName)
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
