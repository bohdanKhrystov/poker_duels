package duels.poker.server.db

import duels.poker.engine.duel.DuelFormat
import duels.poker.engine.duel.DuelOutcome
import duels.poker.server.duel.FinishedDuel
import duels.poker.server.duel.formatLabel
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.DeviceId
import duels.poker.server.session.Player
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `ADR-0053` §6's bundle: one guarantee — *nothing about a takedown reaches anybody but the
 * player it happened to* — asserted once behaviourally and three times structurally, sharing one
 * directory sweep.
 *
 * [aRemovedNameLooksExactlyLikeANameNeverSet] proves the behaviour. [retiredFromIsReadInExactlyOneFile]
 * and [retireDisplayNameIsCalledByNoProductionCode] prove the two strings that make the leak
 * possible — `retired_from` (`ADR-0053` §4.2) and `retire_display_name` (`ADR-0051` §4) — live
 * where those decisions say they must, each as a set of matching file names.
 * [retiredFromAppearsExactlyTwiceInPostgresProfileReads] adds what a file-name set cannot see: a
 * second `retired_from` landing inside the one file already expected to match.
 */
class TakedownIsInvisibleTest {
    /**
     * One database: alice beats bob, alice beats carol. Bob set `"Ann"` and an operator retired
     * it through `retire_display_name`, the real takedown path (`RetireDisplayNameTest`); carol
     * never set a name. Both fixtures have to sit in one database for the comparison to mean
     * anything — two tests with one fixture each would pass however the two `null` states are
     * produced.
     *
     * The two lines are compared as encoded JSON, not property by property: a `nameRemoved`
     * badge added to [DuelSummaryResponse] later would fail this test, and an equality on
     * `opponentDisplayName` alone would not notice it. The `assertNull` calls that follow are not
     * redundant with that equality — two lines could be equal and both wrongly carry a name.
     */
    @Test
    fun aRemovedNameLooksExactlyLikeANameNeverSet() = runBlocking {
        val dataSource = PostgresTestSupport.freshDatabase()
        Migrations.migrate(dataSource)
        val playerDirectory = PostgresPlayerDirectory(dataSource)
        val profileReads = PostgresProfileReads(dataSource)
        val profileWrites = PostgresProfileWrites(dataSource)
        val duelResultStore = PostgresDuelResultStore(dataSource)

        val alice = playerDirectory.resolve(DeviceId("alice"))
        val bob = playerDirectory.resolve(DeviceId("bob"))
        val carol = playerDirectory.resolve(DeviceId("carol"))

        profileWrites.setDisplayName(bob.id, "Ann")
        retireDisplayName(dataSource, bob.id, "Ann")

        duelResultStore.record(aliceBeats(alice, bob, Instant.parse("2026-08-13T10:01:00Z")))
        duelResultStore.record(aliceBeats(alice, carol, Instant.parse("2026-08-13T10:02:00Z")))

        val duels = profileReads.recentDuelsOf(alice.id, 10)
        val bobLine = duels.single { it.opponentPlayerId == bob.id.value }
        val carolLine = duels.single { it.opponentPlayerId == carol.id.value }

        assertEquals(
            protocolJson.encodeToString(bobLine.normalised()),
            protocolJson.encodeToString(carolLine.normalised()),
        )
        assertNull(bobLine.opponentDisplayName)
        assertNull(carolLine.opponentDisplayName)
    }

    /**
     * `ADR-0053` §4.2's hazard, named: `RECENT_DUELS_SQL` already joins the *opponent's* `player`
     * row as `p`, so the same correlated `EXISTS` pasted there compiles, runs, and publishes a
     * takedown to a stranger. `retired_from` may appear in exactly one file under
     * `poker-server/src/main/kotlin`, and it is `PostgresProfileReads.kt`.
     */
    @Test
    fun retiredFromIsReadInExactlyOneFile() {
        val matching = sweepMainSource().filter { it.readText().contains("retired_from") }.map { it.name }.toSet()

        assertEquals(setOf("PostgresProfileReads.kt"), matching)
    }

