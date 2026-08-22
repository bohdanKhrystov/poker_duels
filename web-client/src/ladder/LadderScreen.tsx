import { useCallback, useEffect, useReducer, type ReactElement } from "react";
import type { LadderRead } from "./ladder-read";
import { ladderReducer, initialLadder } from "./ladder-state";
import {
  LADDER_HEADING,
  LOADING_LADDER,
  EMPTY_LADDER,
  LADDER_FAILED,
  rowLine,
  seasonName,
} from "./ladder-text";

/**
 * The ladder screen: it asks for the first page of the season standings
 * through the read it is handed, and prints exactly the rows that page
 * carried, in the order it carried them.
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

  return (
    <section
      aria-label="leaderboard"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"
    >
      <h2>{LADDER_HEADING}</h2>
      {state.season !== null && <p>{seasonName(state.season)}</p>}
      <ul className="w-full">
        {state.rows.map((row) => (
          <li
            key={row.playerId}
            className="border-t border-hairline py-3 first:border-t-0"
          >
            <p className="text-small">{rowLine(row)}</p>
          </li>
        ))}
      </ul>
      {sentence && <p>{sentence}</p>}
    </section>
  );
}
