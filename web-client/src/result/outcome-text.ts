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
