package duels.poker.server.http

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.DeviceId
import duels.poker.server.session.PlayerId

/**
 * A credentials double that records every `create` and `holdsCredential` call.
 *
 * All four arguments to `create` are captured in a data class so later tickets can assert
 * the identifier was folded and the player id came from the server, not the request body.
 * This double never calls `verify` during sign-up.
 */
internal class RecordingCredentials(
    private val createResult: CreateCredentialResult = CreateCredentialResult.Created,
    private val holds: Boolean = false,
) : Credentials {
    val createCalls: MutableList<CreateCall> = mutableListOf()
    val holdsCalls: MutableList<Pair<PlayerId, CredentialKind>> = mutableListOf()

    data class CreateCall(
        val playerId: PlayerId,
        val kind: CredentialKind,
        val identifier: String,
        val secret: PresentedSecret,
    )

    override suspend fun verify(
        kind: CredentialKind,
        identifier: String,
        presented: PresentedSecret,
    ): PlayerId? {
        throw UnsupportedOperationException("Sign-up never verifies credentials")
    }

    override suspend fun create(
        playerId: PlayerId,
        kind: CredentialKind,
        identifier: String,
        secret: PresentedSecret,
    ): CreateCredentialResult {
        createCalls.add(CreateCall(playerId, kind, identifier, secret))
        return createResult
    }

    override suspend fun holdsCredential(playerId: PlayerId, kind: CredentialKind): Boolean {
        holdsCalls.add(playerId to kind)
        return holds
    }
}

/**
 * A profile reads double that returns profiles from a fixed map and records every device id
 * it is asked about.
 *
 * Returns `null` for unknown device ids and an empty list for any player's duels.
 */
internal class FixedProfileReads(
    private val profiles: Map<String, ProfileResponse>,
) : ProfileReads {
    val queried: MutableList<String> = mutableListOf()

    override suspend fun profileOf(deviceId: DeviceId): ProfileResponse? {
        queried.add(deviceId.value)
        return profiles[deviceId.value]
    }

    override suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse> {
        return emptyList()
    }
}
