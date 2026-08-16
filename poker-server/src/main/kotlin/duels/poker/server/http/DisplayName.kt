package duels.poker.server.http

import java.text.Normalizer

private const val MIN_DISPLAY_NAME_CODE_POINTS = 1
private const val MAX_DISPLAY_NAME_CODE_POINTS = 32
private const val SPACE_CODE_POINT = ' '.code

/**
 * Canonicalises a display name the way ADR-0029 §§2–3 require the database to store it, or
 * returns `null` when no canonical form exists for [raw].
 *
 * The steps run in this fixed order — trim, then normalise to NFC, then measure, then refuse the
 * characters §3 names — because normalising can change the code-point count, and a name must be
 * judged, character by character, as it will actually be stored. Measuring or refusing before
 * normalising would accept a name that the database's `player_display_name_nfc` and length
 * `CHECK`s then refuse.
 *
 * @param raw the value as the player typed it
 * @return the trimmed, NFC-normalised name, when its length is between 1 and 32 **code points**
 *   — not [String.length], which counts UTF-16 units and over-counts astral characters — and it
 *   contains none of the code points §3 refuses (every `Cc`/`Cf` character, any whitespace other
 *   than `U+0020`, and two or more consecutive `U+0020`); `null` otherwise
 */
public fun canonicalDisplayNameOrNull(raw: String): String? {
    val trimmed = raw.trim()
    val canonical = Normalizer.normalize(trimmed, Normalizer.Form.NFC)
    val codePoints = canonical.codePointCount(0, canonical.length)
    if (codePoints < MIN_DISPLAY_NAME_CODE_POINTS || codePoints > MAX_DISPLAY_NAME_CODE_POINTS) {
        return null
    }
    if (!containsOnlyPermittedCharacters(canonical)) {
        return null
    }
    return canonical
}

/**
 * ADR-0029 §3: a name that renders as another name is a spoof and is then permanent, so this
 * refuses every code point in Unicode category `Cc` (control) or `Cf` (format) — that alone
 * covers `U+0000`–`U+001F` (so tab and newline need no separate rule), zero-width characters and
 * bidirectional overrides, and `U+FEFF` — plus any whitespace other than `U+0020`, and two or
 * more consecutive `U+0020`. Testing the Unicode category, rather than a fixed list, is what
 * keeps the next zero-width character from arriving unrefused.
 *
 * This is deliberately not a script or alphabet rule: nothing here inspects which letters a name
 * uses, only whether a code point is invisible, foreign whitespace, or a doubled space.
 */
private fun containsOnlyPermittedCharacters(canonical: String): Boolean {
    var previousWasSpace = false
    var offset = 0
    while (offset < canonical.length) {
        val codePoint = canonical.codePointAt(offset)
        val type = Character.getType(codePoint)
        if (type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()) {
            return false
        }
        if (codePoint == SPACE_CODE_POINT) {
            if (previousWasSpace) {
                return false
            }
            previousWasSpace = true
        } else {
            if (Character.isSpaceChar(codePoint)) {
                return false
            }
            previousWasSpace = false
        }
        offset += Character.charCount(codePoint)
    }
    return true
}
