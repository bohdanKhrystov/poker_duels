package duels.poker.server.session

/**
 * Answers a player's display name for the socket, so the table can name the rival across from
 * it. `null` for a player with no name. A port rather than `ProfileReads` itself, because the
 * socket needs one fact and must not hold a `DataSource` or a whole profile read to get it.
 */
public fun interface DisplayNameLookup {
    public suspend fun nameOf(player: PlayerId): String?

    public companion object {
        /** A lookup that knows no names — every seat reads as unnamed. */
        public val NONE: DisplayNameLookup = DisplayNameLookup { null }
    }
}
