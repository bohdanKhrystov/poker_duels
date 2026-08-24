package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import duels.poker.server.duelServer
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.serverComponents
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

private const val OWNER_DEVICE = "d-owner"
private const val OTHER_DEVICE = "d-other"
private const val OWNER_HANDLE = "Owner_1"
private const val OTHER_HANDLE = "Other_1"
private const val PASSWORD = "password1"

/**
 * Proves `ADR-0050` §4's addition to `STORY-0406`'s criterion end to end, against a real database
 * and the shipped composition — `duelServer(serverComponents(config, dataSource))`, exactly as
 * [SignInDatabaseTest] boots it: one `DELETE /api/me/device` leaves the revoking session working
 * and ends every other session that player holds, and it reaches only that player's own rows.
 *
 * Every token in this file is minted by `POST /api/auth/sign-in`, never by calling
 * `PostgresAuthSessions.issue` directly. Sign-up issues no session (`ADR-0030` §3), so a token here
 * only ever exists because a sign-in proved a password — which is what makes "every other session"
 * a meaningful phrase in this file, exactly as it is in [SignInDatabaseTest].
 *
 * `SignInDatabaseTest`'s private top-level helpers are per-file (Kotlin's `private` is
 * file-scoped), so this class declares its own copies of the ones its tests use — `handshake`,
 * `helloWith`, `profileOf`, `meWithToken`, `signUp`, `signIn` — plus one this file adds,
 * `revokeDevice`. Extracting a shared file would edit two merged suites, which is out of scope here.
 */
class DeviceRevocationDatabaseTest {
    private lateinit var dataSource: PGSimpleDataSource
    private lateinit var config: ServerConfig

    @BeforeEach
    fun setUp() {
        PostgresTestSupport.requireDocker()
        val coordinates = PostgresTestSupport.containerCoordinates()
        dataSource = PGSimpleDataSource().apply {
            setUrl(coordinates.url)
            user = coordinates.user
            password = coordinates.password
        }
        Migrations.migrate(dataSource)
        config = ServerConfig(
            port = 8080,
            maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
            maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
            databaseUrl = coordinates.url,
            databaseUser = coordinates.user,
            databasePassword = coordinates.password,
            databasePoolSize = 10,
            roomWaitingTimeoutMillis = 60_000,
            roomFinishedTimeoutMillis = 60_000,
        )
    }

    @Test
    fun theRevokingSessionSurvivesAndTheOthersDoNot() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val ownerId = client.setUpOwner()

