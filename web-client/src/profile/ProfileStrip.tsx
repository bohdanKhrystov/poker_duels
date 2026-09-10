import type { ReactElement } from "react";
import type { ProfileStripState } from "./profile-strip";
import {
  coinBalanceText,
  coinDeltaText,
  finishedAtText,
  outcomeWord,
} from "./profile-text";
import { isStandInName, nameOrNone } from "./name-text";
import { CoinMark } from "../result/CoinMark";

/**
 * The profile strip: states the duel coin balance, or announces no profile yet,
 * or renders nothing when the read did not land.
 *
 * Renders as a prop-driven presentation over `ProfileStripState`, with no hooks,
 * fetching, or state. The read that fills it runs outside the tree.
 */
export function ProfileStrip(props: {
  readonly state: ProfileStripState;
}): ReactElement | null {
  const { state } = props;

  switch (state.kind) {
    case "unavailable":
      // Do not announce every failed background read; the player cannot act on it.
      return null;

    case "no-profile":
      // The ordinary state of a first visit, not an error — and a player who has not named
      // themselves is not told about an absence they did not ask about (ADR-0125 §5).
      return null;

    case "profile":
      // State the balance as the server signed it.
      return (
        <section
          aria-label="your profile"
          className="mx-auto flex w-full max-w-[380px] flex-col gap-3 rounded-medium border border-hairline bg-surface px-5 py-4"
        >
          <div className="flex items-center justify-between gap-4">
            <p
              className={`min-w-0 truncate font-medium ${
                isStandInName(state.profile.displayName)
                  ? "text-text-muted"
                  : ""
              }`}
            >
              {nameOrNone(state.profile.displayName)}
            </p>
            <p className="flex shrink-0 items-center gap-2 font-mono text-small tabular-nums">
              <CoinMark />
              <span>
                {coinBalanceText(state.profile.coinBalance)} Duel coins
              </span>
            </p>
          </div>
          {state.duels.length === 0 ? (
            <p className="text-small text-text-muted">No duels yet.</p>
          ) : (
            <ul className="w-full text-small">
              {state.duels.map((duel) => (
                <li
                  key={duel.duelId}
                  className="flex flex-col gap-1 border-t border-hairline py-2 first:border-t-0"
                >
                  <span className="flex items-baseline gap-3">
                    <span
                      className={`w-[3.5em] shrink-0 font-medium ${outcomeColour(duel.outcome)}`}
                    >
                      {outcomeWord(duel.outcome)}
                    </span>
                    <span className="w-[2.5em] shrink-0 font-mono tabular-nums">
                      {coinDeltaText(duel.coinDelta)}
                    </span>{" "}
                    <span className="min-w-0 flex-1 truncate text-text-muted">
                      {duel.handsPlayed}{" "}
                      {duel.handsPlayed === 1 ? "hand" : "hands"} vs{" "}
                      {nameOrNone(duel.opponentDisplayName)}
                    </span>
                  </span>
                  <span className="text-micro text-text-faint">
                    {finishedAtText(duel.finishedAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      );
  }
}

/** The result screen's two colours, and neither for a draw. */
function outcomeColour(outcome: "WON" | "LOST" | "DREW"): string {
  switch (outcome) {
    case "WON":
      return "text-win";
    case "LOST":
      return "text-loss";
    case "DREW":
      return "";
  }
}
