package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import duels.poker.server.duelServer
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.serverComponents
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

private const val SIGNING_UP_DEVICE = "signer-device"
private const val BYSTANDER_DEVICE = "bystander-device"
private const val PASSWORD = "password1"

/**
 * Boots the shipped server — `duelServer(serverComponents(config, dataSource))`, exactly as
 * [duels.poker.server.DuelServerRoutesTest] does — drives `POST /api/auth/sign-up` over HTTP, and
 * reads the result back with raw SQL, never through a repository, which would only prove the
 * write path agrees with its own read path.
 *
 * Every test seeds two `player` rows through two independent WebSocket handshakes and signs up
 * only one of them. With a single row, a snapshot cannot tell "nothing changed" from "the only
 * row was rewritten to the same thing", and cannot catch a write landing on the wrong player.
 */
class SignUpDatabaseTest {
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
    fun aSignUpWritesExactlyOneCredentialRow() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)
                val profile = client.profileOf(SIGNING_UP_DEVICE)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                assertEquals(1L, dataSource.rowCount("credential"))
                dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT player_id, kind, identifier FROM credential").use { rs ->
                            assertTrue(rs.next())
                            assertEquals(profile.playerId, rs.getString("player_id"))
                            assertEquals("password", rs.getString("kind"))
                            assertEquals("bob_1", rs.getString("identifier"))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun theStoredSecretIsAPhcStringAndNotThePassword() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                val storedHash = dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT secret_hash FROM credential").use { rs ->
                            assertTrue(rs.next())
                            rs.getString("secret_hash")
                        }
                    }
                }

                // Three separate failure modes: gibberish would still fail to equal or contain the
                // password, and some other project's PHC string would still parse.
                assertNotEquals(PASSWORD, storedHash)
                assertFalse(storedHash.contains(PASSWORD))
                assertNotNull(parseArgon2PhcOrNull(storedHash))
            }
        }
    }

    @Test
    fun noSecondPlayerRowAppears() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)

                val before = dataSource.rowCount("player")
                assertEquals(2L, before)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                assertEquals(before, dataSource.rowCount("player"))
            }
        }
    }

    @Test
    fun thePlayerTableIsTheSameMultisetAfterTheSignUp() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)

                val before = dataSource.snapshot("player")
                assertEquals(2, before.rows.size)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                // The whole row list, column names and all — not one column of one row — so an
                // UPDATE, an extra INSERT, or a duel_result copy that renumbers a player all fail.
                assertEquals(before, dataSource.snapshot("player"))
            }
        }
    }

    @Test
    fun noDuelResultRowIsWrittenMovedOrDeleted() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)

                assertEquals(0L, dataSource.rowCount("duel_result"))

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                assertEquals(0L, dataSource.rowCount("duel_result"))
            }
        }
    }
}

/** Completes the WebSocket handshake for [deviceId], minting its `player` row. */
private suspend fun HttpClient.handshake(deviceId: String) {
    val session = webSocketSession("/ws")
    session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = deviceId))))
    val frame = session.incoming.receive() as Frame.Text
    val welcome = protocolJson.decodeFromString<ServerMessage>(frame.readText())
    assertEquals(deviceId, (welcome as ServerMessage.Welcome).deviceId)
}

/** Reads the profile the HTTP route resolves for [deviceId]. */
private suspend fun HttpClient.profileOf(deviceId: String): ProfileResponse {
    val response = get("/api/me") { header(DEVICE_ID_HEADER, deviceId) }
    assertEquals(HttpStatusCode.OK, response.status)
    return protocolJson.decodeFromString<ProfileResponse>(response.bodyAsText())
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

/** Every column of every row of [table], ordered by `id` for a deterministic comparison. */
private data class TableSnapshot(val columns: List<String>, val rows: List<List<Any?>>)

/**
 * Reads [table] back as a generic snapshot: column names and count come from [java.sql.ResultSetMetaData],
 * never a hard-coded list, so this keeps comparing correctly across a migration that adds or drops
 * a column — `ADR-0049` drops `player.device_id` in `STORY-0406`.
 */
private fun DataSource.snapshot(table: String): TableSnapshot {
    connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM $table ORDER BY id").use { rs ->
                val metaData = rs.metaData
                val columns = (1..metaData.columnCount).map { metaData.getColumnName(it) }
                val rows = mutableListOf<List<Any?>>()
                while (rs.next()) {
                    rows.add(columns.indices.map { rs.getObject(it + 1) })
                }
                return TableSnapshot(columns, rows)
            }
        }
    }
}
