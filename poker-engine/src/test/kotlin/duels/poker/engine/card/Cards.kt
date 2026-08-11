package duels.poker.engine.card

/**
 * Parse a whitespace-separated list of cards from standard notation.
 *
 * This helper is test-only: production code has no reason to build cards from strings,
 * and giving it one would put a parser on a hot path.
 */
internal fun cards(notation: String): List<Card> {
    val tokens = notation.split(Regex("\\s+")).filter { it.isNotEmpty() }
    require(tokens.isNotEmpty()) { "Card notation cannot be blank" }

    val result = mutableListOf<Card>()
    for (token in tokens) {
        val card = Card.parse(token)
        require(card !in result) { "Duplicate card: $card" }
        result.add(card)
    }
    return result
}
