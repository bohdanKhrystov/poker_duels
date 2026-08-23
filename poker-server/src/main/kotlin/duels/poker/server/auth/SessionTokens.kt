package duels.poker.server.auth

import java.security.SecureRandom
import java.util.Base64

private const val SESSION_TOKEN_BYTES = 32

/**
 * Mints opaque session tokens that authenticate requests to a game server.
 *
 * The token itself is not hashed here; it is a bearer token, and the stored form (SHA-256) lives
 * in `duels.poker.server.db` per `ADR-0027` §2. This class owns only the generation of the fresh
 * token from entropy, not its persistence or verification against a stored hash.
 *
 * [random] is a constructor parameter — not a default read at call time — so a test can inject a
 * source whose output it controls and pin the exact string [newToken] produces, while production
 * keeps a fresh [SecureRandom] per instance.
 */
public class SessionTokens(private val random: SecureRandom = SecureRandom()) {
    /**
     * Generates a new session token: 256 bits of entropy (32 bytes), encoded as URL-safe base64
     * with no padding.
     *
     * Produces exactly 43 characters per invocation.
     */
    public fun newToken(): SessionToken {
        val bytes = ByteArray(SESSION_TOKEN_BYTES)
        random.nextBytes(bytes)
        return SessionToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
    }
}
