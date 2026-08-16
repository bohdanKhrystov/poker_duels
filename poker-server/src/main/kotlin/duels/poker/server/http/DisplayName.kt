package duels.poker.server.http

import java.text.Normalizer

private const val MIN_DISPLAY_NAME_CODE_POINTS = 1
private const val MAX_DISPLAY_NAME_CODE_POINTS = 32

/**
 * Canonicalises a display name the way ADR-0029 §2 requires the database to store it, or returns
 * `null` when no canonical form exists for [raw].
 *
 * The steps run in this fixed order — trim, then normalise to NFC, then measure — because
 * normalising can change the code-point count. Measuring before normalising would accept a name
 * that the database's `player_display_name_nfc` and length `CHECK`s then refuse.
 *
 * @param raw the value as the player typed it
 * @return the trimmed, NFC-normalised name, when its length is between 1 and 32 **code points**
 *   — not [String.length], which counts UTF-16 units and over-counts astral characters — or
 *   `null` when trimming leaves nothing, or the canonical form falls outside that bound
 */
public fun canonicalDisplayNameOrNull(raw: String): String? {
    val trimmed = raw.trim()
    val canonical = Normalizer.normalize(trimmed, Normalizer.Form.NFC)
    val codePoints = canonical.codePointCount(0, canonical.length)
    if (codePoints < MIN_DISPLAY_NAME_CODE_POINTS || codePoints > MAX_DISPLAY_NAME_CODE_POINTS) {
        return null
    }
    return canonical
}
