package duels.poker.server.e2e

import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.db.assertCoinInvariantHolds
import duels.poker.server.db.deviceBindingTableSnapshot
import duels.poker.server.db.playerTableSnapshot
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.CreateRoom
import duels.poker.server.protocol.JoinRoom
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.File
import javax.sql.DataSource

/**
 * `ADR-0030` §5's whole scenario, against a real database and the shipped composition: connect
 * anonymously, play a duel and win, set a name, sign up, sign in, reconnect with the token, sign
 * into a second account from the same device, play a duel as that account, sign out, reconnect
 * anonymously, read the profile back, read the ladder, then sign in once more and revoke the
 * device that token was issued to (`ADR-0050`) — the scenario's last step.
 *
 * [runScenario] is the one place the whole flow runs, and every test method below calls it and
 * asserts one aspect of the [ScenarioRecord] it returns — never its own copy of the setup or its
 * own re-derivation of the scenario. `dataSource.assertCoinInvariantHolds` runs fourteen times
 * inside it — before the first step, after each of the eleven, and twice more across the closing
 * sign-in-and-revoke — so a coin that moves mid-scenario reddens every method in this class, not
 * only the one whose name happens to describe that step (`ADR-0030` §5: "asserting only at the
 * end is not enough — a mint and a burn cancel").
 *
 * Steps five to eleven are written against `winner`/`loser`, never against the wire-level
 * `HOST_DEVICE`/`GUEST_DEVICE` constants: whichever device wins step 1's duel is the one with a
 * credential once step 4 runs, so it is *that* device — "the host" throughout the rest of this
 * scenario, in the role sense the story's steps use the word, not the connection-order sense
 * `HOST_DEVICE` names — that signs in first and carries the token. The loser signs up under its
 * own handle here, becoming the "second account" step 7 signs into from the winner's device.
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
    fun theWholeScenarioMovesNoCoin() {
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

    @Test
    fun theSecondAccountIsSeatedFromTheFirstsDevice() {
        val record = runScenario()
        assertEquals(
            record.winnerPlayerId,
            record.step6WelcomePlayerId,
            "step 6: Hello carrying the winner's own token named ${record.step6WelcomePlayerId}, " +
                "expected the winner's own player ${record.winnerPlayerId}",
        )
        assertEquals(
            record.loserPlayerId,
            record.step7WelcomePlayerId,
            "step 7: Hello carrying the second account's token, sent from the winner's device, " +
                "named ${record.step7WelcomePlayerId}, expected the second account's player " +
                "${record.loserPlayerId}",
        )
    }

    @Test
    fun theSecondDuelPaidTheAccountAndNotTheDevice() {
        val record = runScenario()
        val expectedSecondAccountDelta = if (record.secondDuelWonByTheAccount) 1 else -1
        assertEquals(
            record.guestAccountBalanceBeforeSecondDuel + expectedSecondAccountDelta,
            record.guestAccountBalanceAfterSecondDuel,
            "second account playerId=${record.loserPlayerId}: expected coinBalance to move by " +
                "$expectedSecondAccountDelta across the second duel, was " +
                "${record.guestAccountBalanceBeforeSecondDuel}, is now " +
                "${record.guestAccountBalanceAfterSecondDuel}",
        )
        assertEquals(
            record.winnerBalanceAfterDuel,
            record.hostBalanceAfterSecondDuel,
            "winner's own deviceId, playerId=${record.winnerPlayerId}: expected coinBalance to " +
                "stay at ${record.winnerBalanceAfterDuel} (its value at the end of step 2) across " +
                "the second duel, is now ${record.hostBalanceAfterSecondDuel}",
        )
    }

    @Test
    fun signingInAndOutLeavesThePlayerTableByteIdentical() {
        val record = runScenario()
        assertEquals(
            record.beforeSigningIn,
            record.afterSigningIn,
            "player table changed across signing in: before=${record.beforeSigningIn} after=" +
                "${record.afterSigningIn}",
        )
        assertEquals(
            record.beforeSigningIntoSecondAccount,
            record.afterSigningIntoSecondAccount,
            "player table changed across signing into the second account: before=" +
                "${record.beforeSigningIntoSecondAccount} after=${record.afterSigningIntoSecondAccount}",
        )
        assertEquals(
            record.beforeSigningOut,
            record.afterSigningOut,
            "player table changed across signing out: before=${record.beforeSigningOut} after=" +
                "${record.afterSigningOut}",
        )
        assertEquals(
            record.beforeReconnectingAnonymously,
            record.afterReconnectingAnonymously,
            "player table changed across reconnecting anonymously: before=" +
                "${record.beforeReconnectingAnonymously} after=${record.afterReconnectingAnonymously}",
        )
    }

    @Test
    fun theDeviceIsItselfAgainAfterSigningOut() {
        val record = runScenario()
        assertEquals(
            record.winnerPlayerId,
            record.step10WelcomePlayerId,
            "step 10: Welcome named ${record.step10WelcomePlayerId}, expected the winner's " +
                "original player ${record.winnerPlayerId}, the one recorded at step 1",
        )
        assertEquals(
            record.winnerPlayerId,
            record.step11ProfilePlayerId,
            "step 11: GET /api/me named ${record.step11ProfilePlayerId}, expected the winner's " +
                "original player ${record.winnerPlayerId}, the one recorded at step 1",
        )
    }

    @Test
    fun revokingLeavesThePlayerTableByteIdentical() {
        val record = runScenario()
        assertEquals(
            record.beforeRevoking,
            record.afterRevoking,
            "player table changed across revoking: before=${record.beforeRevoking} after=" +
                "${record.afterRevoking}",
        )
    }

    /**
     * The positive control [revokingLeavesThePlayerTableByteIdentical] needs beside it: revoking
     * writes nothing to `player` (`ADR-0050` §1), so that test alone cannot tell a `DELETE` that
     * genuinely revoked the device apart from one that silently did nothing — both leave `player`
     * untouched. This asserts the write landed somewhere: exactly one `device_binding` row
     * changes, and within it exactly one column, `revoked_at`.
     */
    @Test
    fun revokingChangesExactlyOneBindingColumn() {
        val record = runScenario()
        val before = record.deviceBindingBeforeRevoking
        val after = record.deviceBindingAfterRevoking

        assertEquals(
            before.size,
            after.size,
            "device_binding row count changed across revoking: before=${before.size} rows, " +
                "after=${after.size} rows",
        )

        val changedRows = before.indices.filter { index -> before[index] != after[index] }
        assertEquals(
            1,
            changedRows.size,
            "expected exactly one device_binding row to change when revoking, changed row " +
                "indices $changedRows",
        )
        val revokedRowIndex = changedRows.single()

        val beforeRow = before[revokedRowIndex]
        val afterRow = after[revokedRowIndex]
        val changedColumns = beforeRow.indices.filter { position -> beforeRow[position] != afterRow[position] }
        assertEquals(
            1,
            changedColumns.size,
            "expected the revoked row to differ in exactly one column, differing positions " +
                "$changedColumns: before=$beforeRow after=$afterRow",
        )
        val changedColumnName = record.deviceBindingColumnNames[changedColumns.single()]
        assertEquals(
            "revoked_at",
            changedColumnName,
            "expected the one changed device_binding column to be revoked_at, was $changedColumnName",
        )
    }

    /**
     * Reads every `/api/…` string literal out of the four `poker-server/src/main/kotlin/duels/poker/server/http`
     * files whose names end `Routes.kt`, and asserts that set equals [SCENARIO_ENDPOINTS] — the
     * write path `ADR-0030` §5's "total over the schema" claim needs to hold at the endpoint
     * level, not only at the row level: the coin properties are total over `player` and
     * `duel_result`, but a new endpoint nobody adds to [SCENARIO_ENDPOINTS] is a write path the
     * invariant checks in this class never reach, however total those checks are over the tables
     * they do reach. Adding a new `/api/…` route makes this test fail the build until either the
     * scenario calls it or [SCENARIO_ENDPOINTS] is extended with a comment recording why it does
     * not move a coin.
     *
     * Two honest limits, named here rather than left for a reader to find:
     * - This reads source text, not the compiled routing table, so a path assembled from
     *   constants rather than written as a literal escapes it. Every route in the repository
     *   today writes its path as a literal, and this test is the reason to keep doing so.
     * - This says the scenario *calls* each path, not that it calls it in a state where a defect
     *   would show. That is left to a reviewer's judgement.
     */
    @Test
    fun everyApiPathInTheRouteSourcesIsExercisedByTheScenario() {
        val sourcePaths = apiPathLiteralsInRouteSources()
        assertEquals(
            SCENARIO_ENDPOINTS,
            sourcePaths,
            "SCENARIO_ENDPOINTS ($SCENARIO_ENDPOINTS) must equal the /api/… literals found in the " +
                "four *Routes.kt files ($sourcePaths): a path in one and not the other is either a " +
                "route the scenario does not account for, or a stale entry naming a route that no " +
                "longer exists",
        )
    }

    /**
     * The vacuity guard for [everyApiPathInTheRouteSourcesIsExercisedByTheScenario]: asserts
     * against the *scanned* set, never against [SCENARIO_ENDPOINTS], because a regular expression
     * that matched nothing would otherwise make that test pass with both sides empty.
     */
    @Test
    fun theEnumerationFoundTheEndpointsItIsChecking() {
        val sourcePaths = apiPathLiteralsInRouteSources()
        assertTrue(
            sourcePaths.isNotEmpty(),
            "expected the route-source scan to find at least one /api/… literal, found none",
        )
        assertTrue(
            setOf("/api/me/device", "/api/auth/sign-in", "/api/me").all { it in sourcePaths },
            "expected the route-source scan to find /api/me/device, /api/auth/sign-in and /api/me, " +
                "found $sourcePaths",
        )
    }

    /**
     * Boots the shipped composition against [dataSource] — `installDuelServer(dataSource)`, then
     * `createClient { install(WebSockets) }`, exactly as every `SocketCoinsTest` test does — and
     * drives `ADR-0030` §5's whole eleven-step scenario over the one client it opens, asserting
     * [dataSource]'s coin invariant before the first step and after each of the eleven, and the
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

            // The scenario's "second account": whichever device did not win step 1's duel holds
            // no credential yet, so it signs up under its own handle here — the only change to
            // the first four steps (`TASK-040620`). This is what step 7 later signs into from the
            // winner's own device.
            assertEquals(
                HttpStatusCode.Created,
                client.signUp(loser.deviceId, "Second_2", "password2"),
                "POST /api/auth/sign-up for the loser's deviceId=${loser.deviceId}",
            )

            // Step 5: sign in as the winner's own account.
            val beforeSigningIn = dataSource.playerTableSnapshot()
            val winnerToken = client.signIn("Winner_1", "password1")
            dataSource.assertCoinInvariantHolds("after signing in")
            val afterSigningIn = dataSource.playerTableSnapshot()

            // Step 6: reconnect with the token, from the winner's own device — device id and
            // token agree, so the Welcome names the same player either way.
            val step6Welcome = client.webSocketSession("/ws").completeHandshake(winner.deviceId, winnerToken)
            dataSource.assertCoinInvariantHolds("after reconnecting with the token")

            // Step 7: sign into the second account from the same device — the winner's own
            // device presents the second account's token, which `ADR-0030` §6 makes legal, and
            // this step is the whole reason the scenario exists.
            val beforeSigningIntoSecondAccount = dataSource.playerTableSnapshot()
            val secondAccountToken = client.signIn("Second_2", "password2")
            val step7Welcome = client.webSocketSession("/ws").completeHandshake(winner.deviceId, secondAccountToken)
            dataSource.assertCoinInvariantHolds("after signing into the second account")
            val afterSigningIntoSecondAccount = dataSource.playerTableSnapshot()

            // Step 8: play a duel as the second account — the winner's own device presents the
            // second account's token, and the opponent presents a third, plain device id,
            // unrelated to either account.
            val guestAccountBalanceBeforeSecondDuel = client.profileOf(loser.deviceId).coinBalance
            val secondDuel = client.openSocketDuelAs(
                hostDeviceId = winner.deviceId,
                hostToken = secondAccountToken,
                opponentDeviceId = THIRD_DEVICE,
            )
            val secondOutcome = secondDuel.playToFinish()
            val secondWinnerSeat = checkNotNull(secondOutcome.winner) {
                "handSeed=${secondDuel.handSeed} policySeed=$POLICY_SEED: second duel outcome has " +
                    "no winner, outcome=$secondOutcome"
            }
            val secondDuelWinner = secondDuel.seat(secondWinnerSeat)
            val secondDuelWonByTheAccount = secondDuelWinner.deviceId == winner.deviceId
            dataSource.assertCoinInvariantHolds("after playing the second duel")
            val guestAccountBalanceAfterSecondDuel = client.profileOf(loser.deviceId).coinBalance
            val hostBalanceAfterSecondDuel = client.profileOf(winner.deviceId).coinBalance

            // Step 9: sign out.
            val beforeSigningOut = dataSource.playerTableSnapshot()
            assertEquals(
                HttpStatusCode.NoContent,
                client.signOut(secondAccountToken),
                "POST /api/auth/sign-out with the second account's token",
            )
            dataSource.assertCoinInvariantHolds("after signing out")
            val afterSigningOut = dataSource.playerTableSnapshot()

            // Step 10: reconnect anonymously — a Hello with the winner's own device id and no
            // token names the winner's original player again.
            val beforeReconnectingAnonymously = dataSource.playerTableSnapshot()
            val step10Welcome = client.webSocketSession("/ws").completeHandshake(winner.deviceId)
            dataSource.assertCoinInvariantHolds("after reconnecting anonymously")
            val afterReconnectingAnonymously = dataSource.playerTableSnapshot()

            // Step 11: read the profile back.
            val step11Profile = client.profileOf(winner.deviceId)
            dataSource.assertCoinInvariantHolds("after reading the profile back")

            // GET /api/standings, once — a read that writes nothing. Calling it here is what
            // makes SCENARIO_ENDPOINTS's inclusion of /api/standings a fact about this scenario
            // itself, not only about the route sources.
            val standingsResponse = client.get("/api/standings")
            assertEquals(
                HttpStatusCode.OK,
                standingsResponse.status,
                "GET /api/standings after step 11 returned ${standingsResponse.status}",
            )

            // Step 12: sign in again as the host's account for a fresh token, then revoke the
            // device that token was issued to — the scenario's last step. `ADR-0050` §1's two
            // statements touch `device_binding` and `auth_session` alone, so `player` is asserted
            // byte-identical exactly as every earlier identity step asserts it
            // (`revokingLeavesThePlayerTableByteIdentical`), and `device_binding` is asserted to
            // have moved instead (`revokingChangesExactlyOneBindingColumn`) — the positive
            // control a `DELETE` that revoked nothing would otherwise satisfy the byte-identical
            // claim for free.
            val beforeRevoking = dataSource.playerTableSnapshot()
            val deviceBindingBeforeRevoking = dataSource.deviceBindingTableSnapshot()
            val revocationToken = client.signIn("Winner_1", "password1")
            dataSource.assertCoinInvariantHolds("after signing in again to revoke")
            val revokeResponse = client.delete("/api/me/device") {
                header(HttpHeaders.Authorization, "Bearer $revocationToken")
            }
            assertEquals(
                HttpStatusCode.NoContent,
                revokeResponse.status,
                "DELETE /api/me/device with the host's freshly issued token returned " +
                    "${revokeResponse.status}",
            )
            dataSource.assertCoinInvariantHolds("after revoking")
            val afterRevoking = dataSource.playerTableSnapshot()
            val deviceBindingAfterRevoking = dataSource.deviceBindingTableSnapshot()
            val deviceBindingColumnNames = dataSource.deviceBindingColumnNames()

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
                beforeSigningIn = beforeSigningIn,
                afterSigningIn = afterSigningIn,
                step6WelcomePlayerId = step6Welcome.playerId,
                beforeSigningIntoSecondAccount = beforeSigningIntoSecondAccount,
                afterSigningIntoSecondAccount = afterSigningIntoSecondAccount,
                step7WelcomePlayerId = step7Welcome.playerId,
                guestAccountBalanceBeforeSecondDuel = guestAccountBalanceBeforeSecondDuel,
                guestAccountBalanceAfterSecondDuel = guestAccountBalanceAfterSecondDuel,
                secondDuelWonByTheAccount = secondDuelWonByTheAccount,
                hostBalanceAfterSecondDuel = hostBalanceAfterSecondDuel,
                beforeSigningOut = beforeSigningOut,
                afterSigningOut = afterSigningOut,
                beforeReconnectingAnonymously = beforeReconnectingAnonymously,
                afterReconnectingAnonymously = afterReconnectingAnonymously,
                step10WelcomePlayerId = step10Welcome.playerId,
                step11ProfilePlayerId = step11Profile.playerId,
                beforeRevoking = beforeRevoking,
                afterRevoking = afterRevoking,
                deviceBindingBeforeRevoking = deviceBindingBeforeRevoking,
                deviceBindingAfterRevoking = deviceBindingAfterRevoking,
                deviceBindingColumnNames = deviceBindingColumnNames,
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

    /**
     * Signs in with [handle] and [password] over `POST /api/auth/sign-in`, asserting the response
     * is `200`, and returns the issued session token.
     *
     * Carries no device id header: sign-in resolves no identity of its own — no `X-Device-Id` and
     * no `Authorization` header are read (`ADR-0030` §2, `AuthRoutes.kt`'s own KDoc) — so a device
     * id here would be misleading, since it is never read.
     */
    private suspend fun HttpClient.signIn(handle: String, password: String): String {
        val response = post("/api/auth/sign-in") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"$handle","password":"$password"}""")
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "POST /api/auth/sign-in for handle=$handle returned ${response.status}",
        )
        return protocolJson.decodeFromString<SignInResponse>(response.bodyAsText()).sessionToken
    }

    /** Signs [token] out over `POST /api/auth/sign-out`, returning the response status. */
    private suspend fun HttpClient.signOut(token: String): HttpStatusCode {
        val response = post("/api/auth/sign-out") { header(HttpHeaders.Authorization, "Bearer $token") }
        return response.status
    }

    /**
     * Opens a two-socket duel exactly as [openSocketDuel] does — a `Hello`, then `CreateRoom` on
     * the first socket, then a second `Hello`, then `JoinRoom` on the second — but lets the caller
     * choose each socket's own device id, needed for step 8: [openSocketDuel] always seats
     * `HOST_DEVICE`/`GUEST_DEVICE`, which cannot express a duel between the winner's own device
     * (presenting the second account's token) and a third device unrelated to either account.
     */
    private suspend fun HttpClient.openSocketDuelAs(
        hostDeviceId: String,
        hostToken: String?,
        opponentDeviceId: String,
        handSeed: Long = HAND_SEED,
    ): SocketDuel {
        val hostSession = webSocketSession("/ws")
        hostSession.completeHandshake(hostDeviceId, hostToken)
        hostSession.send(Frame.Text(ProtocolCodec.encode(CreateRoom)))
        val hostMessage = hostSession.nextServerMessage()
        val hostRoomJoined = hostMessage as? ServerMessage.RoomJoined
            ?: error("Expected RoomJoined from host, got ${hostMessage::class.simpleName}")
        val hostClient = SocketClient(hostDeviceId, hostRoomJoined.seat, hostSession)
        hostClient.received.add(hostRoomJoined)

        val opponentSession = webSocketSession("/ws")
        opponentSession.completeHandshake(opponentDeviceId)
        opponentSession.send(Frame.Text(ProtocolCodec.encode(JoinRoom(hostRoomJoined.code))))
        val opponentMessage = opponentSession.nextServerMessage()
        val opponentRoomJoined = opponentMessage as? ServerMessage.RoomJoined
            ?: error("Expected RoomJoined from opponent, got ${opponentMessage::class.simpleName}")
        val opponentClient = SocketClient(opponentDeviceId, opponentRoomJoined.seat, opponentSession)
        opponentClient.received.add(opponentRoomJoined)

        return SocketDuel(hostRoomJoined.code, handSeed, listOf(hostClient, opponentClient))
    }
}

/** The third device in step 8's duel: a fresh, plain device unrelated to either account. */
private const val THIRD_DEVICE: String = "e2e-third"

/**
 * The `/api/…` paths this scenario answers for, over its own HTTP calls, so [SCENARIO_ENDPOINTS]
 * stays a fact about what runs above rather than a copy of what the route sources declare.
 *
 * Every entry but two is a literal this class calls directly: `sign-up`, `sign-in` (used three
 * times: steps 5, 7 and the closing revocation sign-in), `sign-out`, `GET /api/me`, `PUT
 * /api/me/name`, `GET /api/standings` and `DELETE /api/me/device`. `/api/me/duels` is one
 * exception, written down rather than called: `ProfileRoutes.kt` hands its handler a `ProfileReads`
 * port only, never a `ProfileWrites`, so it is a read that moves no coin, and adding a fresh HTTP
 * call to it is outside this ticket's scope. `/api/auth/verify-email` (`TASK-041618`) is the other:
 * `RecoveryEmails.verifyPending` writes `recovery_email` and deletes from `email_verification`, and
 * touches neither `player.coin_balance` nor `duel_result`, so it moves no coin either — the same
 * "does not move coins" escape [everyApiPathInTheRouteSourcesIsExercisedByTheScenario]'s own KDoc
 * names, used here for the first time.
 */
private val SCENARIO_ENDPOINTS: Set<String> = setOf(
    "/api/auth/sign-up",
    "/api/auth/sign-in",
    "/api/auth/sign-out",
    "/api/auth/verify-email",
    "/api/me",
    "/api/me/duels",
    "/api/me/name",
    "/api/me/device",
    "/api/standings",
)

/** Matches a double-quoted Kotlin string literal beginning `/api/`, capturing the path alone. */
private val API_PATH_LITERAL: Regex = Regex("\"(/api/[^\"]*)\"")

/**
 * Every `/api/…` string literal found in the four files under
 * `poker-server/src/main/kotlin/duels/poker/server/http` whose names end `Routes.kt`
 * (`AuthRoutes.kt`, `DeviceRoutes.kt`, `ProfileRoutes.kt`, `StandingsRoutes.kt`), read as plain
 * text and matched with [API_PATH_LITERAL] rather than parsed as Kotlin — so a path assembled
 * from constants, rather than written as a literal, escapes this scan. Every route in the
 * repository today writes its path as a literal.
 *
 * Walks upward from the working directory looking for the `http` package, the same technique
 * `HttpEndpointDocumentationTest` uses to find `docs/protocol.md`, so this does not depend on
 * whether Gradle's test working directory is the module root or the repository root.
 */
private fun apiPathLiteralsInRouteSources(): Set<String> {
    val httpDirectory = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "poker-server/src/main/kotlin/duels/poker/server/http") }
        .firstOrNull { it.isDirectory }
        ?: error(
            "poker-server/src/main/kotlin/duels/poker/server/http not found above " +
                File("").absolutePath,
        )
    val routeFiles = httpDirectory.listFiles { file -> file.name.endsWith("Routes.kt") }
        ?: error("could not list $httpDirectory")
    return routeFiles.flatMap { file -> API_PATH_LITERAL.findAll(file.readText()).map { it.groupValues[1] } }.toSet()
}

/**
 * The column names of `device_binding`, in the same left-to-right order
 * [deviceBindingTableSnapshot] reads its values in — read from the same query, so a test can name
 * which position changed (`revokingChangesExactlyOneBindingColumn`) rather than only count how
 * many did.
 */
private fun DataSource.deviceBindingColumnNames(): List<String> {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM device_binding ORDER BY player_id").use { rs ->
                return (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
            }
        }
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
 * @property beforeSigningIn The `player` table snapshot immediately before step 5.
 * @property afterSigningIn The `player` table snapshot immediately after step 5.
 * @property step6WelcomePlayerId The player id step 6's `Welcome` named, reconnecting with the
 *   winner's own token from the winner's own device.
 * @property beforeSigningIntoSecondAccount The `player` table snapshot immediately before step 7.
 * @property afterSigningIntoSecondAccount The `player` table snapshot immediately after step 7.
 * @property step7WelcomePlayerId The player id step 7's `Welcome` named, presenting the second
 *   account's token from the winner's own device.
 * @property guestAccountBalanceBeforeSecondDuel The second account's `coinBalance`, read for the
 *   loser's own device, immediately before step 8.
 * @property guestAccountBalanceAfterSecondDuel The second account's `coinBalance`, read for the
 *   loser's own device, immediately after step 8.
 * @property secondDuelWonByTheAccount Whether step 8's duel was won by the socket presenting the
 *   second account's token — the winner's own device — rather than by the third, plain device.
 * @property hostBalanceAfterSecondDuel The winner's own `coinBalance`, read for the winner's own
 *   device, immediately after step 8 — expected to equal [winnerBalanceAfterDuel] unchanged, since
 *   step 8's coin moves the second account, not the winner's own device.
 * @property beforeSigningOut The `player` table snapshot immediately before step 9.
 * @property afterSigningOut The `player` table snapshot immediately after step 9.
 * @property beforeReconnectingAnonymously The `player` table snapshot immediately before step 10.
 * @property afterReconnectingAnonymously The `player` table snapshot immediately after step 10.
 * @property step10WelcomePlayerId The player id step 10's `Welcome` named, reconnecting with the
 *   winner's own device id and no token.
 * @property step11ProfilePlayerId The player id step 11's `GET /api/me` named, for the winner's
 *   own device id.
 * @property beforeRevoking The `player` table snapshot immediately before step 12's revocation.
 * @property afterRevoking The `player` table snapshot immediately after step 12's revocation.
 * @property deviceBindingBeforeRevoking The `device_binding` table snapshot immediately before
 *   step 12's revocation.
 * @property deviceBindingAfterRevoking The `device_binding` table snapshot immediately after
 *   step 12's revocation.
 * @property deviceBindingColumnNames The column names of `device_binding`, in the same order
 *   [deviceBindingBeforeRevoking] and [deviceBindingAfterRevoking] read their values in.
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
    val beforeSigningIn: List<List<Any?>>,
    val afterSigningIn: List<List<Any?>>,
    val step6WelcomePlayerId: String,
    val beforeSigningIntoSecondAccount: List<List<Any?>>,
    val afterSigningIntoSecondAccount: List<List<Any?>>,
    val step7WelcomePlayerId: String,
    val guestAccountBalanceBeforeSecondDuel: Int,
    val guestAccountBalanceAfterSecondDuel: Int,
    val secondDuelWonByTheAccount: Boolean,
    val hostBalanceAfterSecondDuel: Int,
    val beforeSigningOut: List<List<Any?>>,
    val afterSigningOut: List<List<Any?>>,
    val beforeReconnectingAnonymously: List<List<Any?>>,
    val afterReconnectingAnonymously: List<List<Any?>>,
    val step10WelcomePlayerId: String,
    val step11ProfilePlayerId: String,
    val beforeRevoking: List<List<Any?>>,
    val afterRevoking: List<List<Any?>>,
    val deviceBindingBeforeRevoking: List<List<Any?>>,
    val deviceBindingAfterRevoking: List<List<Any?>>,
    val deviceBindingColumnNames: List<String>,
)
