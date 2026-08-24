package duels.poker.server.e2e

import duels.poker.engine.random.SplitMix64Rng
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.Migrations
import duels.poker.server.db.PostgresTestSupport
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duelServer
import duels.poker.server.protocol.Hello
import duels.poker.server.protocol.ProtocolCodec
import duels.poker.server.protocol.ServerMessage
import duels.poker.server.protocol.protocolJson
import duels.poker.server.serverComponents
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromString
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

internal const val HAND_SEED: Long = 0x5EED_000000000001L

/** A fresh, migrated PostgreSQL database with an empty public schema. */
internal fun freshMigratedDatabase(): DataSource {
    val dataSource = PostgresTestSupport.freshDatabase()
    Migrations.migrate(dataSource)
    return dataSource
}

/**
 * A [HandSeedSource] that threads one [SplitMix64Rng], exactly as [playDuel] does: each call
 * returns `draw.value` and keeps `draw.next`. Same seed, same sequence of hand seeds, forever.
 */
internal fun handSeedSource(seed: Long): HandSeedSource {
    var seedRng: SplitMix64Rng = SplitMix64Rng(seed)
    return HandSeedSource {
        val draw = seedRng.nextLong()
        seedRng = draw.next
        draw.value
    }
}

/**
 * A [ServerConfig] built field by field with shipped defaults.
 *
 * The database fields are unused here because the [DataSource] is passed separately.
 */
internal fun e2eConfig(): ServerConfig =
    ServerConfig(
        port = ServerConfig.DEFAULT_PORT,
        maxFrameLength = ServerConfig.DEFAULT_MAX_FRAME_LENGTH,
        maxFrameNestingDepth = ServerConfig.DEFAULT_MAX_FRAME_NESTING_DEPTH,
        databaseUrl = ServerConfig.DEFAULT_DATABASE_URL,
        databaseUser = ServerConfig.DEFAULT_DATABASE_USER,
        databasePassword = ServerConfig.DEFAULT_DATABASE_PASSWORD,
        databasePoolSize = ServerConfig.DEFAULT_DATABASE_POOL_SIZE,
        roomWaitingTimeoutMillis = ServerConfig.DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS,
        roomFinishedTimeoutMillis = ServerConfig.DEFAULT_ROOM_FINISHED_TIMEOUT_MILLIS,
        disconnectGraceMillis = ServerConfig.DEFAULT_DISCONNECT_GRACE_MILLIS,
    )

/**
 * Install the shipping duel server composition against a fresh database and configurable hand seeds.
 *
 * The test never assembles [SocketDependencies] itself — it always calls [duelServer] against
 * components built from the root [serverComponents], so a route the production root forgot to
 * install cannot slip through.
 */
internal fun ApplicationTestBuilder.installDuelServer(dataSource: DataSource, handSeed: Long = HAND_SEED) {
    application {
        duelServer(
            serverComponents(
                e2eConfig(),
                dataSource,
                seeds = handSeedSource(handSeed),
            ),
        )
    }
}

/** Reads the next frame off [this] session as a decoded [ServerMessage]. */
internal suspend fun DefaultClientWebSocketSession.nextServerMessage(): ServerMessage {
    val frame = incoming.receive() as Frame.Text
    return protocolJson.decodeFromString(frame.readText())
}

/**
 * Send a [Hello] handshake naming [deviceId], [sessionToken] or both, and return the server's
 * [ServerMessage.Welcome], failing loudly if the server sends anything else.
 *
 * [sessionToken] defaults to `null`, so every call site that predates it sends exactly the `Hello`
 * it always did and is unaffected by its addition.
 */
internal suspend fun DefaultClientWebSocketSession.completeHandshake(
    deviceId: String?,
    sessionToken: String? = null,
): ServerMessage.Welcome =
    withTimeout(5.seconds) {
        send(Frame.Text(ProtocolCodec.encode(Hello(deviceId = deviceId, sessionToken = sessionToken))))
        val message = nextServerMessage()
        message as? ServerMessage.Welcome
            ?: error("Expected Welcome, got ${message::class.simpleName}")
    }
