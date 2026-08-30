import { useCallback, useEffect, useReducer, type ReactElement } from "react";
import { nameOrNone } from "../profile/name-text";
import { coinBalanceText } from "../profile/profile-text";
import { CoinMark } from "../result/CoinMark";
import type { LadderRead } from "./ladder-read";
import { ladderReducer, initialLadder, nextPageAfter } from "./ladder-state";
import {
  LADDER_HEADING,
  LOADING_LADDER,
  EMPTY_LADDER,
  LADDER_FAILED,
  MORE,
  seasonName,
  selfLine,
} from "./ladder-text";

/**
 * The ladder screen: it asks for the first page of the season standings
 * through the read it is handed, and prints exactly the rows that page
 * carried, in the order it carried them. A *Show more* control asks for
 * each further page the same way, appending under what is already shown.
 *
 * Holds `useReducer(ladderReducer, initialLadder())` and calls `read` once,
 * on mount; it constructs nothing of the browser's fetch and touches none
 * of the browser's storage, so nothing here can reach Node's inert global
 * under Vitest.
 *
 * `read` is injected, exactly as `HistoryScreen`'s is, and must be a stable
 * reference — it is the effect's only dependency, so a `read` whose identity
 * changes on every render would ask again on every render.
 *
 * A row's `rank` is printed exactly as the page carried it, never derived
 * from the row's position in the list — a real ladder repeats and skips
 * ranks, and renumbering by position is wrong everywhere but page one
 * (`ADR-0064` §2).
 */
export function LadderScreen(props: {
  readonly read: (after: string | null) => Promise<LadderRead>;
}): ReactElement {
  const { read } = props;
  const [state, dispatch] = useReducer(ladderReducer, initialLadder());

  const ask = useCallback(
    async (after: string | null): Promise<void> => {
      dispatch({ type: "asked", after });
      const result = await read(after);
      if (result.kind === "page") {
        dispatch({ type: "page", page: result.page });
      } else {
        dispatch({ type: "failed" });
      }
    },
    [read],
  );

  useEffect(() => {
    ask(null);
  }, [ask]);

  // Determine which sentence to show based on phase and rows
  let sentence: string | null = null;
  if (state.phase === "loading") {
    sentence = LOADING_LADDER;
  } else if (state.phase === "failed") {
    sentence = LADDER_FAILED;
  } else if (state.phase === "ready" && state.rows.length === 0) {
    sentence = EMPTY_LADDER;
  }
  // ready + rows → no sentence

  // Read once and reuse for both the render decision and the click's
  // argument below — asking from a second read of `nextPageAfter` is how
  // the two end up disagreeing about which page comes next.
  const after = nextPageAfter(state);
  // A request in flight does not move `nextCursor`, so `after` alone would
  // still name the same page while one is loading. Without this phase term
  // a second press would ask for it again: TASK-050305 showed, with real
  // output, that `askedWith` is a single slot with no per-request identity,
  // so a late answer to a request the walk no longer remembers asking is
  // misread as a first page — the held rows would vanish.
  const canAskMore = after !== null && state.phase !== "loading";

  const askMore = (): void => {
    if (canAskMore) {
      void ask(after);
    }
  };

  return (
    <section
      aria-label="leaderboard"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"
    >
      <h2>{LADDER_HEADING}</h2>
      {state.season !== null && <p>{seasonName(state.season)}</p>}
      {/*
        Rendered from state.self alone — never matched against state.rows.
        Matching would be wrong on every page the reader's row is not on,
        which is nearly all of them (ADR-0065 §8).
      */}
      {state.self !== null && (
        <p className="flex items-center gap-3 rounded-medium border border-accent bg-accent-subtle px-5 py-4">
          <CoinMark />
          <span>{selfLine(state.self)}</span>
        </p>
      )}
      <ul className="w-full">
        {state.rows.map((row) => (
          <li
            key={row.playerId}
            className="flex items-baseline gap-4 border-t border-hairline py-3 text-small first:border-t-0"
          >
            {/*
              The card also sets `min-width: 2ch` on this span. Left off:
              tokens.css names no width family, so expressing it would mean
              either a new token or the bracket literal `min-w-[2ch]` that
              ADR-0091 §4 refuses.
            */}
            <span className="text-right font-mono text-text-muted tabular-nums">
              {row.rank}
            </span>{" "}
            <span className="flex-1">{nameOrNone(row.displayName)}</span>{" "}
            <span className="text-right font-mono tabular-nums">
              {coinBalanceText(row.coins)}
            </span>
          </li>
        ))}
      </ul>
      {/*
        Hidden, not conditionally unmounted: the button must stay the same
        node across the loading transition, so a second press lands on a
        control the guard can refuse — not on one already torn out of the
        tree, which would refuse for the wrong reason and prove nothing.
      */}
      <button
        type="button"
        hidden={!canAskMore}
        onClick={askMore}
        className="rounded-medium bg-accent-fill px-5 py-4 text-on-accent"
      >
        {MORE}
      </button>
      {sentence && <p>{sentence}</p>}
    </section>
  );
}
