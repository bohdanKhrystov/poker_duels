package duels.poker.server.db

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
import kotlin.test.assertNull

/**
 * Exercises `retire_display_name` (`V5`) exactly as an operator would: `SELECT
 * retire_display_name(?, ?)` on a plain autocommit connection, never through
 * `PostgresProfileWrites` or any other Kotlin path -- `ADR-0051` §4: *"the server never calls
 * it."* The fixture gives a player their name through `PostgresProfileWrites.setDisplayName`,
 * the path the product itself uses, so a passing test here is a test against a state the product
 * produced, not one this file hand-built with `INSERT`.
 */
class RetireDisplayNameTest {
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
    fun aTakedownLeavesTheProfileWithNoName() {
        val alice = givenPlayerNamed("alice", "Ann")

        retireDisplayName(alice, "Ann")

        // ADR-0051 §4 and ADR-0021 both forbid the server minting a replacement: the profile is
        // left unset, not renamed. ADR-0038's heading sentence says "force-rename", which is
        // exactly the word this assertion refuses to take on faith.
        assertNull(storedDisplayNameOf(alice))
    }

    @Test
    fun aTakedownRetiresTheStringAndRecordsWhoHeldIt() {
        val alice = givenPlayerNamed("alice", "Ann")

        retireDisplayName(alice, "Ann")

        // A takedown that deletes this row instead of retiring it returns "Ann" to the pool --
        // the one outcome ADR-0038 exists to prevent -- and display_name IS NULL alone would not
        // catch it. The row's continued existence, with reason = 'RETIRED', is the assertion
        // that does.
        val row = registryRow("Ann")
        assertEquals("RETIRED", row?.reason)
        assertEquals(UUID.fromString(alice.value), row?.retiredFrom)
    }

    @Test
    fun theFunctionReturnsTheNameItTookAway() {
        val alice = givenPlayerNamed("alice", "Ann")

        // "ANN", not "Ann": the argument must differ from the stored column so that returning it
        // verbatim is visibly wrong. The assertion is the canonical stored form -- what an
        // operator pastes into a record -- not an echo of whatever case they happened to type.
        val returned = retireDisplayName(alice, "ANN")

        assertEquals("Ann", returned)
    }

    @Test
    fun aMismatchedExpectedNameWritesNothing() {
        val alice = givenPlayerNamed("alice", "Ann")

        val failure = assertFailsWith<SQLException> { retireDisplayName(alice, "Bea") }

        assertEquals("23001", failure.sqlState)
        // The interlock (ADR-0051 §4) must raise before writing either of its two rows -- an
        // interlock that raises after one of them is not an interlock.
        assertEquals("Ann", storedDisplayNameOf(alice))
        val row = registryRow("Ann")
        assertEquals("TAKEN", row?.reason)
        assertNull(row?.retiredFrom)
    }

    @Test
    fun theInterlockIsCaseInsensitive() {
        val alice = givenPlayerNamed("alice", "Ann")

        val returned = retireDisplayName(alice, "  aNN ")

        // ADR-0029 §1's fold: trimmed and NFC-normalised before the comparison, so an operator
        // pasting what the row shows them need not reproduce case. Raw equality would refuse
        // this correct takedown at exactly the moment one is needed.
        assertEquals("Ann", returned)
    }

    @Test
    fun aPlayerWithNoNameCannotHaveOneTakenAway() {
        val bob = givenPlayerWithNoName("bob")

        val failure = assertFailsWith<SQLException> { retireDisplayName(bob, "Anything") }

        assertEquals("P0002", failure.sqlState)
        assertNull(storedDisplayNameOf(bob))
    }

    private fun givenPlayerNamed(device: String, name: String): PlayerId {
        val player = runBlocking { playerDirectory.resolve(DeviceId(device)) }
        runBlocking { profileWrites.setDisplayName(player.id, name) }
        return player.id
    }

    private fun givenPlayerWithNoName(device: String): PlayerId =
        runBlocking { playerDirectory.resolve(DeviceId(device)) }.id

    private fun retireDisplayName(playerId: PlayerId, expectedName: String): String =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT retire_display_name(?, ?)").use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.setString(2, expectedName)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getString(1)
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

    private fun registryRow(name: String): RegistryRow? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT reason, retired_from FROM name_registry WHERE name = ?",
            ).use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows ->
                    if (rows.next()) {
                        RegistryRow(
                            reason = rows.getString("reason"),
                            retiredFrom = rows.getObject("retired_from", UUID::class.java),
                        )
                    } else {
                        null
                    }
                }
            }
        }

    private data class RegistryRow(val reason: String, val retiredFrom: UUID?)
}
