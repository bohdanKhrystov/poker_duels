package duels.poker.server.db

import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLException
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    @Test
    fun aTakedownMovesNoCoin() {
        val alice = givenPlayerNamed("alice", "Ann")
        val bob = givenPlayerWithNoName("bob")

        // A hand-written duel, not a call into PostgresDuelResultStore: this file's job is the
        // takedown, and the fixture only needs the four rows a finished duel always leaves.
        val duelId = UUID.randomUUID()
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO duel (id, format, started_at, finished_at, hands_played) VALUES (?, ?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setString(2, "heads-up-no-limit")
                statement.setObject(3, now)
                statement.setObject(4, now)
                statement.setInt(5, 1)
                statement.executeUpdate()
            }
            listOf(alice to 1, bob to -1).forEach { (player, delta) ->
                connection.prepareStatement(
                    "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
                ).use { statement ->
                    statement.setObject(1, duelId)
                    statement.setObject(2, UUID.fromString(player.value))
                    statement.setInt(3, delta)
                    statement.executeUpdate()
                }
                connection.prepareStatement("UPDATE player SET coin_balance = coin_balance + ? WHERE id = ?")
                    .use { statement ->
                        statement.setInt(1, delta)
                        statement.setObject(2, UUID.fromString(player.value))
                        statement.executeUpdate()
                    }
            }
        }

        // ADR-0030 §5's own trap: P1 and P2 both hold on an empty database. So the duel's take is
        // asserted by value -- +1, -1, two duel_result rows -- before the takedown runs at all,
        // not only after.
        assertFixtureTook(alice, bob)
        assertEquals(0, p1BrokenBalanceCount())
        val before = p2LedgerSums()
        assertEquals(0, before.playerBalanceSum)
        assertEquals(0, before.duelResultDeltaSum)

        retireDisplayName(alice, "Ann")

        // ADR-0051 §7 adds a fourth writer of `player`, a function nobody here reviews by reading
        // Kotlin. These are exactly the two properties a second SET in its UPDATE would break --
        // and the balances are checked by value, not merely unchanged, because a change that
        // decrements both players together would still leave P1 and P2 holding.
        assertEquals(0, p1BrokenBalanceCount())
        val after = p2LedgerSums()
        assertEquals(0, after.playerBalanceSum)
        assertEquals(0, after.duelResultDeltaSum)
        assertFixtureTook(alice, bob)
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

    /**
     * Asserts the duel [aTakedownMovesNoCoin] writes above actually landed: [winner]'s
     * `coin_balance` is `+1`, [loser]'s is `-1`, and there are exactly two `duel_result` rows.
     * Without this, P1 and P2 below could be evaluated against a fixture that silently failed to
     * write -- and both properties hold trivially on no data, which is the trap this test guards
     * against.
     */
    private fun assertFixtureTook(winner: PlayerId, loser: PlayerId) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) FROM duel_result").use { rows ->
                    rows.next()
                    assertEquals(2L, rows.getLong(1))
                }
            }
            connection.prepareStatement("SELECT coin_balance FROM player WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(winner.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    assertEquals(1, rows.getInt(1))
                }
            }
            connection.prepareStatement("SELECT coin_balance FROM player WHERE id = ?").use { statement ->
                statement.setObject(1, UUID.fromString(loser.value))
                statement.executeQuery().use { rows ->
                    rows.next()
                    assertEquals(-1, rows.getInt(1))
                }
            }
        }
    }

    /**
     * P1 of `ADR-0030` §5, run with the exact SQL the ADR gives. Every row this returns names a
     * player whose `coin_balance` no longer equals the sum of their `duel_result` deltas; P1 holds
     * when this returns zero rows.
     */
    private fun p1BrokenBalanceCount(): Int =
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT p.id FROM player p
                    LEFT JOIN duel_result r ON r.player_id = p.id
                    GROUP BY p.id, p.coin_balance
                    HAVING p.coin_balance <> COALESCE(SUM(r.coin_delta), 0)
                    """.trimIndent(),
                ).use { rows ->
                    var count = 0
                    while (rows.next()) count++
                    count
                }
            }
        }

    /** The two sums P2 of `ADR-0030` §5 requires to both be `0`. */
    private data class LedgerSums(val playerBalanceSum: Int, val duelResultDeltaSum: Int)

    /** P2 of `ADR-0030` §5, run with the exact SQL the ADR gives. */
    private fun p2LedgerSums(): LedgerSums =
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT (SELECT COALESCE(SUM(coin_balance), 0) FROM player),
                           (SELECT COALESCE(SUM(coin_delta), 0) FROM duel_result)
                    """.trimIndent(),
                ).use { rows ->
                    rows.next()
                    LedgerSums(rows.getInt(1), rows.getInt(2))
                }
            }
        }
}
