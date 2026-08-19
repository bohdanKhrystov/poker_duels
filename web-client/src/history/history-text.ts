export const HISTORY_HEADING = "Your duels";

export const LOADING_RECORD = "Loading your duels…";

export const NO_DUELS = "No duels yet.";

export const NO_MATCH = "No duel matches this.";

export const READ_FAILED =
  "Your duels did not load. Reload the page to try again.";

export const MORE = "Show more";

export const OUTCOME_LEGEND = "Outcome";

export const EVERY_OUTCOME = "All";

export const OPPONENT_LABEL = "Opponent name";

/**
 * The sentence describing an empty state in the history screen.
 *
 * When `filtered` is true, the player has applied a filter and no duel matched it.
 * When `filtered` is false, the player has played no duels at all. These are two different
 * facts about the world (STORY-0413), and this function is the single place that branches
 * on them — just as `nameOrNone` is the only place that branches on a null display name
 * (ADR-0058 §2). A component that chose between them inline would be a second place able
 * to get it wrong, and drift between two surfaces would be how the screen tells lies.
 */
export function emptyLine(filtered: boolean): string {
  if (filtered) {
    return NO_MATCH;
  }
  return NO_DUELS;
}
