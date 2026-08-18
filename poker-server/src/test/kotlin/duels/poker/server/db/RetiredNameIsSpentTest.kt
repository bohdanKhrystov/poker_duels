package duels.poker.server.db

import duels.poker.server.http.SetNameResult
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * `ADR-0051` §2: blocked, retired and taken are all `409`, and *"no answer says which source
 * refused."* So every refusal in this file asserts exactly [SetNameResult.NameTaken] and a
 * still-`NULL` `display_name` — nothing that would let a caller tell a retired refusal apart from
 * a taken one, because that distinction must not exist on the wire.
 *
 * Both mechanisms under test are the real ones: a claim goes through
 * [PostgresProfileWrites.setDisplayName], the same port the HTTP route calls, and the takedown
 * goes through `SELECT retire_display_name(...)`, exactly as an operator would run it from `psql`
 * (`ADR-0051` §4). No test in this file hand-writes a `RETIRED` row.
 */
class RetiredNameIsSpentTest {
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
    fun theFormerHolderCannotTakeTheirOwnNameBack() {
        val alice = givenAliceRetiredFrom("Ann")

        runBlocking {
            // ADR-0038's "including the player it was taken from" -- the case a naive
            // WHERE NOT EXISTS (... AND player_id <> me) gets wrong.
            val result = profileWrites.setDisplayName(alice, "Ann")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(alice))
        }
    }

    @Test
    fun nobodyElseCanTakeItEither() {
        givenAliceRetiredFrom("Ann")

        runBlocking {
            val bob = playerDirectory.resolve(DeviceId("bob"))

            val result = profileWrites.setDisplayName(bob.id, "Ann")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(bob.id))
        }
    }

    @Test
    fun aDifferentCaseIsTheSameSpentString() {
        givenAliceRetiredFrom("Ann")

        runBlocking {
            val bob = playerDirectory.resolve(DeviceId("bob"))

            // Lower case, never the stored form "Ann". This must fail against any check that
            // compares the stored `name` column with `=` rather than through the fold
            // `name_registry_folded` is built on (ADR-0051 §1) -- that wrong implementation
            // passes the two exact-case refusals above and lets the very next claimant take the
            // retired name back in another case.
            val result = profileWrites.setDisplayName(bob.id, "ann")

            assertEquals(SetNameResult.NameTaken, result)
            assertNull(storedDisplayNameOf(bob.id))
        }
    }

    @Test
    fun theFormerHolderCanTakeADifferentName() {
        val alice = givenAliceRetiredFrom("Ann")

        runBlocking {
            // ADR-0052 §4: nothing is withheld and a new name may be set immediately. Without
            // this test, the four refusals in this file are equally satisfied by a write path
            // that refuses everything.
            val result = profileWrites.setDisplayName(alice, "Bea")

            assertIs<SetNameResult.NameSet>(result)
            assertEquals("Bea", result.profile.displayName)
            assertEquals("Bea", storedDisplayNameOf(alice))
        }
    }

    @Test
    fun aRetiredNameIsStillOneRow() {
        val alice = givenAliceRetiredFrom("Ann")

        runBlocking {
            val bob = playerDirectory.resolve(DeviceId("bob"))
            profileWrites.setDisplayName(alice, "Ann")
            profileWrites.setDisplayName(bob.id, "Ann")
            profileWrites.setDisplayName(bob.id, "ann")
            profileWrites.setDisplayName(alice, "Bea")
        }

        // A refused claim must not have added a second row under a different case, and must not
        // have promoted or demoted this one. Searched under the fold name_registry_folded
        // enforces, not by raw equality on `name` -- a count of one under raw equality on a
        // primary key is a tautology of the schema, true whether or not a same-fold duplicate
        // sits right next to it under a different case. The `name` assertion is what tells "no
        // duplicate" apart from "the canonical row was overwritten by one": either way the fold
        // sees one row, but only the first leaves that row's own `name` still exactly "Ann".
        val rows = registryRowsFoldedTo("Ann")
        assertEquals(1, rows.size)
        assertEquals("Ann", rows.single().name)
        assertEquals("RETIRED", rows.single().reason)
        assertEquals(UUID.fromString(alice.value), rows.single().retiredFrom)
    }

    /**
     * The fixture every test in this file shares: alice resolves, sets [name] through
     * [PostgresProfileWrites.setDisplayName] -- the real write path, not a hand-built `INSERT` --
     * and an operator retires it through `retire_display_name`, exactly as they would from `psql`.
     */
    private fun givenAliceRetiredFrom(name: String): PlayerId {
        val alice =
            runBlocking {
                val player = playerDirectory.resolve(DeviceId("alice"))
                profileWrites.setDisplayName(player.id, name)
                player.id
            }
        retireDisplayName(alice, name)
        return alice
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

    // Searches under the same fold name_registry_folded enforces (ADR-0051 §1), not by raw
    // equality on `name`. Raw equality on a primary key can only ever return zero or one row --
    // that "one row" would be a tautology of the schema, not evidence a same-fold duplicate is
    // absent. This is the query that could actually see one.
    private fun registryRowsFoldedTo(name: String): List<RegistryRow> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT name, reason, retired_from FROM name_registry
                 WHERE lower(name COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu")
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, name)
                statement.executeQuery().use { rows ->
                    val found = mutableListOf<RegistryRow>()
                    while (rows.next()) {
                        found +=
                            RegistryRow(
                                name = rows.getString("name"),
                                reason = rows.getString("reason"),
                                retiredFrom = rows.getObject("retired_from", UUID::class.java),
                            )
                    }
                    found
                }
            }
        }

    private data class RegistryRow(val name: String, val reason: String, val retiredFrom: UUID?)
}
