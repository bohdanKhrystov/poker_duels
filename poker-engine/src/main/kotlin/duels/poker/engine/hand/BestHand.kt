package duels.poker.engine.hand

import duels.poker.engine.card.Card

/**
 * The winning five of a seven-card hand, and the rank they make.
 *
 * [cards] exists because the client highlights the winning five at showdown, and recomputing
 * them there would duplicate this logic in a place that could disagree with it.
 */
public data class BestHand(val rank: HandRank, val cards: List<Card>)
