package duels.poker.server.e2e

import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.StandingsResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.DeviceId
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.sql.DataSource

private const val LADDER_LIMIT = 10

/**
 * The ladder is readable over HTTP from inside the same running application that hosts the
 * two WebSocket clients. Two seated players who have finished nothing are on no ladder and hold
 * no place — the *before* half of every later assertion in this story.
 *
 * Every criterion in this story is a difference, and a difference needs a before: [ADR-0061] §4
 * *"the ladder is results, not players"* so two profiles that exist and have duelled nobody
 * appear on no row. [ADR-0065] §4's three answers distinguish "known, and placed nowhere" from
 * "who are you", and both are asserted here because a `self` that collapsed them would still look
 * right on every later test in this class.
 */
@Timeout(120)
internal class SocketLadderTest {
    private lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        PostgresTestSupport.requireDocker()
        dataSource = freshMigratedDatabase()
    }

    /**
     * Reads the standings from `/api/standings` with the given parameters, asserting the response
     * is `200`, then decodes the body with [protocolJson] directly — the same manual decode every
     * other e2e test in this package uses for frames off a socket, never client-side content
     * negotiation.
     *
     * @param deviceId The device ID to send in the `X-Device-Id` header. If `null`, no header is sent.
     * @param limit The maximum number of rows to return. Defaults to [LADDER_LIMIT].
     * @param after The cursor for pagination. If `null`, not included in the query string.
     */
    private suspend fun HttpClient.ladder(
        deviceId: String?,
        limit: Int = LADDER_LIMIT,
        after: String? = null,
    ): StandingsResponse {
        val queryParams = mutableListOf("limit=$limit")
        if (after != null) {
            queryParams.add("after=$after")
        }
        val query = queryParams.joinToString("&")
        val response = get("/api/standings?$query") {
            if (deviceId != null) {
                header(DEVICE_ID_HEADER, deviceId)
            }
        }
        assertEquals(
            HttpStatusCode.OK,
            response.status,
            "GET /api/standings for deviceId=$deviceId returned ${response.status}",
        )
        return protocolJson.decodeFromString(response.bodyAsText())
    }

    @Test
    fun theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay(): Unit = runBlocking {
        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }
            val duel = client.openSocketDuel()

            // Get the profiles of both players for comparison
            val hostPlayerId = PostgresProfileReads(dataSource).profileOf(DeviceId(HOST_DEVICE))
                ?.playerId
                ?: error("Host profile not found")
            val guestPlayerId = PostgresProfileReads(dataSource).profileOf(DeviceId(GUEST_DEVICE))
                ?.playerId
                ?: error("Guest profile not found")

            // Request 1: Host device
            val hostStandings = client.ladder(HOST_DEVICE)
            assertEquals(true, hostStandings.rows.isEmpty(), "rows should be empty for host device")
            assertEquals(hostPlayerId, hostStandings.self?.playerId, "self.playerId should match host")
            assertEquals(null, hostStandings.self?.rank, "self.rank should be null for host")
            assertEquals(null, hostStandings.self?.coins, "self.coins should be null for host")
            assertEquals(
                true,
                hostStandings.season.isNotBlank(),
                "season should be non-blank",
            )

            // Request 2: Guest device
            val guestStandings = client.ladder(GUEST_DEVICE)
            assertEquals(true, guestStandings.rows.isEmpty(), "rows should be empty for guest device")
            assertEquals(guestPlayerId, guestStandings.self?.playerId, "self.playerId should match guest")
            assertEquals(null, guestStandings.self?.rank, "self.rank should be null for guest")
            assertEquals(null, guestStandings.self?.coins, "self.coins should be null for guest")

            // Request 3: Unknown device
            val strangerStandings = client.ladder("e2e-stranger")
            assertEquals(true, strangerStandings.rows.isEmpty(), "rows should be empty for unknown device")
            assertNull(strangerStandings.self, "self should be null for unknown device")

            // Request 4: No device ID header
            val noHeaderStandings = client.ladder(null)
            assertEquals(true, noHeaderStandings.rows.isEmpty(), "rows should be empty for no header")
            assertNull(noHeaderStandings.self, "self should be null for no header")
        }
    }
}