                // Three separate POST /api/auth/sign-in calls, so three separate auth_session rows
                // — never PostgresAuthSessions.issue called directly.
                val t0 = client.signIn(OWNER_HANDLE, PASSWORD)
                val t1 = client.signIn(OWNER_HANDLE, PASSWORD)
                val t2 = client.signIn(OWNER_HANDLE, PASSWORD)

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(t0))

                // Immediately afterwards, and in this same test: the revoking token still works —
                // ADR-0050 §4's "both asserted by using both tokens" — and its body resolves to the
                // owner, not merely a 200 that could belong to anybody.
                val survivor = client.meWithToken(t0)
                assertEquals(HttpStatusCode.OK, survivor.status)
                val profile = protocolJson.decodeFromString<ProfileResponse>(survivor.bodyAsText())
                assertEquals(ownerId.toString(), profile.playerId)

                // The two sessions not presented to revokeDevice are both dead.
                assertEquals(HttpStatusCode.Unauthorized, client.meWithToken(t1).status)
                assertEquals(HttpStatusCode.Unauthorized, client.meWithToken(t2).status)
            }
        }
    }

    @Test
    fun exactlyOneSessionRowIsLeft() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val ownerId = client.setUpOwner()

                val t0 = client.signIn(OWNER_HANDLE, PASSWORD)
                client.signIn(OWNER_HANDLE, PASSWORD)
                client.signIn(OWNER_HANDLE, PASSWORD)

                // A count, not a status: a 401 cannot tell a deleted row from an expired one, and
                // this 3 -> 1 cannot be satisfied by a DELETE that removed everything.
                assertEquals(3L, dataSource.authSessionCountFor(ownerId))

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(t0))

                assertEquals(1L, dataSource.authSessionCountFor(ownerId))
            }
        }
    }

    @Test
    fun anotherPlayersSessionsAreUntouched() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val ownerId = client.setUpOwner()

                client.handshake(OTHER_DEVICE)
                val otherId = UUID.fromString(client.profileOf(OTHER_DEVICE).playerId)
                assertEquals(HttpStatusCode.Created, client.signUp(OTHER_DEVICE, OTHER_HANDLE, PASSWORD))

                val keeping = client.signIn(OWNER_HANDLE, PASSWORD)
                val o0 = client.signIn(OTHER_HANDLE, PASSWORD)
                val o1 = client.signIn(OTHER_HANDLE, PASSWORD)

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(keeping))

                // Without a player_id predicate on the DELETE, this is what would catch it — the
                // other player's own tokens still resolving, by identity and not merely by status.
                val other0 = client.meWithToken(o0)
                assertEquals(HttpStatusCode.OK, other0.status)
                val other0Profile = protocolJson.decodeFromString<ProfileResponse>(other0.bodyAsText())
                assertEquals(otherId.toString(), other0Profile.playerId)

                val other1 = client.meWithToken(o1)
                assertEquals(HttpStatusCode.OK, other1.status)
                val other1Profile = protocolJson.decodeFromString<ProfileResponse>(other1.bodyAsText())
                assertEquals(otherId.toString(), other1Profile.playerId)

                // The owner's own kept token, asserted again here: a DELETE with no player_id
                // predicate at all would still pass the two checks above.
                val keptOwner = client.meWithToken(keeping)
                assertEquals(HttpStatusCode.OK, keptOwner.status)
                val keptOwnerProfile = protocolJson.decodeFromString<ProfileResponse>(keptOwner.bodyAsText())
                assertEquals(ownerId.toString(), keptOwnerProfile.playerId)
            }
        }
    }

    @Test
    fun aRevokedDeviceIsSeatedAsSomebodyElse() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val owner = client.establishOwnerWithABalance(dataSource)
                val controlBefore =
                    client.helloWith(deviceId = OTHER_DEVICE, sessionToken = null) as ServerMessage.Welcome

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(owner.token))

                // The owner's own device, presenting no token: with the live binding revoked, this
                // is a stranger's Hello, and the stranger it seats must not be the player who just
                // revoked it.
                val revoked = client.helloWith(deviceId = OWNER_DEVICE, sessionToken = null) as ServerMessage.Welcome
                assertNotEquals(owner.ownerId, revoked.playerId)

                // A device revocation never touched still resolves to exactly the player it always
                // did — without this half, a Hello that had stopped resolving anything at all would
                // also pass the assertion above.
                val controlAfter =
                    client.helloWith(deviceId = OTHER_DEVICE, sessionToken = null) as ServerMessage.Welcome
                assertEquals(controlBefore.playerId, controlAfter.playerId)
            }
        }
    }

    @Test
    fun theRevokedDevicesNewProfileHasNoCoins() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val owner = client.establishOwnerWithABalance(dataSource)

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(owner.token))

                // GET /api/me never mints — only the socket's Hello does (ADR-0012) — so the
                // revoked device's fresh profile has to be seated once before the HTTP read below
                // can find it. This is the same mint aRevokedDeviceIsSeatedAsSomebodyElse proves by
                // identity; here it only sets up the profile whose balance this test reads.
                client.handshake(OWNER_DEVICE)

                // The revoked device id, over HTTP: its fresh profile owes nobody a coin.
                val freshProfile = client.profileOf(OWNER_DEVICE)
                assertEquals(0, freshProfile.coinBalance)

                // The account the device left behind, read by the token that revoked it rather than
                // by the device id: still the same player, and still holding the coin it won.
                val survivor = client.meWithToken(owner.token)
                assertEquals(HttpStatusCode.OK, survivor.status)
                val survivorProfile = protocolJson.decodeFromString<ProfileResponse>(survivor.bodyAsText())
                assertEquals(owner.ownerId, survivorProfile.playerId)
                assertEquals(1, survivorProfile.coinBalance)
            }
        }
    }

    @Test
    fun aSocketOpenedBeforeTheRevocationIsNotClosed() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val owner = client.establishOwnerWithABalance(dataSource)

                val session = client.webSocketSession("/ws")
                session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = OWNER_DEVICE, sessionToken = null))))
                val welcome =
                    protocolJson.decodeFromString<ServerMessage>(
                        (session.incoming.receive() as Frame.Text).readText(),
                    ) as ServerMessage.Welcome
                assertEquals(owner.ownerId, welcome.playerId)

                assertEquals(HttpStatusCode.NoContent, client.revokeDevice(owner.token))

                // ADR-0049 §6: revocation closes no live socket. A frame sent on this same session,
                // after the revoking DELETE has already committed, must still get an answer rather
                // than the channel having gone dead underneath a player who could be mid-duel.
                session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = OWNER_DEVICE, sessionToken = null))))
                val stillAnswers = session.incoming.receive()
                assertTrue(stillAnswers is Frame.Text)
            }
        }
    }
}

