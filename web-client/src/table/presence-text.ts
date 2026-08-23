import type { SeatPresence } from "../protocol";

/**
 * The line under the table that explains the rival's presence, in `ADR-0046` §2's words.
 *
 * `returned` is the client's own bookkeeping, not a field of any frame: a resuming client is
 * always sent its rival's presence, `PRESENT` included, so `PRESENT` alone cannot tell a
 * return from a status quo. Telling a player their rival is back when the rival never left is
 * the one way this copy can state a falsehood.
 */
export function presenceLine(
  presence: SeatPresence | null,
  returned: boolean,
): string {
  switch (presence) {
    case "AWAY":
      return "Your rival is away. The duel is paused.";
    case "ABSENT":
      return "Your rival did not come back. The duel continues, and the server acts for them.";
    case "PRESENT":
      return returned ? "Your rival is back." : "";
    default:
      return "";
  }
}
