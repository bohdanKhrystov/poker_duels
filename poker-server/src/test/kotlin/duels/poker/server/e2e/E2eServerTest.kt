package duels.poker.server.e2e

import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.http.SignInResponse
import duels.poker.server.protocol.protocolJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@Timeout(120)
internal class E2eServerTest {
    @Test
    fun theSameSeedGivesTheSameHandSeeds() {
        val seed = 7L
        val source1 = handSeedSource(seed)
        val source2 = handSeedSource(seed)

        val first1 = source1.newHandSeed()
        val first2 = source2.newHandSeed()

        val second1 = source1.newHandSeed()
        val second2 = source2.newHandSeed()

        val third1 = source1.newHandSeed()
        val third2 = source2.newHandSeed()

        // Both sources yield equal first three values
        assert(first1 == first2) { "First seeds should be equal" }
        assert(second1 == second2) { "Second seeds should be equal" }
        assert(third1 == third2) { "Third seeds should be equal" }

        // And those three values are not all equal to each other
        assert(setOf(first1, second1, third1).size > 1) {
            "Not all three seeds should be equal"
        }
    }

    @Test
    fun theInstalledServerWritesToTheDatabaseGiven() {
        PostgresTestSupport.requireDocker()
        val dataSource = freshMigratedDatabase()

        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            val session = client.webSocketSession("/ws")
            session.completeHandshake("probe")

            // Verify that a player was written to the database
            dataSource.connection.use { connection ->
                connection.createStatement().use { stmt ->
                    val resultSet = stmt.executeQuery("SELECT COUNT(*) FROM player")
                    resultSet.next()
                    val count = resultSet.getInt(1)
                    assert(count == 1) { "Expected 1 player in database, got $count" }
                }
            }
        }
    }

    @Test
    fun aHandshakeUnderASessionTokenSeatsTheSessionsPlayer() {
        PostgresTestSupport.requireDocker()
        val dataSource = freshMigratedDatabase()

        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            // Two devices: "d-a" is the one behind the token, "d-b" is the device id the Hello
            // carries alongside it. With only one device, a Hello whose session route silently fell
            // back to its own header would still seat the same player, and this test could not tell
            // the two routes apart.
            val aWelcome = client.webSocketSession("/ws").completeHandshake(deviceId = "d-a")
            val bWelcome = client.webSocketSession("/ws").completeHandshake(deviceId = "d-b")

            val signUpStatus = client.signUp(deviceId = "d-a", handle = "Signer_1", password = "password1")
            assert(signUpStatus == HttpStatusCode.Created) { "sign-up for d-a returned $signUpStatus" }
            val token = client.signIn(handle = "Signer_1", password = "password1")

            val welcome = client.webSocketSession("/ws").completeHandshake(deviceId = "d-b", sessionToken = token)

            assert(welcome.playerId == aWelcome.playerId) {
                "expected the token to seat d-a's player (${aWelcome.playerId}), got ${welcome.playerId}"
            }
            assert(welcome.playerId != bWelcome.playerId) {
                "expected the token to outrank d-b's own player (${bWelcome.playerId}), but the " +
                    "Welcome named it"
            }
            assert(welcome.deviceId == null) {
                "expected a session-borne Welcome to carry deviceId = null, got ${welcome.deviceId}"
            }
        }
    }

    @Test
    fun aHandshakeWithNoTokenStillSeatsTheDevice() {
        PostgresTestSupport.requireDocker()
        val dataSource = freshMigratedDatabase()

        testApplication {
            installDuelServer(dataSource)
            val client = createClient { install(WebSockets) }

            // The control for the test above: with no token at all, completeHandshake's deviceId
            // argument alone must still resolve the same player on a second connection — a harness
            // that silently dropped that argument (always sending null) would mint a fresh player
            // every time instead of resolving back to the same one.
            val first = client.webSocketSession("/ws").completeHandshake(deviceId = "d-b")
            val second = client.webSocketSession("/ws").completeHandshake(deviceId = "d-b")

            assert(second.playerId == first.playerId) {
                "expected a second handshake naming device d-b, with no token, to seat the same " +
                    "player (${first.playerId}) as the first, got ${second.playerId}"
            }
        }
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
     * Signs in with [handle] and [password] over `POST /api/auth/sign-in`, asserts the call
     * succeeded, and returns the plaintext session token.
     */
    private suspend fun HttpClient.signIn(handle: String, password: String): String {
        val response = post("/api/auth/sign-in") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"handle":"$handle","password":"$password"}""")
        }
        assert(response.status == HttpStatusCode.OK) { "sign-in for $handle returned ${response.status}" }
        return protocolJson.decodeFromString<SignInResponse>(response.bodyAsText()).sessionToken
    }
}