/**
 * Seeds the one device and profile every test in this file needs: `d-owner` completes the socket
 * handshake, minting its player row, then signs up with [OWNER_HANDLE] and [PASSWORD] — sign-up
 * itself mints no session (`ADR-0030` §3), so every token a test mints afterwards comes from a
 * sign-in that proved that password. Returns the owner's player id.
 */
private suspend fun HttpClient.setUpOwner(): UUID {
    handshake(OWNER_DEVICE)
    val ownerId = UUID.fromString(profileOf(OWNER_DEVICE).playerId)
    assertEquals(HttpStatusCode.Created, signUp(OWNER_DEVICE, OWNER_HANDLE, PASSWORD))
    return ownerId
}

/** The owner's player id, read from its own [ServerMessage.Welcome], and the token that revokes it. */
private data class OwnerFixture(val ownerId: String, val token: String)

/**
 * Seeds the fixture the three revocation-outcome tests below share: `d-owner` completes the socket
 * handshake, minting its `player` row; a `duel_result` pair, written with raw SQL against a bare
 * opponent row minted the same way, lands its balance at `1`; `signUp` attaches the credential; a
 * sign-in yields the token that revokes. [setUpOwner] deliberately has neither a duel nor a balance
 * behind it — these tests need both, because a device sitting at `0` both before and after
 * revocation could not tell a fresh, empty profile from the one it left.
 */
private suspend fun HttpClient.establishOwnerWithABalance(dataSource: DataSource): OwnerFixture {
    val welcome = helloWith(deviceId = OWNER_DEVICE, sessionToken = null) as ServerMessage.Welcome
    assertEquals(OWNER_DEVICE, welcome.deviceId)
    val ownerId = UUID.fromString(welcome.playerId)

    val loserId = UUID.randomUUID()
    val duelId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    dataSource.connection.use { connection ->
        connection.prepareStatement("INSERT INTO player (id) VALUES (?)").use { statement ->
            statement.setObject(1, loserId)
            statement.executeUpdate()
        }
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
        listOf(ownerId to 1, loserId to -1).forEach { (player, delta) ->
            connection.prepareStatement(
                "INSERT INTO duel_result (duel_id, player_id, coin_delta) VALUES (?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, duelId)
                statement.setObject(2, player)
                statement.setInt(3, delta)
                statement.executeUpdate()
            }
            connection.prepareStatement("UPDATE player SET coin_balance = coin_balance + ? WHERE id = ?")
                .use { statement ->
                    statement.setInt(1, delta)
                    statement.setObject(2, player)
                    statement.executeUpdate()
                }
        }
    }

    assertEquals(HttpStatusCode.Created, signUp(OWNER_DEVICE, OWNER_HANDLE, PASSWORD))
    val token = signIn(OWNER_HANDLE, PASSWORD)
    return OwnerFixture(welcome.playerId, token)
}

