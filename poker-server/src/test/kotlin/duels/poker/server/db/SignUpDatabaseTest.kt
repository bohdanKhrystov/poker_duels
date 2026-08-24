package duels.poker.server.db

import duels.poker.server.config.ServerConfig
import duels.poker.server.duelServer
import duels.poker.server.http.DEVICE_ID_HEADER
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
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

    @Test
    fun theWinnersCoinIsStillThereAfterSigningUp() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)
                val winner = UUID.fromString(client.profileOf(SIGNING_UP_DEVICE).playerId)
                val loser = UUID.fromString(client.profileOf(BYSTANDER_DEVICE).playerId)

                dataSource.writeFinishedDuel(winner, loser)
                dataSource.assertFixtureTook(winner, loser)

                // Two players, two signs: a balance read as 0 on both sides would pass a test
                // that only checked "unchanged", so both the winner's +1 and the loser's -1 are
                // asserted by value, not by equality with a captured "before" snapshot.
                assertEquals(1, client.profileOf(SIGNING_UP_DEVICE).coinBalance)
                assertEquals(-1, client.profileOf(BYSTANDER_DEVICE).coinBalance)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                assertEquals(1, client.profileOf(SIGNING_UP_DEVICE).coinBalance)
                assertEquals(-1, client.profileOf(BYSTANDER_DEVICE).coinBalance)
            }
        }
    }

    @Test
    fun theLedgerSumsAreUnchangedByASignUp() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)
                val winner = UUID.fromString(client.profileOf(SIGNING_UP_DEVICE).playerId)
                val loser = UUID.fromString(client.profileOf(BYSTANDER_DEVICE).playerId)

                dataSource.writeFinishedDuel(winner, loser)
                dataSource.assertFixtureTook(winner, loser)

                // P2, before and after: a mint and a burn cancel, so an after-only assertion, or
                // a delta assertion, would miss either one landing on the wrong side of a sign-up.
                dataSource.assertCoinInvariantHolds("before sign-up")

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                dataSource.assertCoinInvariantHolds("after sign-up")
            }
        }
    }

    @Test
    fun everyPlayersBalanceStillEqualsTheirDeltas() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)
                val winner = UUID.fromString(client.profileOf(SIGNING_UP_DEVICE).playerId)
                val loser = UUID.fromString(client.profileOf(BYSTANDER_DEVICE).playerId)

                dataSource.writeFinishedDuel(winner, loser)
                dataSource.assertFixtureTook(winner, loser)

                // The wrong implementation this must fail against is any UPDATE player SET
                // coin_balance, or the duel_result repointing ADR-0030 names, both of which
                // leave a row whose balance no longer matches its deltas.
                dataSource.assertCoinInvariantHolds("before sign-up")

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                dataSource.assertCoinInvariantHolds("after sign-up")
            }
        }
    }

    @Test
    fun theWinnerReadsTheSameDuelLineAfterSigningUp() {
        testApplication {
            application { duelServer(serverComponents(config, dataSource)) }
            val client = createClient { install(WebSockets) }

            withTimeout(5.seconds) {
                client.handshake(SIGNING_UP_DEVICE)
                client.handshake(BYSTANDER_DEVICE)
                val winner = UUID.fromString(client.profileOf(SIGNING_UP_DEVICE).playerId)
                val loser = UUID.fromString(client.profileOf(BYSTANDER_DEVICE).playerId)

                val duelId = dataSource.writeFinishedDuel(winner, loser)
                dataSource.assertFixtureTook(winner, loser)

                val before = client.duelsOf(SIGNING_UP_DEVICE)
                assertEquals(1, before.size)
                assertEquals(duelId.toString(), before.single().duelId)
                assertEquals(loser.toString(), before.single().opponentPlayerId)
                assertEquals(1, before.single().coinDelta)

                assertEquals(HttpStatusCode.Created, client.signUp(SIGNING_UP_DEVICE, "Bob_1", PASSWORD))

                // The opponent-side double-count ADR-0030 describes shows up here as two lines
                // where there was one, so size is asserted, not merely non-empty.
                val after = client.duelsOf(SIGNING_UP_DEVICE)
                assertEquals(1, after.size)
                assertEquals(duelId.toString(), after.single().duelId)
                assertEquals(loser.toString(), after.single().opponentPlayerId)
                assertEquals(1, after.single().coinDelta)
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

/** Reads the duel list the HTTP route resolves for [deviceId]. */
private suspend fun HttpClient.duelsOf(deviceId: String): List<DuelSummaryResponse> {
    val response = get("/api/me/duels") { header(DEVICE_ID_HEADER, deviceId) }
    assertEquals(HttpStatusCode.OK, response.status)
    return protocolJson.decodeFromString<RecentDuelsResponse>(response.bodyAsText()).duels
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

/**
 * Writes one finished duel with raw SQL — a `duel` row, **two** `duel_result` rows of `+1` and
 * `-1`, and the matching `coin_balance` update on both players — mirroring exactly what
 * [duels.poker.server.db.PostgresDuelResultStore.record] writes for a real duel.
 *
 * A fixture writing only the winner's row would make P2 fail before the sign-up under test even
 * runs, for a reason that has nothing to do with sign-up — that misdiagnosis is what this helper
 * exists to avoid.
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
 * `+1`, the loser's is `-1`, and there are exactly two `duel_result` rows. Without this, P1 and
 * P2 below could be evaluated against a fixture that silently failed to write — and both
 * properties hold trivially on no data, which is the trap this class guards against.
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
