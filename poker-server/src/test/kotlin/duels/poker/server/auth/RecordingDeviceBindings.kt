package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A recording test double for [DeviceBindings] that records all invocations for assertion.
 *
 * This double appends a [RevokeCall] to [revokeCalls] for each invocation of [revoke],
 * allowing tests to verify the exact sequence of calls and their arguments. The list grows
 * monotonically; no calls are discarded or overwritten.
 */
internal class RecordingDeviceBindings : DeviceBindings {
    /** A recording of all calls made to [revoke], in order. */
    val revokeCalls: MutableList<RevokeCall> = mutableListOf()

    override suspend fun revoke(playerId: PlayerId, keeping: SessionToken) {
        revokeCalls.add(RevokeCall(playerId, keeping))
    }
}

/**
 * A single invocation of [RecordingDeviceBindings.revoke].
 *
 * @param playerId The player whose binding was revoked.
 * @param keeping The session token that was kept.
 */
data class RevokeCall(
    val playerId: PlayerId,
    val keeping: SessionToken,
)
