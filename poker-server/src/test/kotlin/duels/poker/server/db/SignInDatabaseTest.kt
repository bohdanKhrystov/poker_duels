package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import duels.poker.server.duelServer
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ProtocolError
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.serverComponents
import duels.poker.server.session.PlayerId
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

private const val OWNER_DEVICE = "d-owner"
private const val OTHER_DEVICE = "d-other"
private const val OWNER_HANDLE = "Owner_1"
private const val PASSWORD = "password1"

/** The byte length of a SHA-256 digest — a fact about the algorithm, not about this codebase. */
private const val HASH_BYTE_LENGTH = 32

/**
 * Boots the shipped server — `duelServer(serverComponents(config, dataSource))`, exactly as
 * [SignUpDatabaseTest] does — and drives `STORY-0405`'s whole identity flow against a real schema:
 * a real sign-up, a real sign-in, a token that reads the right profile over HTTP and seats the
 * right player on the socket, and a sign-out that answers `204` twice. Every token is presented on
 * a request separate from the one that issued it — the plaintext crosses only through the
 * `auth_session` row Postgres holds, never through an in-process object shared between them — so
 * this is the one place the whole story composes rather than each piece being proven alone.
 *
 * Every test seeds two independent profiles: `d-owner` signs up and wins a duel, landing a coin
 * balance of `1`; `d-other` is left an untouched anonymous profile with a balance of `-1`. With a
 * single profile, or two profiles that both read `0`, a fixture cannot tell a session's row
 * resolving to the right player from a device id resolving to it by coincidence (`ADR-0030` §5).
 */
class SignInDatabaseTest {
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
    fun aCorrectCredentialAnswersATokenThatReadsThatPlayersProfile() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (ownerId, _) = client.establishTwoProfiles(dataSource)

                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                val response = client.meWithToken(token)
                assertEquals(HttpStatusCode.OK, response.status)
                val profile = protocolJson.decodeFromString<ProfileResponse>(response.bodyAsText())
                assertEquals(ownerId.toString(), profile.playerId)
                assertEquals(1, profile.coinBalance)

