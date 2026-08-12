package duels.poker.server.session

/**
 * The collaborators a `/ws` socket handler needs.
 *
 * Bundling them into a single parameter means a story that adds a collaborator changes this type
 * and the route's parameter list, not the call sites in every socket test.
 */
public data class SocketDependencies(
    val directory: PlayerDirectory,
    val deviceIds: DeviceIdSource,
    val sessions: SessionRegistry,
)
