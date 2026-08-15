import type { DuelOutcome } from "../protocol";

/** How the duel ended, from the reader's side of the table. */
export type Verdict = "win" | "loss" | "draw" | "unknown";

/**
 * The verdict, read off the two fields that carry it: the winner the server
 * named, and the seat this client was given in `RoomJoined`.
 *
 * Comparing the final stacks would reach the same answer almost always and be
 * a client asserting a game fact — exactly what `EPIC-03` forbids. A draw is
 * decided first, because `winner: null` is a draw for whoever is reading it,
 * seat or no seat (`ADR-0015`).
 */
export function verdictOf(
  outcome: DuelOutcome,
  mySeat: number | null,
): Verdict {
  if (outcome.winner === null) return "draw";
  if (mySeat === null) return "unknown";
  return outcome.winner === mySeat ? "win" : "loss";
}

/** The verdict in the design's words (`design/screens/duel-end.html`). */
export function verdictHeadline(verdict: Verdict): string {
  switch (verdict) {
    case "win":
      return "Victory";
    case "loss":
      return "Defeat";
    case "draw":
      return "Draw";
    case "unknown":
      return "Duel over";
  }
}

/**
 * What the coin did, in the one figure `ADR-0014` fixes: the winner gains one,
 * the loser loses one, a draw moves none. `null` is "say nothing" — a draw
 * prints no coin line at all, because a coin that did not move is not news.
 *
 * This is stated, not counted. The *balance* is the server's and is read from
 * `GET /api/me` (`STORY-0311`); a client that added this delta to a number it
 * held would be asserting a fact about the economy.
 */
export function coinLine(verdict: Verdict): string | null {
  switch (verdict) {
    case "win":
      return "+1 duel coin";
    case "loss":
      return "−1 duel coin";
    case "draw":
    case "unknown":
      return null;
  }
}
