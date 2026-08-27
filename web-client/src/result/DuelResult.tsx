import type { ReactElement, ReactNode } from "react";
import type { DuelOutcome } from "../protocol";
import { CoinMark } from "./CoinMark";
import {
  coinLine,
  verdictHeadline,
  verdictOf,
  type Verdict,
} from "./outcome-text";
import { formatChips } from "../table/chips";

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
  /**
   * An optional rematch control. A panel with no control is a real state, not an
   * omission — a client holding no seat has nothing to press, and every test
   * that is about the verdict passes nothing. The panel does not know why;
   * it renders what it is handed.
   */
  rematch?: ReactNode;
  /**
   * An optional account offer (`ADR-0036`). The panel does not decide whether one
   * is due — it renders what it is handed, exactly as it does for `rematch`.
   */
  offer?: ReactNode;
  /**
   * An optional handler called before the way back link navigates. The link
   * stays an `<a href="/">`, so the handler runs and navigation stays the
   * browser's. Storage operations are synchronous, so a handler that forgets
   * has finished before the page leaves.
   */
  onLeave?: () => void;
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
      <p className="text-small text-text-muted">
        {metaLine(props.outcome, props.mySeat)}
      </p>
      {props.rematch}
      {props.offer}
      <a
        className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
        href="/"
        onClick={props.onLeave}
      >
        Back to the lobby
      </a>
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

/**
 * The duel's ledger: the hand count, then one entry per final stack, in the
 * order the server sent them. Mapped rather than indexed — the wire says
 * `finalStacks` is an array and says nothing about its length, so the line
 * states what arrived instead of assuming two.
 *
 * The owner words are the table's own (`DuelTable`), and they need a seat: a
 * client that does not know which side it sat on states the stacks plainly
 * rather than guessing which is whose.
 */
function metaLine(outcome: DuelOutcome, mySeat: number | null): string {
  const hands = `${outcome.handsPlayed} ${outcome.handsPlayed === 1 ? "hand" : "hands"}`;
  const stacks = outcome.finalStacks.map((stack, seat) =>
    mySeat === null
      ? formatChips(stack)
      : `${seat === mySeat ? "You" : "Your rival"} ${formatChips(stack)}`,
  );
  return [hands, ...stacks].join(" · ");
}
