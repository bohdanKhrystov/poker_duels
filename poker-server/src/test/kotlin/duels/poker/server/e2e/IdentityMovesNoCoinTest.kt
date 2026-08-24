package duels.poker.server.e2e

import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.db.assertCoinInvariantHolds
import duels.poker.server.db.playerTableSnapshot
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.protocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.sql.DataSource

/**
 * `ADR-0030` §5's scenario, the first four steps, against a real database and the shipped
 * composition: connect anonymously, play a duel and win, set a name, sign up.
 *
 * [runScenario] is the one place the whole flow runs, and every test method below calls it and
 * asserts one aspect of the [ScenarioRecord] it returns — never its own copy of the setup or its
 * own re-derivation of the scenario. `dataSource.assertCoinInvariantHolds` runs five times inside
 * it, before the first step and after each of the four, so a coin that moves mid-scenario reddens
 * every method in this class, not only the one whose name happens to describe that step
 * (`ADR-0030` §5: "asserting only at the end is not enough — a mint and a burn cancel").
 *
 * The winner's player id, read over `GET /api/me` right after step 1, is carried through
 * [ScenarioRecord] and re-asserted after every later step, together with the display name step 3
 * sets and the coin balance step 2 pays — so a defect that quietly swapped in a fresh profile
 * partway through, rather than carrying the same one forward, reddens here even though every
 * individual step's own HTTP call still answers success.
 */
