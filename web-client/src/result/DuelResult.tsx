import type { ReactElement } from "react";
import type { DuelOutcome } from "../protocol";
import { CoinMark } from "./CoinMark";
import {
  coinLine,
  verdictHeadline,
  verdictOf,
  type Verdict,
} from "./outcome-text";

/**
 * The result screen: who won, and the coin.
 *
 * Both verdicts get the same panel, the design's point — losing must not feel
 * like a different, smaller product. Everything on it is read off the
 * `DuelOutcome` the server sent and the seat the server gave this client;
 * nothing here compares a stack, adds a chip or names a hand.
 */
export function DuelResult(props: {
  outcome: DuelOutcome;
  mySeat: number | null;
}): ReactElement {
  const verdict = verdictOf(props.outcome, props.mySeat);
  const coin = coinLine(verdict);
  return (
    <section
      aria-label="the result"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <h2
        className={`text-display leading-tight font-bold ${verdictColour(verdict)}`}
      >
        {verdictHeadline(verdict)}
      </h2>
      {coin !== null && (
        <p className="flex items-center gap-3 font-mono">
          <CoinMark />
          <span className={verdictColour(verdict)}>{coin}</span>
        </p>
      )}
    </section>
  );
}

/**
 * The design's two colours, and neither when there is no side to take: a draw
 * and an unread seat keep the body colour.
 */
function verdictColour(verdict: Verdict): string {
  switch (verdict) {
    case "win":
      return "text-win";
    case "loss":
      return "text-loss";
    case "draw":
    case "unknown":
      return "";
  }
}
