import type { ReactElement } from "react";
import type { ProfileStripState } from "./profile-strip";
import {
  coinBalanceText,
  coinDeltaText,
  finishedAtText,
  outcomeWord,
} from "./profile-text";

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
      // The ordinary state of a first visit, not an error.
      return (
        <section
          aria-label="your profile"
          className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
        >
          <p className="text-small">No profile yet.</p>
        </section>
      );

    case "profile":
      // State the balance as the server signed it.
      return (
        <section
          aria-label="your profile"
          className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
        >
          <p className="font-mono text-small">
            {coinBalanceText(state.profile.coinBalance)} Duel coins
          </p>
          {state.duels.length === 0 ? (
            <p className="text-small">No duels yet.</p>
          ) : (
            <ul className="w-full text-small">
              {state.duels.map((duel) => (
                <li
                  key={duel.duelId}
                  className="border-t border-hairline py-3 first:border-t-0"
                >
                  <p>
                    {outcomeWord(duel.outcome)} {coinDeltaText(duel.coinDelta)}{" "}
                    {duel.handsPlayed}{" "}
                    {duel.handsPlayed === 1 ? "hand" : "hands"}{" "}
                    {finishedAtText(duel.finishedAt)}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </section>
      );
  }
}
