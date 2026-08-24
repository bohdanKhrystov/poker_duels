package duels.poker.server.session

import duels.poker.server.auth.IdentityResolver
import duels.poker.server.room.RoomRegistry

/**
 * The collaborators a `/ws` socket handler needs.
 *
 * Bundling them into a single parameter means a story that adds a collaborator changes this type
 * and the route's parameter list, not the call sites in every socket test.
 *
 * [maxFrameLength] and [maxFrameNestingDepth] carry `ServerConfig`'s frame limits down to every
 * [duels.poker.server.protocol.ProtocolCodec.decodeClient] call the socket makes. There are
 * deliberately no default values here: a caller that forgets these two fields must fail to
 * compile, not silently inherit whatever the codec happens to default to.
 */
public data class SocketDependencies(
    val directory: PlayerDirectory,
    val deviceIds: DeviceIdSource,
    val sessions: SessionRegistry,
    val rooms: RoomRegistry,
    val connections: ConnectionDirectory,
    val maxFrameLength: Int,
    val maxFrameNestingDepth: Int,
    val identities: IdentityResolver,
)
