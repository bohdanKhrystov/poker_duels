import {
  useCallback,
  useEffect,
  useReducer,
  useState,
  type FormEvent,
  type ReactElement,
} from "react";
import type { DuelPageRead } from "../profile/duel-page";
import type { DuelOutcomeWord } from "../profile/recent-duels";
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
  MORE,
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
      <fieldset className="p-0 flex w-full flex-wrap items-center gap-4 border-0">
        <legend>{OUTCOME_LEGEND}</legend>
        <label className={outcomeLabelClass(state.filter.outcome === null)}>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === null}
            onChange={() => handleOutcomeChange(null)}
          />
          {EVERY_OUTCOME}
        </label>
        <label className={outcomeLabelClass(state.filter.outcome === "WON")}>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "WON"}
            onChange={() => handleOutcomeChange("WON")}
          />
          {outcomeWord("WON")}
        </label>
        <label className={outcomeLabelClass(state.filter.outcome === "LOST")}>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "LOST"}
            onChange={() => handleOutcomeChange("LOST")}
          />
          {outcomeWord("LOST")}
        </label>
        <label className={outcomeLabelClass(state.filter.outcome === "DREW")}>
          <input
            type="radio"
            name="outcome"
            checked={state.filter.outcome === "DREW"}
            onChange={() => handleOutcomeChange("DREW")}
          />
          {outcomeWord("DREW")}
        </label>
      </fieldset>
      <form
        onSubmit={handleSearch}
        className="flex w-full flex-wrap items-end gap-3"
      >
        <label className="flex flex-col gap-2 text-small text-text-muted">
          {OPPONENT_LABEL}
          <input
            type="text"
            value={opponentTerm}
            onChange={(event) => setOpponentTerm(event.target.value)}
            className="rounded-small border border-hairline bg-surface px-4 py-3 text-text"
          />
        </label>
        <button
          type="submit"
          className="rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        >
          {SEARCH}
        </button>
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
                <span className={outcomeWordClass(duel.outcome)}>
                  {outcomeWord(duel.outcome)}
                </span>{" "}
                {coinDeltaText(duel.coinDelta)} {duel.handsPlayed}{" "}
                {duel.handsPlayed === 1 ? "hand" : "hands"} vs{" "}
                <span className="text-text">
                  {nameOrNone(duel.opponentDisplayName)}
                </span>{" "}
                <span className="text-text-faint">
                  {finishedAtText(duel.finishedAt)}
                </span>
              </p>
            </li>
          ))}
        </ul>
      )}
      {sentence && <p>{sentence}</p>}
      {nextQuery !== null && state.phase !== "loading" && (
        <button type="button" onClick={() => ask(nextQuery)}>
          {MORE}
        </button>
      )}
    </section>
  );
}

/**
 * The row's colour cue for its outcome word: win and loss carry the card's
 * colours (`design/screens/duels.html`'s `.outcome-word.won`/`.lost`); a draw
 * keeps the row's own text colour, exactly as the card assigns no colour of
 * its own to a tie.
 */
function outcomeColour(outcome: DuelOutcomeWord): string {
  switch (outcome) {
    case "WON":
      return "text-win";
    case "LOST":
      return "text-loss";
    case "DREW":
      return "";
  }
}

/**
 * The outcome word's full class list: `outcomeColour` supplies the win/loss
 * tint (or none, for a draw); the card's `.row .outcome-word { font-weight:
 * 500 }` applies to all three alike, so it is added here rather than folded
 * into the colour rule above.
 */
function outcomeWordClass(outcome: DuelOutcomeWord): string {
  return [outcomeColour(outcome), "font-medium"].filter(Boolean).join(" ");
}

/**
 * The outcome-filter label's text treatment: the checked one carries the
 * card's distinction (`.radio.on` in `design/screens/duels.html`) and the
 * other three keep the row's muted default. Driven off the same
 * `state.filter.outcome` comparison each `checked` prop already uses.
 */
function outcomeLabelClass(checked: boolean): string {
  return checked
    ? "flex items-center gap-2 text-small text-text font-medium"
    : "flex items-center gap-2 text-small text-text-muted";
}
