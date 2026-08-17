package duels.poker.server.auth

import duels.poker.server.session.PlayerId

/**
 * A kind of credential identifying what sort of secret proves a player's identity.
 *
 * A value class, not an enum. `ADR-0041` says `"password"` is the only kind v0.1 writes, and
 * the door stays open to future kinds without recompilation. Any other kind is constructible at
 * the call site.
 */
@JvmInline
public value class CredentialKind(public val value: String) {
    public companion object {
        /**
         * The password credential kind, the only kind v0.1 writes.
         */
        public val PASSWORD: CredentialKind = CredentialKind("password")
    }
}

/**
 * A port for proving and storing credentials that name and authenticate a player.
 *
 * **Nothing on this port returns a hash, and nothing ever will.** Hashes are secrets and
 * must never leave the storage layer. This port exposes `verify` returning a `PlayerId?` —
 * the identity proven — not a hash for the caller to compare. Verification runs on the
 * storage side, where the hash lives.
 *
 * **`identifier` has already been folded** by `loginHandleOrNull`, exactly as `ProfileWrites`
 * takes a `canonicalName`. The caller is responsible for the fold; this port does not
 * re-fold. A port that did would be a second place the rule lives, and the fold rule would
 * have two implementations to keep in sync.
 *
 * **`verify` answers `null` for both an unknown identifier and a wrong secret**, deliberately.
 * See `ADR-0027` §6: the distinction tells a stranger nothing, protecting identifiers from
 * enumeration attacks.
 */
public interface Credentials {
    /**
     * Verify a presented credential and return the player it names, or null.
     *
     * An unknown identifier and a wrong secret both answer `null`. No path distinguishes them.
     *
     * @param kind The kind of credential to verify.
     * @param identifier The folded identifier to verify against. The caller has already applied
     *   `loginHandleOrNull` if this kind uses a login handle.
     * @param presented The presented secret to verify.
     * @return The `PlayerId` this credential names, or `null` if the identifier is unknown or
     *   the secret is wrong.
     */
    public suspend fun verify(
        kind: CredentialKind,
        identifier: String,
        presented: PresentedSecret,
    ): PlayerId?

    /**
     * Store a new credential for a player.
     *
     * @param playerId The player to create a credential for.
     * @param kind The kind of credential to create.
     * @param identifier The folded identifier to store. The caller has already applied
     *   `loginHandleOrNull` if this kind uses a login handle.
     * @param secret The presented secret to hash and store.
     * @return A [CreateCredentialResult] describing the outcome.
     */
    public suspend fun create(
        playerId: PlayerId,
        kind: CredentialKind,
        identifier: String,
        secret: PresentedSecret,
    ): CreateCredentialResult
}

/**
 * The answer to a request to create a new credential.
 *
 * A sealed type because callers must act on the outcomes differently: the credential was
 * created successfully, or the folded identifier is already taken by another player.
 */
public sealed interface CreateCredentialResult {
    /**
     * The credential was created successfully.
     */
    public object Created : CreateCredentialResult

    /**
     * The folded identifier is already taken by another player.
     *
     * The same identifier cannot be held by two players. This answers `409 Conflict`.
     * The player may retry with a different identifier.
     */
    public object IdentifierTaken : CreateCredentialResult
}
