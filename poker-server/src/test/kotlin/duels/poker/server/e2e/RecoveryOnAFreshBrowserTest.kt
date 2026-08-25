package duels.poker.server.e2e

import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.db.assertCoinInvariantHolds
import duels.poker.server.db.deviceBindingTableSnapshot
import duels.poker.server.db.playerTableSnapshot
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
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
 * `STORY-0407`'s setup: boots the shipped composition against a real database, plays one
 * anonymous duel between two fresh devices, and records what each device's `GET /api/me` reads
 * afterwards — the coin balance and the profile that a browser which has never been seen must
 * later recover.
 *
 * [runRecovery] is the one place the arc runs, and every test method below calls it and asserts
 * one aspect of the [RecoveryRecord] it returns — never its own copy of the setup or its own
 * re-derivation of the arc. Later tickets in this story extend [runRecovery] with the sign-up and
 * recovery steps that hand this duel's coin and history back to a device that never played it;
 * this ticket only plays the duel and proves the coin invariant already holds around it.
 *
 * The winner is whichever seat the engine adjudicated (`checkNotNull(outcome.winner)`), never a
 * fixed seat — `STORY-0213` shipped a hard-coded seat `0` that passed eight of nine tests, and
 * this story's later tickets all hang off `winner`, so a seat assumed once here would be assumed
 * everywhere.
 */
