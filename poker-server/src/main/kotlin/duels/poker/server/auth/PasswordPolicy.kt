package duels.poker.server.auth

import java.text.Normalizer

private const val MINIMUM_PASSWORD_CODE_POINTS = 8
private const val MAXIMUM_PASSWORD_CODE_POINTS = 128

/**
 * The NFC-normalised form of [secret].
 *
 * NFC, never NFKC (`ADR-0048` §5): NFKC folds `ﬁ` (U+FB01 LATIN SMALL LIGATURE FI) to `f` + `i`,
 * and folds fullwidth digits such as `５` to `5` — strings a player can tell apart — so it would
 * silently store a different secret from the one that was set. This is the only call to
 * [Normalizer] this project makes for a secret; `Argon2Hasher` calls this function rather than
 * adding a second one.
 *
 * @param secret the secret as presented, before normalisation
 * @return [secret]'s value in Unicode Normalization Form C
 */
internal fun nfcNormalisedSecret(secret: PresentedSecret): String =
    Normalizer.normalize(secret.value, Normalizer.Form.NFC)

/**
 * Whether [secret], in its NFC form, has at least 8 code points.
 *
 * A sign-up and reset rule (`ADR-0048` §2). Never applied at sign-in — a later increase to this
 * minimum must not lock out accounts whose secret was set under an earlier one.
 *
 * Counts with `codePointCount`, never [String.length]: [String.length] counts UTF-16 units, so
 * four astral characters (each a surrogate pair) would measure 8 there though they are only 4
 * code points.
 *
 * @param secret the secret to check, normalised internally before counting
 * @return `true` when the NFC form of [secret] has at least 8 code points
 */
public fun passwordIsLongEnough(secret: PresentedSecret): Boolean {
    val normalised = nfcNormalisedSecret(secret)
    return normalised.codePointCount(0, normalised.length) >= MINIMUM_PASSWORD_CODE_POINTS
}

/**
 * Whether [secret], in its NFC form, has at most 128 code points.
 *
 * Applied wherever a secret is hashed, including sign-in (`ADR-0048` §2) — which is why this is a
 * separate function from [passwordIsLongEnough] rather than one combined predicate: the minimum
 * must never run at sign-in, and the maximum must always run there.
 *
 * Counts with `codePointCount`, never [String.length], for the same reason as
 * [passwordIsLongEnough].
 *
 * @param secret the secret to check, normalised internally before counting
 * @return `true` when the NFC form of [secret] has at most 128 code points
 */
public fun passwordIsWithinTheWorkBound(secret: PresentedSecret): Boolean {
    val normalised = nfcNormalisedSecret(secret)
    return normalised.codePointCount(0, normalised.length) <= MAXIMUM_PASSWORD_CODE_POINTS
}
