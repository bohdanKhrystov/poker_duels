package duels.poker.server.db

import duels.poker.server.http.SetNameResult
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * `ADR-0038`'s three sources of truth — names in use, names retired, and the blocklist — are
 * `ADR-0051` §1's one `name_registry` table, distinguished only by `reason` and consulted through
 * one folded index. This file exercises the `BLOCKED` source and the fail-closed requirement
 * `ADR-0038` calls *"the single thing most likely to be got backwards"* — `ADR-0051` §2 answers it
 * structurally, so the unreachable-registry test plants the failure rather than assuming the
 * structural argument holds.
 *
 * Blocklist rows are created with `ADR-0051` §5's own statement, never hand-derived. Every
 * refusal asserts exactly [SetNameResult.NameTaken] and a still-`NULL` `display_name` — nothing
 * that would let a caller tell a blocked refusal apart from a taken or retired one, because
 * `ADR-0051` §2 says no answer may.
 */
class NameBlocklistTest {
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
    fun aBlockedNameIsRefused() {
        insertBlockedName("Slur")
        val alice = resolvePlayer("alice")

        runBlocking {
            val result = profileWrites.setDisplayName(alice, "Slur")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(alice))
        }
    }

    @Test
    fun aBlockedNameIsRefusedInAnotherCase() {
        insertBlockedName("Slur")
        val alice = resolvePlayer("alice")

        runBlocking {
            // "slur", never the blocked row's own case "Slur". This must fail against a screen
            // that folds in the JVM instead of consulting name_registry_folded's ICU fold
            // (ADR-0029 §1) -- the two disagree on exactly the confusable strings a blocklist
            // exists to catch.
            val result = profileWrites.setDisplayName(alice, "slur")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(alice))
        }
    }

    @Test
    fun blockingIsRefusedForANameInUse() {
        val alice = resolvePlayer("alice")
        runBlocking { profileWrites.setDisplayName(alice, "Ann") }

        val conflict = assertFailsWith<SQLException> { insertBlockedName("Ann") }

        assertEquals(UNIQUE_VIOLATION_SQLSTATE, conflict.sqlState)
        // ADR-0051 §5: the schema "refuses to express a third, quieter state" -- alice's row is
        // untouched, not silently reason-flipped to BLOCKED underneath her.
        assertEquals("TAKEN", registryReasonOf("Ann"))
    }

    @Test
    fun aNameAlreadyHeldIsNotRescreened() {
        val alice = resolvePlayer("alice")
        runBlocking { profileWrites.setDisplayName(alice, "Ann") }

        // Blocked only after alice already holds it. ADR-0051 §5: "screening is a set-time event
        // and nothing is re-screened" -- this fails the day somebody adds a job that walks the
        // registry and takes held names away.
        insertBlockedName("Anne")

        assertEquals("Ann", storedDisplayNameOf(alice))
    }

    @Test
    fun aRegistryThatCannotBeReachedRefusesTheName() {
        val alice = resolvePlayer("alice")
        renameRegistryTableUnreachable()

        assertFailsWith<SQLException> {
            runBlocking { profileWrites.setDisplayName(alice, "Fresh") }
        }
        assertNull(storedDisplayNameOf(alice))
    }

    @Test
    fun theRegistryFailureIsRethrownRatherThanTranslated() {
        // "Ghost" is registered -- BLOCKED, so no player ever holds it -- before the registry is
        // renamed away. A fail-open write that swallows the registry failure and falls through to
        // a bare UPDATE would then find "Ghost" already present under the renamed table's
        // constraint and succeed: player_display_name_registered has nothing left to refuse, and
        // player_display_name_unique doesn't fire either, because nobody else holds the name. Only
        // PostgresProfileWrites.kt:43's rethrow -- sqlState != UNIQUE_VIOLATION_SQLSTATE -- stands
        // between the registry being unreachable and alice walking away with a blocked name.
        insertBlockedName("Ghost")
        val alice = resolvePlayer("alice")
        renameRegistryTableUnreachable()

        val failure =
            assertFailsWith<SQLException> {
                runBlocking { profileWrites.setDisplayName(alice, "Ghost") }
            }

        assertEquals(UNDEFINED_TABLE_SQLSTATE, failure.sqlState)
        assertNull(storedDisplayNameOf(alice))
    }

    @Test
    fun eachOfTheThreeReasonsRefusesOnItsOwn() {
        val bob = resolvePlayer("bob")
        runBlocking { profileWrites.setDisplayName(bob, "Held") }

        insertBlockedName("Barred")

        val carol = resolvePlayer("carol")
        runBlocking { profileWrites.setDisplayName(carol, "Gone") }
        retireDisplayName(carol, "Gone")

        // A distinct claimant per string: a shared claimant already holding a name would answer
        // AlreadyNamed for the second and third attempts instead, a different refusal that would
        // hide whichever source ran second and third.
        val dave = resolvePlayer("dave")
        val eve = resolvePlayer("eve")
        val frank = resolvePlayer("frank")
        val grace = resolvePlayer("grace")

        runBlocking {
            assertEquals(SetNameResult.NameTaken, profileWrites.setDisplayName(dave, "Held"))
            assertNull(storedDisplayNameOf(dave))

            assertEquals(SetNameResult.NameTaken, profileWrites.setDisplayName(eve, "Barred"))
            assertNull(storedDisplayNameOf(eve))

            assertEquals(SetNameResult.NameTaken, profileWrites.setDisplayName(frank, "Gone"))
            assertNull(storedDisplayNameOf(frank))

            // The control: without this, the three refusals above are equally satisfied by a
            // write path that refuses every claim regardless of source.
            val freeResult = profileWrites.setDisplayName(grace, "Free")
            assertIs<SetNameResult.NameSet>(freeResult)
            assertEquals("Free", storedDisplayNameOf(grace))
        }
    }

    private fun resolvePlayer(deviceId: String): PlayerId = runBlocking { playerDirectory.resolve(DeviceId(deviceId)).id }

    private fun insertBlockedName(name: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(INSERT_BLOCKED_NAME_SQL).use { statement ->
                statement.setString(1, name)
                statement.executeUpdate()
            }
        }
    }

    // The plant for the fail-closed test. Not REVOKE: the Testcontainers user owns the database,
    // grants do not bind an owner, and a REVOKE-based plant would silently succeed and prove
    // nothing. The foreign key follows the renamed table, so player's constraint stays intact and
    // the only change is that INSERT INTO name_registry no longer resolves.
    private fun renameRegistryTableUnreachable() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("ALTER TABLE name_registry RENAME TO name_registry_unreachable")
            }
        }
    }

    private fun retireDisplayName(playerId: PlayerId, expectedName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT retire_display_name(?, ?)").use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.setString(2, expectedName)
                statement.executeQuery().use { rows -> rows.next() }
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

    private fun registryReasonOf(name: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT reason FROM name_registry WHERE name = ?").use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
            }
        }

    private companion object {
        private const val UNIQUE_VIOLATION_SQLSTATE = "23505"
        private const val UNDEFINED_TABLE_SQLSTATE = "42P01"

        // ADR-0051 §5, verbatim -- do not paraphrase.
        private const val INSERT_BLOCKED_NAME_SQL =
            "INSERT INTO name_registry (name, reason) VALUES (normalize(btrim(?), NFC), 'BLOCKED')"
    }
}
