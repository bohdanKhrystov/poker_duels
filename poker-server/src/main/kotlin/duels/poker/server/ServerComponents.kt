package duels.poker.server

import duels.poker.server.auth.Credentials
import duels.poker.server.auth.IdentityResolver
import duels.poker.server.config.ServerConfig
import duels.poker.server.db.PostgresAuthSessions
import duels.poker.server.db.PostgresCredentials
import duels.poker.server.db.PostgresDuelResultSink
import duels.poker.server.db.PostgresDuelResultStore
import duels.poker.server.db.PostgresPlayerDirectory
import duels.poker.server.db.PostgresProfileReads
import duels.poker.server.db.PostgresProfileWrites
import duels.poker.server.db.PostgresStandingsReads
import duels.poker.server.duel.HandSeedSource
import duels.poker.server.duel.SecureHandSeedSource
import duels.poker.server.http.ProfileReads
import duels.poker.server.http.ProfileWrites
import duels.poker.server.http.StandingsReads
import duels.poker.server.room.RandomRoomCodeSource
import duels.poker.server.room.RoomRegistry
import duels.poker.server.session.ConnectionDirectory
import duels.poker.server.session.DeviceIdSource
import duels.poker.server.session.RandomDeviceIdSource
import duels.poker.server.session.SessionRegistry
import duels.poker.server.session.SocketDependencies
import duels.poker.server.time.ServerClock
import duels.poker.server.time.SystemClock
import java.time.Clock
import javax.sql.DataSource

/**
 * The real collaborators a shipping server needs: database-backed directories, registries, and
 * sources, wired together with the given configuration and data source.
 */
public data class ServerComponents(
    val socket: SocketDependencies,
    val reads: ProfileReads,
    val writes: ProfileWrites,
    val credentials: Credentials,
    val standings: StandingsReads,
    val wallClock: Clock,
    val identities: IdentityResolver,
)

/**
 * Build the real server components from configuration and a data source.
 *
 * This function constructs all the collaborators the server runs on: the room registry with its
 * real result sink, the Postgres-backed player directory and profile reads, and the device ID and
 * room code sources. The result is ready to use immediately — this function opens no pool, runs
 * no migration and installs no route.
 *
 * @param config The server configuration, including database coordinates and frame limits.
 * @param dataSource The data source to use for all database operations.
 * @param clock The clock to use for room timestamps. Defaults to [SystemClock] in production;
 *   tests inject a fixed clock for determinism.
 * @param seeds The source of hand seeds for new duels. Defaults to [SecureHandSeedSource] in
 *   production; tests inject a fixed source for reproducibility.
 * @return A complete set of server components ready to use.
 */
public fun serverComponents(
    config: ServerConfig,
    dataSource: DataSource,
    clock: ServerClock = SystemClock,
    seeds: HandSeedSource = SecureHandSeedSource(),
    wallClock: Clock = Clock.systemUTC(),
): ServerComponents {
    val directory = PostgresPlayerDirectory(dataSource)
    val reads = PostgresProfileReads(dataSource)
    val writes = PostgresProfileWrites(dataSource)
    val identities = IdentityResolver(PostgresAuthSessions(dataSource, wallClock), directory)
    val deviceIds: DeviceIdSource = RandomDeviceIdSource()
    val sessions = SessionRegistry()
    val connections = ConnectionDirectory()
    val rooms = RoomRegistry(
        RandomRoomCodeSource(),
        clock,
        config.roomTimeouts(),
        seeds,
        PostgresDuelResultSink(PostgresDuelResultStore(dataSource), wallClock),
    )

    val socket = SocketDependencies(
        directory = directory,
        deviceIds = deviceIds,
        sessions = sessions,
        rooms = rooms,
        connections = connections,
        maxFrameLength = config.maxFrameLength,
        maxFrameNestingDepth = config.maxFrameNestingDepth,
    )

    val credentials = PostgresCredentials(dataSource)
    val standings = PostgresStandingsReads(dataSource)

    return ServerComponents(
        socket = socket,
        reads = reads,
        writes = writes,
        credentials = credentials,
        standings = standings,
        wallClock = wallClock,
        identities = identities,
    )
}
