import {
  useCallback,
  useEffect,
  useReducer,
  useState,
  type FormEvent,
  type ReactElement,
} from "react";
import type { DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery, HistoryFilter } from "../profile/duels-query";
import { NO_FILTER, isFiltered } from "../profile/duels-query";
import {
  historyReducer,
  initialHistory,
  firstPageQuery,
  nextPageQuery,
} from "./history-state";
import {
  HISTORY_HEADING,
  emptyLine,
  LOADING_RECORD,
  READ_FAILED,
  OUTCOME_LEGEND,
  EVERY_OUTCOME,
  OPPONENT_LABEL,
  SEARCH,
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
 *
 * The `MORE` control and the request it sends read one call to `nextPageQuery` — rendering it from
 * one rule and asking from another is how the two end up disagreeing. It is withheld while `phase`
 * is `loading`: `nextPageQuery` is pure over `nextCursor`, which a request in flight does not move,
 * so without this guard a second click would re-issue the identical query already outstanding.
 *
 * The opponent box lives inside a `<form>`. `ADR-0059` fires a search on exactly two acts — Enter
 * and the `SEARCH` button, both submits of that form — and on nothing else: typing, pausing,
 * focusing and blurring send nothing, and no timer is introduced here. The submitted term is sent
 * exactly as typed; `duelsPath` (`../profile/duels-query`) is what percent-encodes it and what
 * turns an empty term into no `opponent` parameter at all, so this component neither trims, folds
 * nor special-cases the empty box — committing it is a search like any other.
 */
export function HistoryScreen(props: {
  readonly read: (query: HistoryQuery) => Promise<DuelPageRead>;
  readonly filter?: HistoryFilter;
}): ReactElement {
  const { read, filter = NO_FILTER } = props;
  const [state, dispatch] = useReducer(historyReducer, filter, initialHistory);
  const [opponentTerm, setOpponentTerm] = useState(filter.opponent);

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

  const handleOutcomeChange = useCallback(
    (newOutcome: string | null) => {
      const newFilter: HistoryFilter = {
        outcome: newOutcome as "WON" | "LOST" | "DREW" | null,
        opponent: state.filter.opponent,
      };
      dispatch({ type: "filtered", filter: newFilter });
      ask(firstPageQuery(newFilter));
    },
    [state.filter.opponent, ask],
  );

  const handleSearch = useCallback(
    (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      const newFilter: HistoryFilter = {
        outcome: state.filter.outcome,
        opponent: opponentTerm,
      };
      dispatch({ type: "filtered", filter: newFilter });
      ask(firstPageQuery(newFilter));
    },
    [state.filter.outcome, opponentTerm, ask],
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

  const nextQuery = nextPageQuery(state);

  return (
    <section
      aria-label="your duels"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"
    >
      <h2>{HISTORY_HEADING}</h2>
      <fieldset>
        <legend>{OUTCOME_LEGEND}</legend>
        <label>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === null}
            onChange={() => handleOutcomeChange(null)}
          />
          {EVERY_OUTCOME}
        </label>
        <label>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "WON"}
            onChange={() => handleOutcomeChange("WON")}
          />
          {outcomeWord("WON")}
        </label>
        <label>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "LOST"}
            onChange={() => handleOutcomeChange("LOST")}
          />
          {outcomeWord("LOST")}
        </label>
        <label>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "DREW"}
            onChange={() => handleOutcomeChange("DREW")}
          />
          {outcomeWord("DREW")}
        </label>
      </fieldset>
      <form onSubmit={handleSearch}>
        <label>
          {OPPONENT_LABEL}
          <input
            type="text"
            value={opponentTerm}
            onChange={(event) => setOpponentTerm(event.target.value)}
          />
        </label>
        <button type="submit">{SEARCH}</button>
      </form>
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
      {nextQuery !== null && state.phase !== "loading" && (
        <button type="button" onClick={() => ask(nextQuery)}>
          MORE
        </button>
      )}
    </section>
  );
}
