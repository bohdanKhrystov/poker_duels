package duels.poker.server.auth

private const val MAX_ADDRESS_CODE_POINTS = 254
private const val AT_CODE_POINT = 0x0040

/**
 * Accepts an address the way [ADR-0078](../adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md)
 * §1 specifies: a string that holds an `@` which is neither its first nor its last code point,
 * holds no ASCII control character, and is at most 254 code points long — returning it **unchanged** —
 * and answers `null` for everything else.
 *
 * The rule runs in this order: count, then check structure, then check characters.
 *
 * Clause 4 (no ASCII control character) is not a syntax rule and should not be read as one.
 * No `addr-spec` contains an ASCII control character in any position, so it denies no mailbox
 * that exists. It is here because a line terminator inside an address is the one thing this
 * predicate could pass to an unwritten transport that would harm somebody who is not a player
 * of this game.
 *
 * @param raw the address as the player typed it
 * @return the address unchanged when all five clauses hold: (1) `raw` contains at least one `@`;
 *   (2) its first code point is not `@`; (3) its last code point is not `@`; (4) it contains no
 *   ASCII control character — nothing in `U+0000`–`U+001F`, and not `U+007F`; and (5) it is at
 *   most 254 code points long; `null` otherwise
 */
public fun emailAddressOrNull(raw: String): EmailAddress? {
    // Check length in code points first
    val codePoints = raw.codePointCount(0, raw.length)
    if (codePoints > MAX_ADDRESS_CODE_POINTS) {
        return null
    }

    // Check for at least one @ (clause 1)
    if ('@' !in raw) {
        return null
    }

    // Check that first code point is not @ (clause 2)
    val firstCodePoint = raw.codePointAt(0)
    if (firstCodePoint == AT_CODE_POINT) {
        return null
    }

    // Check that last code point is not @ (clause 3)
    val lastCodePoint = raw.codePointBefore(raw.length)
    if (lastCodePoint == AT_CODE_POINT) {
        return null
    }

    // Check for ASCII control characters (clause 4)
    var offset = 0
    while (offset < raw.length) {
        val codePoint = raw.codePointAt(offset)
        // U+0000–U+001F and U+007F are ASCII control characters
        if ((codePoint in 0x0000..0x001F) || codePoint == 0x007F) {
            return null
        }
        offset += Character.charCount(codePoint)
    }

    // All clauses passed; return the address unchanged
    return EmailAddress(raw)
}