                // Not that the stored digest matches SHA-256(token) — PostgresAuthSessionsTest
                // already proves that in isolation — but that the token's own bytes are nowhere
                // in the row the database actually holds.
                val storedHash = dataSource.soleAuthSessionTokenHash()
                assertEquals(HASH_BYTE_LENGTH, storedHash.size)
                assertFalse(storedHash.contentEquals(token.toByteArray(Charsets.UTF_8)))
                // ISO-8859-1 is a lossless byte<->char mapping for every byte value, so this
                // catches the token appearing anywhere in the stored bytes, not only a whole-value
                // match under a different encoding.
                assertFalse(String(storedHash, Charsets.ISO_8859_1).contains(token))
            }
        }
    }

    @Test
    fun theTokenOutranksTheDeviceItTravelsWith() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (ownerId, _) = client.establishTwoProfiles(dataSource)
                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                // The token travels beside d-other's own device id — the session must still win.
                val response = client.meWithToken(token, deviceId = OTHER_DEVICE)
                assertEquals(HttpStatusCode.OK, response.status)
                val profile = protocolJson.decodeFromString<ProfileResponse>(response.bodyAsText())
                assertEquals(ownerId.toString(), profile.playerId)
                assertEquals(1, profile.coinBalance)
            }
        }
    }

    @Test
    fun theDevicesOwnProfileIsUnchangedByAnyOfIt() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (_, otherId) = client.establishTwoProfiles(dataSource)
                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                // The signed-in reads this guards against leaving a trace: with and without
                // d-other's own device id riding beside the token.
                assertEquals(HttpStatusCode.OK, client.meWithToken(token).status)
                assertEquals(HttpStatusCode.OK, client.meWithToken(token, deviceId = OTHER_DEVICE).status)

                val profile = client.profileOf(OTHER_DEVICE)
                assertEquals(otherId.toString(), profile.playerId)
                assertEquals(-1, profile.coinBalance)
            }
        }
    }

    @Test
    fun signingInWritesNothingToPlayer() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.establishTwoProfiles(dataSource)

                val before = dataSource.snapshot("player")
                assertEquals(2, before.rows.size)

                client.signIn(OWNER_HANDLE, PASSWORD)

                // The whole row list, column names and all — not one column of one row — so an
                // UPDATE landing on either player's row fails this exactly as SignUpDatabaseTest's
                // own multiset check catches a write on the credential side.
                assertEquals(before, dataSource.snapshot("player"))
            }
        }
    }

    @Test
    fun theSocketSeatsTheSessionsPlayer() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (ownerId, _) = client.establishTwoProfiles(dataSource)
                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                val welcome = client.helloWith(deviceId = OTHER_DEVICE, sessionToken = token) as ServerMessage.Welcome
                assertEquals(ownerId.toString(), welcome.playerId)
                assertNull(welcome.deviceId)
            }
        }
    }

    @Test
    fun anExpiredSessionIsRefusedOverHttpAndOnTheSocket() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (ownerId, _) = client.establishTwoProfiles(dataSource)

                // Written directly through PostgresAuthSessions on a clock 31 days back — the
                // lifetime is 30, so this row is already a day past its expiresAt the moment it is
                // written, without this test ever sleeping or moving the JVM's own clock at read
                // time: the predicate that refuses it is Postgres's own `now()`.
                val expiredClock = Clock.fixed(Instant.now().minus(31, DAYS), ZoneOffset.UTC)
                val expiredToken = PostgresAuthSessions(dataSource, expiredClock).issue(PlayerId(ownerId.toString()))

                val response = client.meWithToken(expiredToken.value)
                assertEquals(HttpStatusCode.Unauthorized, response.status)

                val message = client.helloWith(deviceId = null, sessionToken = expiredToken.value)
                assertEquals(ProtocolError.INVALID_SESSION, (message as ServerMessage.Failure).error)
            }
        }
    }

    @Test
    fun signingOutAnswersTwoHundredAndFourTwiceAndThenTheDeviceIsItselfAgain() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (_, otherId) = client.establishTwoProfiles(dataSource)
                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                assertEquals(HttpStatusCode.NoContent, client.signOut(token))
                // An already-deleted token is not an error — the second call answers exactly the
                // first's 204, never a 404 that would tell a caller which tokens exist.
                assertEquals(HttpStatusCode.NoContent, client.signOut(token))

                // ADR-0030 §3's restore is subtraction, with nothing to restore here: d-other was
                // never touched, so its own device id alone reads exactly what it always did.
                val profile = client.profileOf(OTHER_DEVICE)
                assertEquals(otherId.toString(), profile.playerId)
                assertEquals(-1, profile.coinBalance)
            }
        }
    }

    @Test
    fun aBrowserThatSignsInHavingNeverConnectedGetsNoDeviceAndNoRow() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                val (ownerId, _) = client.establishTwoProfiles(dataSource)
                val token = client.signIn(OWNER_HANDLE, PASSWORD)

                val before = dataSource.rowCount("player")
                assertEquals(2L, before)

                // No deviceId at all — a browser that has never completed a handshake holds none
                // to send. Identity.Anonymous would mint a fresh player row for this if the
                // session did not win outright before a device id is ever read.
                val welcome = client.helloWith(deviceId = null, sessionToken = token) as ServerMessage.Welcome
                assertEquals(ownerId.toString(), welcome.playerId)
                assertNull(welcome.deviceId)
                assertEquals(before, dataSource.rowCount("player"))
            }
        }
    }
}

/** [UUID]s for the fixture's two profiles, returned by [establishTwoProfiles]. */
private data class TwoProfiles(val ownerId: UUID, val otherId: UUID)

/**
 * Seeds the two devices and two profiles every test in this file needs: `d-owner` signs up and
 * wins a duel, landing a coin balance of `1`; `d-other` is left an untouched anonymous profile
 * with a balance of `-1`. Returns before any sign-in happens, so a caller's own "before" snapshot
 * of `player` is taken after every write this fixture makes and none the test under it makes.
 */
private suspend fun HttpClient.establishTwoProfiles(dataSource: DataSource): TwoProfiles {
    handshake(OWNER_DEVICE)
    handshake(OTHER_DEVICE)
    val ownerId = UUID.fromString(profileOf(OWNER_DEVICE).playerId)
    val otherId = UUID.fromString(profileOf(OTHER_DEVICE).playerId)
    dataSource.writeFinishedDuel(ownerId, otherId)
    dataSource.assertFixtureTook(ownerId, otherId)
    assertEquals(HttpStatusCode.Created, signUp(OWNER_DEVICE, OWNER_HANDLE, PASSWORD))
    return TwoProfiles(ownerId, otherId)
}

