import type { SetNameOutcome } from "./set-name";

export const NAME_REMOVED_HEADING = "Your display name was removed.";

export const NAME_REMOVED_BODY =
  "A person running Poker Duels removed it — not a bug, and not another player. " +
  "That name cannot be used again, by you or by anyone. " +
  "Choose a new one whenever you like.";

export const PERMANENCE_LINE =
  "A name is chosen once. You cannot change it later, and it can be taken away.";

/**
 * The sentence describing why a name could not be set.
 *
 * Each refusal kind has its own sentence, addressing the player directly
 * and accounting for what the player cannot tell from the HTTP status alone.
 * The sentences ship in `ADR-0052` and are the exact strings this module
 * returns — they are golden.
 */
export function refusalSentence(
  kind: Exclude<SetNameOutcome["kind"], "named">,
): string {
  switch (kind) {
    case "rejected":
      return "That name cannot be used. Try another.";
    case "conflict":
      return "That name is not available. Try another.";
    case "permanent":
      return "You already have a display name. That choice is permanent and cannot be changed.";
    case "no-profile":
      return "This browser has no profile. Reload the page and try again.";
    case "unavailable":
      return "That did not reach the server. Reload the page to see whether the name was set.";
  }
}

/**
 * Whether a refusal leaves the player free to try a different name.
 *
 * A `rejected` or `conflict` response means the player should try again
 * with a different name. All other refusals mean the player cannot proceed
 * until something else changes — their profile, their browser's state, or
 * the server's availability. Retrying without that change would be pointless.
 */
export function mayTryAgain(
  kind: Exclude<SetNameOutcome["kind"], "named">,
): boolean {
  switch (kind) {
    case "rejected":
    case "conflict":
      return true;
    case "permanent":
    case "no-profile":
    case "unavailable":
      return false;
  }
}
