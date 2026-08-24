package duels.poker.server

import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.protocol.protocolJson
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.delete
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import kotlin.time.Duration.Companion.seconds

/**
 * Tests that [duelServer] installs the socket, HTTP profile routes, and plugins in one
 * application, all backed by a single database.
 *
 * Each test starts with a fresh, migrated database to ensure isolation.
 */
class DuelServerRoutesTest {
    private lateinit var dataSource: org.postgresql.ds.PGSimpleDataSource
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
    fun theHealthRouteStillAnswers(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun theProfileRouteIsInstalled(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.get("/api/me")
        // A route that was never installed answers 404, which makes this falsifiable
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun theSocketRouteCompletesAHandshake(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "wired"))))
            val frame = session.incoming.receive() as Frame.Text
            val welcome = protocolJson.decodeFromString<ServerMessage>(frame.readText())
            assertEquals("wired", (welcome as ServerMessage.Welcome).deviceId)
        }
    }

    @Test
    fun aHandshakeCreatesTheProfileTheHttpRouteThenReads(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // Complete the handshake to create the profile
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "wired"))))
            val frame = session.incoming.receive() as Frame.Text
            val welcome = protocolJson.decodeFromString<ServerMessage>(frame.readText())
            assertEquals("wired", (welcome as ServerMessage.Welcome).deviceId)

            // Now read the profile via HTTP with the same device ID
            val profileResponse = client.get("/api/me") {
                header(DEVICE_ID_HEADER, "wired")
            }
            assertEquals(HttpStatusCode.OK, profileResponse.status)
            val profile = protocolJson.decodeFromString<ProfileResponse>(profileResponse.bodyAsText())
            assertNotNull(profile)
            assertEquals(0, profile.coinBalance)

            // Read the duels for the same device; one database serves both routes
            val duelsResponse = client.get("/api/me/duels") {
                header(DEVICE_ID_HEADER, "wired")
            }
            assertEquals(HttpStatusCode.OK, duelsResponse.status)
            val recentDuels = protocolJson.decodeFromString<RecentDuelsResponse>(duelsResponse.bodyAsText())
            assertNotNull(recentDuels)
            assertEquals(0, recentDuels.duels.size)
        }
    }

    @Test
    fun theSignUpRouteIsInstalled(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.post("/api/auth/sign-up") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"alice","password":"password1"}""")
        }
        // A route that was never installed answers 404, which makes this falsifiable
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun aHandshakeThenASignUpAnswersCreated(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val client = createClient { install(WebSockets) }

        withTimeout(5.seconds) {
            // Complete the handshake to mint the profile for device "wired"
            val session = client.webSocketSession("/ws")
            session.send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = "wired"))))
            val frame = session.incoming.receive() as Frame.Text
            val welcome = protocolJson.decodeFromString<ServerMessage>(frame.readText())
            assertEquals("wired", (welcome as ServerMessage.Welcome).deviceId)

            // Now sign up with that device id, a fresh handle and an 8-code-point password
            val signUpResponse = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "wired")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"password1"}""")
            }
            assertEquals(HttpStatusCode.Created, signUpResponse.status)
        }
    }

    @Test
    fun theStandingsRouteAnswersWithoutAProfile(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.get("/api/standings")
        assertEquals(HttpStatusCode.OK, response.status)
        val standings = protocolJson.decodeFromString<StandingsResponse>(response.bodyAsText())
        assertEquals(0, standings.rows.size)
        assertEquals(null, standings.nextCursor)
        assertEquals(null, standings.self)
        // Season should match YYYY-MM format
        assert(standings.season.matches(Regex("""\d{4}-\d{2}""")))
    }

    @Test
    fun theDeviceRouteIsInstalled(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.delete("/api/me/device")
        // A route that was never installed answers 404, which makes this falsifiable
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun theDeviceRouteIsNotInstalledOnAnyOtherVerb(): Unit = testApplication {
        application { duelServer(serverComponents(config, dataSource)) }
        val response = client.get("/api/me/device")
        // Only DELETE was asked for (ADR-0049 §5); assert not OK and not Unauthorized
        assert(response.status != HttpStatusCode.OK)
        assert(response.status != HttpStatusCode.Unauthorized)
    }
}