/** Completes the WebSocket handshake for [deviceId], minting its `player` row. */
private suspend fun HttpClient.handshake(deviceId: String) {
    val welcome = helloWith(deviceId = deviceId, sessionToken = null) as ServerMessage.Welcome
    assertEquals(deviceId, welcome.deviceId)
}

/**
 * Opens one `/ws` connection, sends a [Hello] naming [deviceId] and [sessionToken], and decodes
 * the single frame the handshake answers with — a [ServerMessage.Welcome] on success — leaving the
 * caller to branch on which arrived.
 */
private suspend fun HttpClient.helloWith(deviceId: String?, sessionToken: String?): ServerMessage {
    val session = webSocketSession("/ws")
    session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = deviceId, sessionToken = sessionToken))))
    val frame = session.incoming.receive() as Frame.Text
    return protocolJson.decodeFromString<ServerMessage>(frame.readText())
}

/** Reads the profile the HTTP route resolves for [deviceId]. */
private suspend fun HttpClient.profileOf(deviceId: String): ProfileResponse {
    val response = get("/api/me") { header(DEVICE_ID_HEADER, deviceId) }
    assertEquals(HttpStatusCode.OK, response.status)
    return protocolJson.decodeFromString<ProfileResponse>(response.bodyAsText())
}

/**
 * Reads `GET /api/me` presenting [token] as `Authorization: Bearer <token>`, and [deviceId] as
 * `X-Device-Id` when given. Unlike [profileOf], this does not assert the status: a caller checking
 * a revoked session needs the raw response.
 */
private suspend fun HttpClient.meWithToken(token: String, deviceId: String? = null): HttpResponse =
    get("/api/me") {
        header(HttpHeaders.Authorization, "Bearer $token")
        deviceId?.let { header(DEVICE_ID_HEADER, it) }
    }

/** Signs [deviceId] up with [handle] and [password], returning the response status. */
private suspend fun HttpClient.signUp(deviceId: String, handle: String, password: String): HttpStatusCode {
    val response = post("/api/auth/sign-up") {
        header(DEVICE_ID_HEADER, deviceId)
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"handle":"$handle","password":"$password"}""")
    }
    return response.status
}

/**
 * Signs in with [handle] and [password] over `POST /api/auth/sign-in`, asserts the call
 * succeeded, and returns the plaintext session token — the one moment it is ever readable again.
 */
private suspend fun HttpClient.signIn(handle: String, password: String): String {
    val response = post("/api/auth/sign-in") {
        header(HttpHeaders.ContentType, "application/json")
        setBody("""{"handle":"$handle","password":"$password"}""")
    }
    assertEquals(HttpStatusCode.OK, response.status)
    return protocolJson.decodeFromString<SignInResponse>(response.bodyAsText()).sessionToken
}

/** Revokes the device behind [token] over `DELETE /api/me/device`, returning the response status. */
private suspend fun HttpClient.revokeDevice(token: String): HttpStatusCode {
    val response = delete("/api/me/device") { header(HttpHeaders.Authorization, "Bearer $token") }
    return response.status
}

/** The number of `auth_session` rows currently held by [playerId], read fresh with a plain count. */
private fun DataSource.authSessionCountFor(playerId: UUID): Long {
    connection.use { connection ->
        connection.prepareStatement("SELECT count(*) FROM auth_session WHERE player_id = ?").use { statement ->
            statement.setObject(1, playerId)
            statement.executeQuery().use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }
}
