package duels.poker.server.http

import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.DeviceBindings
import duels.poker.server.auth.Identity
import duels.poker.server.auth.IdentityResolver
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.routing

/**
 * Installs `DELETE /api/me/device`, the route through which a signed-in player revokes their
 * device's standing as a credential (`ADR-0037`).
 *
 * Identity is resolved through [identities], exactly as `profileRoutes` resolves it, but this
 * route does **not** reuse the helper the other three route files share for turning an
 * [Identity] into a player — that helper answers a player for [Identity.Device] too, and here
 * that is wrong. **A session is required; a device id, however well it resolves, is not enough**
 * (`ADR-0049` §5): a caller with no session has no screen to keep alive, so revoking for them
 * would sign them out of the page they are standing on — the hostility `ADR-0037` forbids.
 * Requiring a session also makes revocation a step-up operation for free, since the password
 * behind it was proved minutes ago, by construction. So [Identity.Device],
 * [Identity.UnknownDevice], [Identity.Refused] and [Identity.Anonymous] all answer
 * `401 Unauthorized` with an empty body; only [Identity.Session] reaches [credentials].
 *
 * The request carries no body, and none is read.
 *
 * Once identity is confirmed, [credentials]' `holdsCredential` decides whether the player holds a
 * password credential at all (`ADR-0049` §5). A player who holds none answers `409 Conflict` with
 * an empty body, and [bindings] is never called — a profile whose only route in is the device can
 * never be stranded by revoking it. The two guards answer different questions: the session
 * requirement above is about not stranding the caller's *screen*, this one is about not stranding
 * the *profile* — so the order is fixed, identity then credential then write, and neither guard
 * may stand in for the other.
 *
 * On success, [bindings]' `revoke` is called for the resolved player, `keeping` the same session
 * token the caller presented, and the route answers `204 No Content` — whether or not a live
 * binding existed to revoke (`ADR-0049` §5), since a distinct answer would tell a caller which
 * bindings exist.
 *
 * @param identities The port that resolves a session token or a device id into a player.
 * @param credentials The port checked, once identity is confirmed, for a password credential; a
 *     player holding none is refused `409` before [bindings] is ever touched.
 * @param bindings The port that revokes a player's device binding and signs out its other sessions.
 */
public fun Application.deviceRoutes(
    identities: IdentityResolver,
    credentials: Credentials,
    bindings: DeviceBindings,
) {
    routing {
        delete("/api/me/device") {
            val token = call.sessionTokenOrNull()
            // Why a session, not a device (ADR-0049 §5): a caller with no session has no screen
            // to keep alive, so revoking for them would sign them out of the page they are
            // standing on — the hostility ADR-0037 forbids. It also makes revocation a step-up
            // operation for free: the password behind the session was proved minutes ago.
            val playerId = when (val identity = identities.resolve(token, call.deviceIdOrNull())) {
                is Identity.Session -> identity.playerId
                is Identity.Device -> null
                is Identity.UnknownDevice -> null
                is Identity.Refused -> null
                is Identity.Anonymous -> null
            }
            if (playerId == null) {
                call.respond(HttpStatusCode.Unauthorized)
                return@delete
            }
            // Why here, and not before identity or after the write (ADR-0049 §5): the token
            // guard above is about not stranding the caller's screen; this one is about not
            // stranding the profile. Checked first, an unauthenticated caller would learn from a
            // 409 that some profile holds no credential. Checked after the write, it would
            // revoke before refusing.
            if (!credentials.holdsCredential(playerId, CredentialKind.PASSWORD)) {
                call.respond(HttpStatusCode.Conflict)
                return@delete
            }
            // token is non-null here: identities.resolve only answers Identity.Session — the one
            // branch above that yields a non-null playerId — when a non-null token was presented.
            bindings.revoke(playerId, keeping = checkNotNull(token))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