    /**
     * A blunt second guard on the one hazard `ADR-0053` §4.2 names by hand.
     * [retiredFromIsReadInExactlyOneFile] counts files, not occurrences, so it cannot see a
     * second reference land inside a file it already expects to match — which is exactly what
     * happens if `profileOf`'s correlated `EXISTS` is pasted into `DUEL_LINES`: the file set
     * stays `{PostgresProfileReads.kt}`, unchanged, while the leak ships.
     *
     * Today's count is two, and this pins it: the partial index's name in the companion object's
     * comment (`name_registry_retired_from`) and the column reference in `PROFILE_OF_SQL`
     * (`r.retired_from = p.id`). This is not a ban on a third occurrence — a comment, a rename —
     * it is a forced stop. Raise the number only after checking the new occurrence is not inside
     * `DUEL_LINES` or `RECENT_DUELS_SQL`, which is the one place a third occurrence stops being
     * harmless.
     */
    @Test
    fun retiredFromAppearsExactlyTwiceInPostgresProfileReads() {
        val postgresProfileReads = sweepMainSource().single { it.name == "PostgresProfileReads.kt" }

        val occurrences = postgresProfileReads.readText().split("retired_from").size - 1

        assertEquals(2, occurrences)
    }

    /**
     * `ADR-0051` §4: the server never calls `retire_display_name`. No Kotlin references it, no
     * port exposes it, and there is no Gradle task — the operator path is `psql`.
     */
    @Test
    fun retireDisplayNameIsCalledByNoProductionCode() {
        val matching =
            sweepMainSource().filter { it.readText().contains("retire_display_name") }.map { it.name }.toSet()

        assertEquals(emptySet<String>(), matching)
    }

    private fun DuelSummaryResponse.normalised() = copy(duelId = "d", opponentPlayerId = "p", finishedAt = "t")

    private fun aliceBeats(alice: Player, opponent: Player, finishedAt: Instant): FinishedDuel =
        FinishedDuel(
            id = UUID.randomUUID(),
            format = formatLabel(DuelFormat.DEFAULT),
            startedAt = Instant.parse("2026-08-13T10:00:00Z"),
            finishedAt = finishedAt,
            seats = listOf(alice.id, opponent.id),
            outcome = DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(11_000, 9_000)),
        )

    /** `SELECT retire_display_name(?, ?)` on a plain connection, exactly as an operator would. */
    private fun retireDisplayName(dataSource: DataSource, playerId: PlayerId, expectedName: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT retire_display_name(?, ?)").use { statement ->
                statement.setObject(1, UUID.fromString(playerId.value))
                statement.setString(2, expectedName)
                statement.executeQuery().use { rows -> rows.next() }
            }
        }
    }

    /**
     * Every `.kt` file under `poker-server/src/main/kotlin`, shared by all three structural tests
     * so none can drift from the others about what counts as "the sweep". Asserted non-empty and
     * known to include [PostgresProfileReads]'s file before either caller draws a conclusion from
     * a match: a universal claim over a directory that was never found — for instance because the
     * working directory Gradle chose was not anticipated — holds vacuously, and a passing test
     * that proves nothing is worse than a failing one.
     */
    private fun sweepMainSource(): List<File> {
        val root = mainSourceDirectory()
        val kotlinFiles = root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        assertTrue(
            kotlinFiles.size >= 50,
            "expected at least 50 .kt files under $root, found ${kotlinFiles.size} — the sweep found the wrong directory",
        )
        assertTrue(
            kotlinFiles.any { it.name == "PostgresProfileReads.kt" },
            "PostgresProfileReads.kt was not found under $root — the sweep found the wrong directory",
        )

        return kotlinFiles
    }

    /**
     * Locates `poker-server/src/main/kotlin` the way `HttpEndpointDocumentationTest` locates
     * `docs/protocol.md`: walk up from `File("")` — Gradle's working directory, whatever it is —
     * until the directory is found, so the sweep is not tied to running `./gradlew` from the
     * repository root specifically.
     */
    private fun mainSourceDirectory(): File {
        var directory = File("").absoluteFile
        while (true) {
            val candidate = File(directory, "poker-server/src/main/kotlin")
            if (candidate.isDirectory) return candidate
            directory = directory.parentFile ?: return candidate
        }
    }
}