@Timeout(120)
internal class IdentityMovesNoCoinTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    @Test
    fun theFirstFourStepsMoveNoCoin() {
        runScenario()
    }

    @Test
    fun theDuelPaidExactlyOneCoinEachWay() {
        val record = runScenario()
        assertEquals(
            1,
            record.winnerBalanceAfterDuel,
            "winner playerId=${record.winnerPlayerId}: expected coinBalance 1 after the duel, got " +
                "${record.winnerBalanceAfterDuel}",
        )
        assertEquals(
            -1,
            record.loserBalanceAfterDuel,
            "loser playerId=${record.loserPlayerId}: expected coinBalance -1 after the duel, got " +
                "${record.loserBalanceAfterDuel}",
        )
    }

    @Test
    fun signingUpLeavesThePlayerTableByteIdentical() {
        val record = runScenario()
        assertEquals(
            record.beforeSigningUp,
            record.afterSigningUp,
            "player table changed across sign-up: before=${record.beforeSigningUp} after=" +
                "${record.afterSigningUp}",
        )
    }

    @Test
    fun settingANameTouchesOneRowAndOneColumn() {
        val record = runScenario()
        val before = record.beforeSettingName
        val after = record.afterSettingName

        assertEquals(
            before.size,
            after.size,
            "player row count changed across setting a name: before=${before.size} rows, after=" +
                "${after.size} rows",
        )

        val changedRows = before.indices.filter { index -> before[index] != after[index] }
        assertEquals(
            1,
            changedRows.size,
            "expected exactly one player row to change when setting a name, changed row indices " +
                "$changedRows",
        )
        val renamedRowIndex = changedRows.single()

        before.indices.filter { it != renamedRowIndex }.forEach { index ->
            assertEquals(
                before[index],
                after[index],
                "row at index $index changed when only the renamed player's row was expected to",
            )
        }

        val renamedBefore = before[renamedRowIndex]
        val renamedAfter = after[renamedRowIndex]
        val changedColumns =
            renamedBefore.indices.filter { position -> renamedBefore[position] != renamedAfter[position] }
        assertEquals(
            1,
            changedColumns.size,
            "expected the renamed player's row to differ in exactly one column, differing " +
                "positions $changedColumns: before=$renamedBefore after=$renamedAfter",
        )
    }

    @Test
    fun theInvariantWasAlreadyRunningBeforeTheFirstStep() {
        val record = runScenario()
        assertEquals(
            0,
            record.playerCountBeforeAnyStep,
            "expected the player table empty before step 1, got ${record.playerCountBeforeAnyStep} rows",
        )
    }

    /**
     * Boots the shipped composition against [dataSource] — `installDuelServer(dataSource)`, then
     * `createClient { install(WebSockets) }`, exactly as every `SocketCoinsTest` test does — and
     * drives `ADR-0030` §5's first four steps over the one client it opens, asserting
     * [dataSource]'s coin invariant before the first step and after each of the four, and the
     * winner's player id, display name and coin balance after every step from the second onward.
     *
     * Every test method in this class calls this and asserts one fact off the returned
     * [ScenarioRecord], so a defect anywhere in the flow reddens every method, not only the one
     * whose name happens to describe the step it broke.
     */
    private fun runScenario(): ScenarioRecord = runBlocking {
        var record: ScenarioRecord? = null
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            val playerCountBeforeAnyStep = dataSource.playerTableSnapshot().size
            dataSource.assertCoinInvariantHolds("before connecting anonymously")

            // Step 1: connect anonymously.
            val duel = client.openSocketDuel()
            val hostPlayerId = client.profileOf(HOST_DEVICE).playerId
            val guestPlayerId = client.profileOf(GUEST_DEVICE).playerId
            dataSource.assertCoinInvariantHolds("after connecting anonymously")

            // Step 2: play a duel and win.
            val outcome = duel.playToFinish()
            val winnerSeat = checkNotNull(outcome.winner) {
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: outcome has no winner, outcome=$outcome"
            }
            val loserSeat = 1 - winnerSeat
            val winner = duel.seat(winnerSeat)
            val loser = duel.seat(loserSeat)

            // The id each device resolved to in step 1, before either client knew who would win —
            // carried forward rather than re-derived, so the checks below are against step 1's own
            // reading and not merely against each other.
            val winnerPlayerId = if (winner.deviceId == HOST_DEVICE) hostPlayerId else guestPlayerId
            val loserPlayerId = if (loser.deviceId == HOST_DEVICE) hostPlayerId else guestPlayerId

            val winnerProfileAfterDuel = client.profileOf(winner.deviceId)
            val loserProfileAfterDuel = client.profileOf(loser.deviceId)
            assertEquals(
                winnerPlayerId,
                winnerProfileAfterDuel.playerId,
                "winner seat=$winnerSeat deviceId=${winner.deviceId}: playerId after the duel " +
                    "(${winnerProfileAfterDuel.playerId}) differs from the playerId read on " +
                    "connecting ($winnerPlayerId)",
            )
            assertEquals(
                loserPlayerId,
                loserProfileAfterDuel.playerId,
                "loser seat=$loserSeat deviceId=${loser.deviceId}: playerId after the duel " +
                    "(${loserProfileAfterDuel.playerId}) differs from the playerId read on " +
                    "connecting ($loserPlayerId)",
            )
            dataSource.assertCoinInvariantHolds("after playing the duel")

            // Step 3: set a name.
            val beforeSettingName = dataSource.playerTableSnapshot()
            val chosenName = "Champion"
            assertEquals(
                HttpStatusCode.OK,
                client.setName(winner.deviceId, chosenName),
                "PUT /api/me/name for the winner's deviceId=${winner.deviceId}",
            )
            dataSource.assertCoinInvariantHolds("after setting a name")
            val afterSettingName = dataSource.playerTableSnapshot()

            val profileAfterSettingName = client.profileOf(winner.deviceId)
            assertEquals(
                winnerPlayerId,
                profileAfterSettingName.playerId,
                "winner deviceId=${winner.deviceId}: playerId changed after setting a name, was " +
                    "$winnerPlayerId, is now ${profileAfterSettingName.playerId}",
            )
            assertEquals(
                chosenName,
                profileAfterSettingName.displayName,
                "winner deviceId=${winner.deviceId}: the name just set did not read back, got " +
                    "${profileAfterSettingName.displayName}",
            )
            assertEquals(
                1,
                profileAfterSettingName.coinBalance,
                "winner deviceId=${winner.deviceId}: the duel's coin did not survive setting a " +
                    "name, coinBalance is now ${profileAfterSettingName.coinBalance}",
            )

            // Step 4: sign up.
            val beforeSigningUp = dataSource.playerTableSnapshot()
            assertEquals(
                HttpStatusCode.Created,
                client.signUp(winner.deviceId, "Winner_1", "password1"),
                "POST /api/auth/sign-up for the winner's deviceId=${winner.deviceId}",
            )
            dataSource.assertCoinInvariantHolds("after signing up")
            val afterSigningUp = dataSource.playerTableSnapshot()

            val profileAfterSigningUp = client.profileOf(winner.deviceId)
            assertEquals(
                winnerPlayerId,
                profileAfterSigningUp.playerId,
                "winner deviceId=${winner.deviceId}: playerId changed after signing up, was " +
                    "$winnerPlayerId, is now ${profileAfterSigningUp.playerId}",
            )
            assertEquals(
                chosenName,
                profileAfterSigningUp.displayName,
                "winner deviceId=${winner.deviceId}: the name set in step 3 did not survive " +
                    "signing up, got ${profileAfterSigningUp.displayName}",
            )
            assertEquals(
                1,
                profileAfterSigningUp.coinBalance,
                "winner deviceId=${winner.deviceId}: the duel's coin did not survive signing up, " +
                    "coinBalance is now ${profileAfterSigningUp.coinBalance}",
            )

            record = ScenarioRecord(
                hostPlayerId = hostPlayerId,
                guestPlayerId = guestPlayerId,
                winnerPlayerId = winnerPlayerId,
                loserPlayerId = loserPlayerId,
                winnerBalanceAfterDuel = winnerProfileAfterDuel.coinBalance,
                loserBalanceAfterDuel = loserProfileAfterDuel.coinBalance,
                playerCountBeforeAnyStep = playerCountBeforeAnyStep,
                beforeSettingName = beforeSettingName,
                afterSettingName = afterSettingName,
                beforeSigningUp = beforeSigningUp,
                afterSigningUp = afterSigningUp,
            )
        }
        checkNotNull(record) { "runScenario: testApplication completed without producing a ScenarioRecord" }
    }

    /**
     * Reads [deviceId]'s profile over `GET /api/me`, asserting the response is `200`, then decodes
     * the body with [protocolJson] — copied from `SocketCoinsTest`, not shared, because a
     * file-private top-level declaration in Kotlin is scoped to the file that declares it.
     */
    private suspend fun HttpClient.profileOf(deviceId: String): ProfileResponse {
        val response = get("/api/me") { header(DEVICE_ID_HEADER, deviceId) }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me for deviceId=$deviceId returned ${response.status}",
        )
        return protocolJson.decodeFromString(response.bodyAsText())
    }

    /** Sets [deviceId]'s display name to [name] over `PUT /api/me/name`, returning the response status. */
    private suspend fun HttpClient.setName(deviceId: String, name: String): HttpStatusCode {
        val response = put("/api/me/name") {
            header(DEVICE_ID_HEADER, deviceId)
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"$name"}""")
        }
        return response.status
    }

    /** Signs [deviceId] up with [handle] and [password] over `POST /api/auth/sign-up`, returning the status. */
    private suspend fun HttpClient.signUp(deviceId: String, handle: String, password: String): HttpStatusCode {
        val response = post("/api/auth/sign-up") {
            header(DEVICE_ID_HEADER, deviceId)
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"$handle","password":"$password"}""")
        }
        return response.status
    }
}

