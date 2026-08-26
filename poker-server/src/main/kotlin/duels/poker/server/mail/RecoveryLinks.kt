package duels.poker.server.mail

/**
 * Builds the two mailed recovery links from a configured origin, without reading request headers.
 *
 * Every URL this system will ever mail is built in one function, from configuration, and nothing
 * under `poker-server/src/main` reads a `Host` header. `ADR-0081` makes both links fragment
 * routes on the client's single address — `GET /reset` against a static host with no rewrite rule
 * is a `404`, deterministic and permanent. The token rides as the segment **behind** the slug, in
 * the fragment, so it is still never transmitted, never logged, never in a proxy record and never
 * in a `Referer`.
 */
public class RecoveryLinks(private val baseUrl: String) {
    /**
     * Builds a reset link from the configured origin and the provided token.
     *
     * The result is `baseUrl/#/reset/token` — `ADR-0081` §1's first table row, with the token
     * as a path segment **of the fragment**, not a URL path segment.
     *
     * @param token The reset token, typically a 32-character URL-safe random string
     * @return The complete reset link
     */
    public fun reset(token: String): String = "$baseUrl/#/reset/$token"

    /**
     * Builds a verification link from the configured origin and the provided token.
     *
     * The result is `baseUrl/#/verify/token` — `ADR-0081` §1's second table row, with the token
     * as a path segment **of the fragment**, not a URL path segment.
     *
     * @param token The verification token, typically a 32-character URL-safe random string
     * @return The complete verification link
     */
    public fun verification(token: String): String = "$baseUrl/#/verify/$token"
}
