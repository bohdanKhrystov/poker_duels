import { useCallback, useEffect, useReducer, type ReactElement } from "react";
import type { DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery, HistoryFilter } from "../profile/duels-query";
import { NO_FILTER, isFiltered } from "../profile/duels-query";
import {
  historyReducer,
  initialHistory,
  firstPageQuery,
} from "./history-state";
import {
  HISTORY_HEADING,
  emptyLine,
  LOADING_RECORD,
  READ_FAILED,
} from "./history-text";
import {
  coinDeltaText,
  finishedAtText,
  outcomeWord,
} from "../profile/profile-text";
import { nameOrNone } from "../profile/name-text";

/**
 * The history screen: it asks for the first page of the whole record through the read it is handed,
 * and renders what came back in the order it came back.
 *
 * Holds `useReducer(historyReducer, NO_FILTER, initialHistory)` and calls `read`;
 * it constructs no `fetch` and touches no `Storage`, so nothing here can reach Node's inert
 * `localStorage` global under Vitest.
 *
 * `read` is injected, exactly as `ProfileProvider`'s is, and must be a stable reference — it is the
 * effect's only dependency.
 *
 * `no-profile` is dispatched as an empty page, not as a failure: a browser holding no profile has
 * played no duels, so *"No duels yet."* is true of it and *"Your duels did not load."* is not.
 */
export function HistoryScreen(props: {
  readonly read: (query: HistoryQuery) => Promise<DuelPageRead>;
  readonly filter?: HistoryFilter;
}): ReactElement {
  const { read, filter = NO_FILTER } = props;
  const [state, dispatch] = useReducer(historyReducer, filter, initialHistory);

  const ask = useCallback(
    async (query: HistoryQuery): Promise<void> => {
      dispatch({ type: "asked", after: query.after });
      const result = await read(query);
      if (result.kind === "page") {
        dispatch({
          type: "page",
          duels: result.duels,
          nextCursor: result.nextCursor,
          restarted: result.restarted,
        });
      } else if (result.kind === "no-profile") {
        dispatch({
          type: "page",
          duels: [],
          nextCursor: null,
          restarted: false,
        });
      } else {
        dispatch({ type: "failed" });
      }
    },
    [read],
  );

  useEffect(() => {
    ask(firstPageQuery(filter));
  }, [ask, filter]);

  const hasRows = state.rows.length > 0;
  const isEmpty = !hasRows;

  // Determine what sentence to show based on phase and rows
  let sentence: string | null = null;

  if (state.phase === "loading" && isEmpty) {
    // loading + none → LOADING_RECORD, no list
    sentence = LOADING_RECORD;
  } else if (state.phase === "loading" && hasRows) {
    // loading + some → rows, then LOADING_RECORD
    sentence = LOADING_RECORD;
  } else if (state.phase === "failed") {
    // failed + any → rows (if any), then READ_FAILED
    sentence = READ_FAILED;
  } else if (state.phase === "ready" && isEmpty) {
    // ready + none → call emptyLine with isFiltered(state.filter), no list
    sentence = emptyLine(isFiltered(state.filter));
  }
  // ready + some → no sentence, just rows

  return (
    <section
      aria-label="your duels"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"
    >
      <h2>{HISTORY_HEADING}</h2>
      {(state.phase === "loading" && isEmpty) ||
      (state.phase === "ready" && isEmpty) ? null : (
        <ul className="w-full">
          {state.rows.map((duel) => (
            <li
              key={duel.duelId}
              className="border-t border-hairline py-3 first:border-t-0"
            >
              <p className="text-small">
                {outcomeWord(duel.outcome)} {coinDeltaText(duel.coinDelta)}{" "}
                {duel.handsPlayed} {duel.handsPlayed === 1 ? "hand" : "hands"}{" "}
                vs {nameOrNone(duel.opponentDisplayName)}{" "}
                {finishedAtText(duel.finishedAt)}
              </p>
            </li>
          ))}
        </ul>
      )}
      {sentence && <p>{sentence}</p>}
    </section>
  );
}