/**
 * What one run of [IdentityMovesNoCoinTest]'s scenario observed at each step, so every test method
 * can assert one aspect of a single run rather than re-deriving the whole scenario itself.
 *
 * @property hostPlayerId The player id `GET /api/me` reported for the host's device right after
 *   step 1.
 * @property guestPlayerId The player id `GET /api/me` reported for the guest's device right after
 *   step 1.
 * @property winnerPlayerId The duel winner's player id, as read for their device in step 1 —
 *   before either client knew who would win — and re-asserted after every step from the second
 *   onward.
 * @property loserPlayerId The duel loser's player id, as read for their device in step 1.
 * @property winnerBalanceAfterDuel The winner's `coinBalance` read over `GET /api/me` right after
 *   step 2.
 * @property loserBalanceAfterDuel The loser's `coinBalance` read over `GET /api/me` right after
 *   step 2.
 * @property playerCountBeforeAnyStep The number of `player` rows before step 1 — zero, against the
 *   fresh database [IdentityMovesNoCoinTest.setup] builds for every test.
 * @property beforeSettingName The `player` table snapshot immediately before step 3.
 * @property afterSettingName The `player` table snapshot immediately after step 3.
 * @property beforeSigningUp The `player` table snapshot immediately before step 4.
 * @property afterSigningUp The `player` table snapshot immediately after step 4.
 */
private data class ScenarioRecord(
    val hostPlayerId: String,
    val guestPlayerId: String,
    val winnerPlayerId: String,
    val loserPlayerId: String,
    val winnerBalanceAfterDuel: Int,
    val loserBalanceAfterDuel: Int,
    val playerCountBeforeAnyStep: Int,
    val beforeSettingName: List<List<Any?>>,
    val afterSettingName: List<List<Any?>>,
    val beforeSigningUp: List<List<Any?>>,
    val afterSigningUp: List<List<Any?>>,
)
