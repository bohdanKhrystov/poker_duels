package duels.poker.server.auth

private const val MIN_LOGIN_HANDLE_CODE_POINTS = 3
private const val MAX_LOGIN_HANDLE_CODE_POINTS = 32
private val PERMITTED_CHARACTERS = setOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '_', '-')
private val PERMITTED_FIRST_CHARACTERS = setOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

/**
 * Canonicalises a login handle the way the handle must be stored before it is persisted to the
 * database, or returns `null` when no canonical form exists for [raw].
 *
 * The steps run in this fixed order — fold to ASCII, then measure, then refuse characters, then
 * refuse leading characters — because the fold must run first to establish what will be stored.
 *
 * A handle is never shown to anybody, so only its canonical form needs to exist. This is the
 * deliberate contrast with display names: a display name is shown, so the form the player typed
 * has to survive and the fold lives in a database index; a handle is never displayed, so only its
 * canonical form needs to exist and there is no second representation to disagree with.
 *
 * @param raw the value as the player typed it
 * @return the folded handle (ASCII uppercase to lowercase), when its length is between 3 and 32
 *   **code points** — not [String.length], which counts UTF-16 units and over-counts astral
 *   characters — and it contains only ASCII characters from `[a-z0-9._-]` with the first character
 *   from `[a-z0-9]`; `null` otherwise
 */
public fun loginHandleOrNull(raw: String): String? {
    // Fold ASCII uppercase to lowercase (A-Z to a-z by code point, nothing else)
    val folded = asciiLowercase(raw)

    // Check length in code points
    val codePoints = folded.codePointCount(0, folded.length)
    if (codePoints < MIN_LOGIN_HANDLE_CODE_POINTS || codePoints > MAX_LOGIN_HANDLE_CODE_POINTS) {
        return null
    }

    // Check that all characters are permitted
    if (!containsOnlyPermittedCharacters(folded)) {
        return null
    }

    // Check that the first character is permitted for the first position
    val firstCodePoint = folded.codePointAt(0)
    if (firstCodePoint > 127 || !PERMITTED_FIRST_CHARACTERS.contains(firstCodePoint.toChar())) {
        return null
    }

    return folded
}

/**
 * Folds ASCII uppercase letters to lowercase by code point, leaving all other characters
 * unchanged. This is not locale-sensitive: `U+0130` (LATIN CAPITAL LETTER I WITH DOT ABOVE)
 * is left unchanged (not folded to `i`), and `U+212A` (KELVIN SIGN) is left unchanged (not
 * folded to `k`). Only the ASCII range `A`–`Z` (`U+0041`–`U+005A`) are folded to their
 * lowercase equivalents `a`–`z` (`U+0061`–`U+007A`).
 */
private fun asciiLowercase(raw: String): String {
    val result = StringBuilder(raw.length)
    var offset = 0
    while (offset < raw.length) {
        val codePoint = raw.codePointAt(offset)
        when {
            // A-Z (U+0041 to U+005A) → a-z (U+0061 to U+007A)
            codePoint in 0x0041..0x005A -> result.append((codePoint + 32).toChar())
            else -> result.appendCodePoint(codePoint)
        }
        offset += Character.charCount(codePoint)
    }
    return result.toString()
}

/**
 * Checks that all characters in the given string are from the permitted set `[a-z0-9._-]`.
 * This assumes the string has already been folded to lowercase.
 */
private fun containsOnlyPermittedCharacters(folded: String): Boolean {
    var offset = 0
    while (offset < folded.length) {
        val codePoint = folded.codePointAt(offset)
        // Only permit ASCII characters (code point < 128)
        if (codePoint > 127 || !PERMITTED_CHARACTERS.contains(codePoint.toChar())) {
            return false
        }
        offset += Character.charCount(codePoint)
    }
    return true
}