@Timeout(120)
internal class RecoveryOnAFreshBrowserTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    @Test
    fun theRecoveryArcMovesNoCoin() {
        runRecovery()
    }

    @Test
    fun theDuelPaidExactlyOneCoinEachWay() {
        val record = runRecovery()
        assertEquals(
            1,
            record.winnerProfileAfterDuel.coinBalance,
            "winner deviceId=${record.winnerDeviceId}: expected coinBalance 1 after the duel, got " +
                "${record.winnerProfileAfterDuel.coinBalance}",
        )
        assertEquals(
            -1,
            record.loserProfileAfterDuel.coinBalance,
            "loser deviceId=${record.loserDeviceId}: expected coinBalance -1 after the duel, got " +
                "${record.loserProfileAfterDuel.coinBalance}",
        )
    }

    @Test
    fun theWinnerIsNamedAndTheLoserIsNot() {
        val record = runRecovery()
        assertEquals(
            RECOVERED_NAME,
            record.originalProfile.displayName,
            "winner deviceId=${record.winnerDeviceId}: expected displayName '$RECOVERED_NAME' after setting name, got " +
                "${record.originalProfile.displayName}",
        )
        assertEquals(
            null,
            record.loserProfile.displayName,
            "loser deviceId=${record.loserDeviceId}: expected displayName null (never set a name), got " +
                "${record.loserProfile.displayName}",
        )
    }

    @Test
    fun theFreshBrowsersWelcomeNamesTheRecoveredAccount() {
        val record = runRecovery()
        assertEquals(
            record.winnerProfileAfterDuel.playerId,
            record.freshWelcome.playerId,
            "fresh browser deviceId=$FRESH_DEVICE: Welcome named ${record.freshWelcome.playerId}, " +
                "expected the winner's own playerId ${record.winnerProfileAfterDuel.playerId}, read " +
                "over HTTP right after the duel, before either client knew who would win",
        )
    }

    @Test
    fun theFreshBrowsersWelcomeCarriesNoDeviceId() {
        val record = runRecovery()
        assertEquals(
            null,
            record.freshWelcome.deviceId,
            "fresh browser deviceId=$FRESH_DEVICE: expected Welcome.deviceId null (the session " +
                "outranks the device id, ADR-0030 §2), got ${record.freshWelcome.deviceId}",
        )
    }

    /**
     * The positive control [theFreshBrowsersWelcomeCarriesNoDeviceId] needs beside it: a `Welcome`
     * whose `deviceId` were *always* null would satisfy that test for free. This is the input that
     * says otherwise — the same winner's device, reconnecting with no token, still gets its own
     * device id back.
     */
    @Test
    fun theOriginalDevicesWelcomeStillCarriesItsDeviceId() {
        val record = runRecovery()
        assertEquals(
            record.winnerDeviceId,
            record.originalWelcome.deviceId,
            "original device: expected Welcome.deviceId equal to winnerDeviceId=" +
                "${record.winnerDeviceId}, got ${record.originalWelcome.deviceId}",
        )
    }

    @Test
    fun theFreshBrowserReadsTheSameProfile() {
        val record = runRecovery()
        assertEquals(
            record.originalProfile,
            record.recoveredProfile,
            "fresh browser deviceId=$FRESH_DEVICE: profileOf read a different " +
                "ProfileResponse than the original device",
        )
    }

    @Test
    fun theFreshBrowserReadsTheSameDuels() {
        val record = runRecovery()
        assertEquals(
            1,
            record.originalDuels.duels.size,
            "original device: expected exactly one duel, got ${record.originalDuels.duels.size}",
        )
        assertEquals(
            record.originalDuels,
            record.recoveredDuels,
            "fresh browser deviceId=$FRESH_DEVICE: duelsOf read a different " +
                "RecentDuelsResponse than the original device",
        )
    }

    @Test
    fun aDifferentPlayersProfileDoesNotCompareEqual() {
        val record = runRecovery()
        assertEquals(
            false,
            record.originalProfile == record.loserProfile,
            "two distinct ProfileResponse values should not compare equal: winner " +
                "deviceId=${record.winnerDeviceId} vs loser deviceId=${record.loserDeviceId}",
        )
    }

    /**
     * `ADR-0027` path 1 short-circuits before path 3's minting, so recovery litters no orphan
     * profile — the whole `player` table, not only the recovered row, is byte-identical across
     * the bracket that runs the sign-in, the fresh handshake, and both reads.
     */
    @Test
    fun theRecoveryCreatesNoPlayerRow() {
        val record = runRecovery()
        assertEquals(
            record.playerBeforeRecovery,
            record.playerAfterRecovery,
            "the player table changed across recovery (sign-in, fresh handshake, profile and " +
                "duel reads): before=${record.playerBeforeRecovery}, after=${record.playerAfterRecovery}",
        )
    }

    /**
     * The half a `player`-only check cannot see: `ADR-0027` resolves the fresh device's `Hello`
     * to the already-live session, so no rebinding — an `INSERT` naming the recovered player — is
     * added to `device_binding` either.
     */
    @Test
    fun theRecoveryCreatesNoDeviceBinding() {
        val record = runRecovery()
        assertEquals(
            record.deviceBindingBeforeRecovery,
            record.deviceBindingAfterRecovery,
            "the device_binding table changed across recovery (sign-in, fresh handshake, profile " +
                "and duel reads): before=${record.deviceBindingBeforeRecovery}, " +
                "after=${record.deviceBindingAfterRecovery}",
        )
    }

    /**
     * Two inputs, two different expected values: a `liveBindingCountFor` that answered `0` for
     * everything would satisfy the fresh device's assertion for free, so the winner's own device
     * — which the recovery bracket also reconnects, as the positive control — has to answer `1`.
     */
    @Test
    fun theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne() {
        val record = runRecovery()
        assertEquals(
            0,
            record.freshDeviceLiveBindings,
            "deviceId=$FRESH_DEVICE: expected no live device_binding row after recovery, got " +
                "${record.freshDeviceLiveBindings}",
        )
        assertEquals(
            1,
            record.originalDeviceLiveBindings,
            "winner deviceId=${record.winnerDeviceId}: expected exactly one live device_binding " +
                "row, got ${record.originalDeviceLiveBindings}",
        )
    }

    @Test
    fun aWrongPasswordFromTheFreshBrowserIsRefused() {
        val record = runRecovery()
        assertEquals(
            HttpStatusCode.Unauthorized,
            record.wrongPasswordStatus,
            "POST /api/auth/sign-in with RECOVERY_HANDLE and wrong password: " +
                "expected status 401 Unauthorized, got ${record.wrongPasswordStatus}",
        )
    }

    @Test
    fun theRefusedSignInAddedNoSessionRow() {
        val record = runRecovery()
        assertEquals(
            1,
            record.sessionsBeforeWrongPassword,
            "auth_session row count before the wrong password attempt: expected 1, got " +
                "${record.sessionsBeforeWrongPassword}",
        )
        assertEquals(
            1,
            record.sessionsAfterWrongPassword,
            "auth_session row count after the wrong password attempt: expected 1 (unchanged), " +
                "got ${record.sessionsAfterWrongPassword}",
        )
    }

    @Test
    fun theCountSeesASessionRowAppear() {
        val record = runRecovery()
        assertEquals(
            0,
            record.sessionsBeforeAnySignIn,
            "auth_session row count before any sign-in: expected 0, got " +
                "${record.sessionsBeforeAnySignIn}",
        )
        assertEquals(
            1,
            record.sessionsBeforeWrongPassword,
            "auth_session row count before the wrong password attempt: expected 1, got " +
                "${record.sessionsBeforeWrongPassword}",
        )
    }

    /**
     * Boots the shipped composition against [dataSource] — `installDuelServer(dataSource)`, then
     * `createClient { install(WebSockets) }`, exactly as [IdentityMovesNoCoinTest] does — opens
     * one duel, plays it to completion, and reads both devices' profiles afterwards, asserting
     * [dataSource]'s coin invariant before the duel and after it.
     *
     * Every test method in this class calls this and asserts one fact off the returned
     * [RecoveryRecord], so a defect anywhere in the arc reddens every method, not only the one
     * whose name happens to describe the step it broke.
     */
    private fun runRecovery(): RecoveryRecord = runBlocking {
        var record: RecoveryRecord? = null
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            dataSource.assertCoinInvariantHolds("before the duel")

            // Play one anonymous duel between two fresh devices.
            val duel = client.openSocketDuel()
            val outcome = duel.playToFinish()
            dataSource.assertCoinInvariantHolds("after the duel")

            // The winner is whichever seat the engine adjudicated, never a fixed seat.
            val winnerSeat = checkNotNull(outcome.winner) {
                "handSeed=${duel.handSeed} policySeed=$POLICY_SEED: outcome has no winner, outcome=$outcome"
            }
            val loserSeat = 1 - winnerSeat
            val winner = duel.seat(winnerSeat)
            val loser = duel.seat(loserSeat)

            val winnerProfileAfterDuel = client.profileOf(winner.deviceId)
            val loserProfileAfterDuel = client.profileOf(loser.deviceId)

            // Set the winner's display name.
            assertEquals(
                HttpStatusCode.OK,
                client.setName(winner.deviceId, RECOVERED_NAME),
                "PUT /api/me/name for the winner's deviceId=${winner.deviceId}",
            )
            dataSource.assertCoinInvariantHolds("after setting the winner's name")

            // Sign up the winner.
            assertEquals(
                HttpStatusCode.Created,
                client.signUp(winner.deviceId, RECOVERY_HANDLE, RECOVERY_PASSWORD),
                "POST /api/auth/sign-up for the winner's deviceId=${winner.deviceId}",
            )
            dataSource.assertCoinInvariantHolds("after the winner signs up")

            // Read the winner's profile after sign-up, and the loser's profile at the same moment.
            val originalProfile = client.profileOf(winner.deviceId)
            val loserProfile = client.profileOf(loser.deviceId)
            val originalDuels = client.duelsOf(winner.deviceId)

            // The bracket opens here: everything from here to recoveredDuels below is what a
            // browser that has never been seen does, plus the original device's control
            // handshake, which belongs inside it and is correct to include — that handshake
            // presents a device with a live binding, so IdentityResolver answers Identity.Device
            // and the directory is never asked to create anything.
            val playerBeforeRecovery = dataSource.playerTableSnapshot()
            val deviceBindingBeforeRecovery = dataSource.deviceBindingTableSnapshot()

            // The positive control, taken first: the winner's own device, no token. ADR-0018
            // gives a player one live socket and the newest wins, and both this handshake and the
            // fresh browser's below resolve to the same player, so taking this one second would
            // have it evicted before its Welcome could be read.
            val originalWelcome = client.webSocketSession("/ws").completeHandshake(winner.deviceId)

            // A browser that has never connected recovers the account: no X-Device-Id, no
            // Authorization — sign-in resolves neither (ADR-0030 §2, AuthRoutes.kt's own KDoc).
            val sessionsBeforeAnySignIn = dataSource.authSessionRowCount()
            val sessionToken = client.signIn(RECOVERY_HANDLE, RECOVERY_PASSWORD)
            dataSource.assertCoinInvariantHolds("after the fresh browser signs in")

            // The fresh browser's own handshake: a device id it has never presented before, and
            // the token, together in one Hello — a real client keeps sending its device id whether
            // or not it holds a token (ADR-0030 §8), and this evicts the original device's socket.
            val freshWelcome = client.webSocketSession("/ws").completeHandshake(FRESH_DEVICE, sessionToken)
            dataSource.assertCoinInvariantHolds("after the fresh browser's handshake")

            // The fresh browser reads back the profile and duel history through the recovered session.
            val recoveredProfile = client.profileOf(FRESH_DEVICE, sessionToken)
            val recoveredDuels = client.duelsOf(FRESH_DEVICE, sessionToken)

            // The bracket closes here.
            val playerAfterRecovery = dataSource.playerTableSnapshot()
            val deviceBindingAfterRecovery = dataSource.deviceBindingTableSnapshot()
            dataSource.assertCoinInvariantHolds("after the fresh browser reads the profile and duels")

            // Attempt to sign in with the correct handle and a wrong password.
            val sessionsBeforeWrongPassword = dataSource.authSessionRowCount()
            val wrongPasswordStatus = client.signInStatus(RECOVERY_HANDLE, "not-$RECOVERY_PASSWORD")
            val sessionsAfterWrongPassword = dataSource.authSessionRowCount()
            dataSource.assertCoinInvariantHolds("after the wrong password attempt")

            val freshDeviceLiveBindings = dataSource.liveBindingCountFor(FRESH_DEVICE)
            val originalDeviceLiveBindings = dataSource.liveBindingCountFor(winner.deviceId)

            record = RecoveryRecord(
                winnerDeviceId = winner.deviceId,
                loserDeviceId = loser.deviceId,
                winnerProfileAfterDuel = winnerProfileAfterDuel,
                loserProfileAfterDuel = loserProfileAfterDuel,
                originalProfile = originalProfile,
                loserProfile = loserProfile,
                sessionToken = sessionToken,
                originalWelcome = originalWelcome,
                freshWelcome = freshWelcome,
                originalDuels = originalDuels,
                recoveredProfile = recoveredProfile,
                recoveredDuels = recoveredDuels,
                playerBeforeRecovery = playerBeforeRecovery,
                playerAfterRecovery = playerAfterRecovery,
                deviceBindingBeforeRecovery = deviceBindingBeforeRecovery,
                deviceBindingAfterRecovery = deviceBindingAfterRecovery,
                freshDeviceLiveBindings = freshDeviceLiveBindings,
                originalDeviceLiveBindings = originalDeviceLiveBindings,
                sessionsBeforeAnySignIn = sessionsBeforeAnySignIn,
                wrongPasswordStatus = wrongPasswordStatus,
                sessionsBeforeWrongPassword = sessionsBeforeWrongPassword,
                sessionsAfterWrongPassword = sessionsAfterWrongPassword,
            )
        }
        checkNotNull(record) { "runRecovery: testApplication completed without producing a RecoveryRecord" }
    }

    /**
     * Reads [deviceId]'s profile over `GET /api/me`, asserting the response is `200`, then decodes
     * the body with [protocolJson] — copied from `IdentityMovesNoCoinTest`, not shared, because a
     * file-private top-level declaration in Kotlin is scoped to the file that declares it.
     *
     * Sets `X-Device-Id` always. Sets `Authorization: Bearer $token` only when [token] is
     * non-null, so a call site that omits it sends exactly the request this function always sent;
     * a later ticket in this story passes the recovered account's token here.
     */
    private suspend fun HttpClient.profileOf(deviceId: String, token: String? = null): ProfileResponse {
        val response = get("/api/me") {
            header(DEVICE_ID_HEADER, deviceId)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me for deviceId=$deviceId returned ${response.status}",
        )
        return protocolJson.decodeFromString(response.bodyAsText())
    }

    /**
     * Reads [deviceId]'s recent duels over `GET /api/me/duels`, asserting the response is `200`,
     * then decodes the body with [protocolJson] — shaped exactly like [profileOf].
     *
     * Sets `X-Device-Id` always. Sets `Authorization: Bearer $token` only when [token] is
     * non-null. No query string — the endpoint's defaults are what a client sends.
     */
    private suspend fun HttpClient.duelsOf(deviceId: String, token: String? = null): RecentDuelsResponse {
        val response = get("/api/me/duels") {
            header(DEVICE_ID_HEADER, deviceId)
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/me/duels for deviceId=$deviceId returned ${response.status}",
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
     * is `200`, and returns the issued session token — copied from `IdentityMovesNoCoinTest`, not
     * shared, because a file-private top-level declaration in Kotlin is scoped to the file that
     * declares it.
     *
     * Carries no device id header and no `Authorization` header: sign-in resolves no identity of
     * its own — no `X-Device-Id` and no `Authorization` header are read (`ADR-0030` §2,
     * `AuthRoutes.kt`'s own KDoc) — which is precisely what lets a browser that has never
     * connected recover an account.
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

    /**
     * Posts the same body [signIn] does over `POST /api/auth/sign-in`, but returns the response
     * status without asserting it — [signIn] asserts `200` and cannot express a refusal, so this
     * is a second helper rather than a change to the first.
     *
     * Carries no device id header and no `Authorization` header, exactly as [signIn] does.
     */
    private suspend fun HttpClient.signInStatus(handle: String, password: String): HttpStatusCode {
        val response = post("/api/auth/sign-in") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"$handle","password":"$password"}""")
        }
        return response.status
    }
}

/**
 * Runs `SELECT count(*) FROM auth_session`.
 */
private fun DataSource.authSessionRowCount(): Int {
    connection.use { connection ->
        connection.prepareStatement("SELECT count(*) FROM auth_session").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }
}

/**
 * What one run of [RecoveryOnAFreshBrowserTest.runRecovery] set up, so every test method can
 * assert one aspect of a single run rather than re-deriving the arc itself.
 *
 * @property winnerDeviceId The device id of the socket that won the duel.
 * @property loserDeviceId The device id of the socket that lost the duel.
 * @property winnerProfileAfterDuel The winner's whole `GET /api/me` response, read right after
 *   the duel.
 * @property loserProfileAfterDuel The loser's whole `GET /api/me` response, read right after the
 *   duel.
 * @property originalProfile The winner's whole `GET /api/me` response, read after the sign-up —
 *   the value the whole story compares against.
 * @property loserProfile The loser's whole `GET /api/me` response, read at the same moment as
 *   [originalProfile], after the winner has signed up but the loser has not.
 * @property sessionToken The session token `POST /api/auth/sign-in` issued for
 *   `RECOVERY_HANDLE`/`RECOVERY_PASSWORD`.
 * @property originalWelcome The whole `Welcome` frame the winner's own device received on
 *   reconnecting with no token — the positive control, taken before the fresh browser's
 *   handshake evicts it.
 * @property freshWelcome The whole `Welcome` frame the fresh browser received, presenting
 *   `FRESH_DEVICE` and [sessionToken] together in one `Hello`.
 * @property originalDuels The winner's whole `GET /api/me/duels` response, read after the
 *   sign-up — the value the whole story compares against.
 * @property recoveredProfile The fresh browser's whole `GET /api/me` response, read through the
 *   session token after the handshake.
 * @property recoveredDuels The fresh browser's whole `GET /api/me/duels` response, read through
 *   the session token after the handshake.
 * @property playerBeforeRecovery Every column of every `player` row, taken after the winner's
 *   sign-up and before the original device's control handshake — the open end of the bracket
 *   recovery must not perturb.
 * @property playerAfterRecovery Every column of every `player` row, taken immediately after
 *   [recoveredDuels] is read — the closed end of the same bracket.
 * @property deviceBindingBeforeRecovery Every column of every `device_binding` row, taken
 *   alongside [playerBeforeRecovery].
 * @property deviceBindingAfterRecovery Every column of every `device_binding` row, taken
 *   alongside [playerAfterRecovery].
 * @property freshDeviceLiveBindings The count of live `device_binding` rows for `FRESH_DEVICE`
 *   once the bracket has closed — recovery binds no device, so this is `0`.
 * @property originalDeviceLiveBindings The count of live `device_binding` rows for
 *   [winnerDeviceId] once the bracket has closed — the winner's own device keeps its one binding.
 * @property sessionsBeforeAnySignIn The count of `auth_session` rows before the successful
 *   sign-in, expected to be `0`.
 * @property wrongPasswordStatus The HTTP response status code from attempting to sign in with the
 *   correct handle and a wrong password, expected to be `401 Unauthorized`.
 * @property sessionsBeforeWrongPassword The count of `auth_session` rows before the wrong password
 *   attempt, expected to be `1`.
 * @property sessionsAfterWrongPassword The count of `auth_session` rows after the wrong password
 *   attempt, expected to be `1` (unchanged).
 */
private data class RecoveryRecord(
    val winnerDeviceId: String,
    val loserDeviceId: String,
    val winnerProfileAfterDuel: ProfileResponse,
    val loserProfileAfterDuel: ProfileResponse,
    val originalProfile: ProfileResponse,
    val loserProfile: ProfileResponse,
    val sessionToken: String,
    val originalWelcome: ServerMessage.Welcome,
    val freshWelcome: ServerMessage.Welcome,
    val originalDuels: RecentDuelsResponse,
    val recoveredProfile: ProfileResponse,
    val recoveredDuels: RecentDuelsResponse,
    val playerBeforeRecovery: List<List<Any?>>,
    val playerAfterRecovery: List<List<Any?>>,
    val deviceBindingBeforeRecovery: List<List<Any?>>,
    val deviceBindingAfterRecovery: List<List<Any?>>,
    val freshDeviceLiveBindings: Int,
    val originalDeviceLiveBindings: Int,
    val sessionsBeforeAnySignIn: Int,
    val wrongPasswordStatus: HttpStatusCode,
    val sessionsBeforeWrongPassword: Int,
    val sessionsAfterWrongPassword: Int,
)

private const val RECOVERED_NAME: String = "Champion"
private const val RECOVERY_HANDLE: String = "Recovered_1"
private const val RECOVERY_PASSWORD: String = "password1"

/** The device id a browser that has never connected before presents, alongside a session token. */
private const val FRESH_DEVICE: String = "e2e-fresh-browser"

/**
 * Counts live `device_binding` rows for [deviceId] — `revoked_at IS NULL`. A count, not a
 * boolean: "there is no binding" and "there is exactly one" are different claims, and
 * [RecoveryOnAFreshBrowserTest.theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne] asserts both.
 *
 * One use site in this file today; a second would move this to `CoinInvariant.kt` beside
 * `deviceBindingTableSnapshot`.
 */
private fun DataSource.liveBindingCountFor(deviceId: String): Int {
    connection.use { connection ->
        connection.prepareStatement(
            "SELECT count(*) FROM device_binding WHERE device_id = ? AND revoked_at IS NULL",
        ).use { statement ->
            statement.setString(1, deviceId)
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }
}