/** Completes the WebSocket handshake for [deviceId], minting its `player` row. */
private suspend fun HttpClient.handshake(deviceId: String) {
    val welcome = helloWith(deviceId = deviceId, sessionToken = null) as ServerMessage.Welcome
    assertEquals(deviceId, welcome.deviceId)
}

/**
 * Opens one `/ws` connection, sends a [Hello] naming [deviceId] and [sessionToken], and decodes
 * the single frame the handshake answers with — a [ServerMessage.Welcome] on success or a
 * [ServerMessage.Failure] on a refused session — leaving the caller to branch on which arrived.
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
 * `X-Device-Id` when given — the header a client keeps sending whether or not it holds a token
 * (`ADR-0030` §8). Unlike [profileOf], this does not assert the status: a caller checking an
 * expired session needs the raw response.
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

/** Signs out [token] over `POST /api/auth/sign-out`, returning the response status. */
private suspend fun HttpClient.signOut(token: String): HttpStatusCode {
    val response = post("/api/auth/sign-out") { header(HttpHeaders.Authorization, "Bearer $token") }
    return response.status
}

/** The row count of [table], read fresh with a plain count query. */
private fun DataSource.rowCount(table: String): Long {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT count(*) FROM $table").use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }
}

/** Every column of every row of a table, ordered by `id` for a deterministic comparison. */
private data class TableRows(val columns: List<String>, val rows: List<List<Any?>>)

/**
 * Reads [table] back as a generic snapshot: column names and count come from
 * [java.sql.ResultSetMetaData], never a hard-coded list, so this keeps comparing correctly across
 * a migration that adds or drops a column — mirrors [SignUpDatabaseTest]'s own copy of this
 * helper, under a different class name so the two files' top-level classes do not collide.
 */
private fun DataSource.snapshot(table: String): TableRows {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM $table ORDER BY id").use { rs ->
                val metaData = rs.metaData
                val columns = (1..metaData.columnCount).map { metaData.getColumnName(it) }
                val rows = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    rows.add(columns.indices.map { rs.getObject(it + 1) })
                }
                return TableRows(columns, rows)
            }
        }
    }
}

/**
 * Writes one finished duel with raw SQL — a `duel` row, **two** `duel_result` rows of `+1` and
 * `-1`, and the matching `coin_balance` update on both players — mirroring exactly what
 * [duels.poker.server.db.PostgresDuelResultStore.record] writes for a real duel, exactly as
 * [SignUpDatabaseTest]'s own copy of this helper does.
 *
 * @return the id of the `duel` row written.
 */
private fun DataSource.writeFinishedDuel(winner: UUID, loser: UUID): UUID {
    val duelId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    connection.use { connection ->
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
        listOf(winner to 1, loser to -1).forEach { (player, delta) ->
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
    return duelId
}

/**
 * Asserts the fixture [writeFinishedDuel] wrote actually landed: the winner's `coin_balance` is
 * `+1`, the loser's is `-1`, and there are exactly two `duel_result` rows — without this, a
 * silently failed fixture write would let every balance assertion below hold trivially on no data,
 * exactly as [SignUpDatabaseTest]'s own copy of this helper guards against.
 */
private fun DataSource.assertFixtureTook(winner: UUID, loser: UUID) {
    assertEquals(2L, rowCount("duel_result"))
    assertEquals(1, balanceOf(winner))
    assertEquals(-1, balanceOf(loser))
}

/** The `coin_balance` of [player], read fresh with a plain select. */
private fun DataSource.balanceOf(player: UUID): Int {
    connection.use { connection ->
        connection.prepareStatement("SELECT coin_balance FROM player WHERE id = ?").use { statement ->
            statement.setObject(1, player)
            statement.executeQuery().use { rs ->
                assertTrue(rs.next())
                return rs.getInt("coin_balance")
            }
        }
    }
}

/**
 * The `token_hash` bytes of the sole row in `auth_session`, read as the `bytea` column it actually
 * is. No test in this file signs in more than once, so "the" row is unambiguous — enforced here,
 * not assumed, by asserting there is no second row.
 */
private fun DataSource.soleAuthSessionTokenHash(): ByteArray {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT token_hash FROM auth_session").use { rs ->
                assertTrue(rs.next())
                val hash = rs.getBytes("token_hash")
                assertFalse(rs.next())
                return hash
            }
        }
    }
}
