import {
  NO_FILTER,
  type HistoryFilter,
  type HistoryQuery,
} from "../profile/duels-query";
import type { RecentDuel } from "../profile/recent-duels";

/**
 * Where the history walk stands: fetching a page, showing what it has read,
 * or unable to fetch the page it last asked for.
 */
export type HistoryPhase = "loading" | "ready" | "failed";

/**
 * The duel history screen's state: the filter in effect, the rows read so
 * far, the cursor naming the next page, and — while a request is in
 * flight — the cursor that request carried.
 *
 * `askedWith` is the whole mechanism by which `page` tells a first page
 * from a next page apart: the reducer cannot ask the component what kind of
 * request came back, and a `page` event carries no memory of what was
 * asked, so the state has to hold it.
 */
export interface HistoryState {
  readonly filter: HistoryFilter;
  readonly rows: readonly RecentDuel[];
  readonly nextCursor: string | null;
  readonly askedWith: string | null;
  readonly phase: HistoryPhase;
}

/**
 * An event the history walk reduces: a request was sent, a page answered,
 * or a request failed.
 */
export type HistoryEvent =
  | { readonly type: "asked"; readonly after: string | null }
  | {
      readonly type: "page";
      readonly duels: readonly RecentDuel[];
      readonly nextCursor: string | null;
      readonly restarted: boolean;
    }
  | { readonly type: "failed" }
  | { readonly type: "filtered"; readonly filter: HistoryFilter };

/**
 * The state a fresh history screen starts in: loading, with nothing read
 * and nowhere to go until the first page answers.
 */
export function initialHistory(
  filter: HistoryFilter = NO_FILTER,
): HistoryState {
  return {
    filter,
    rows: [],
    nextCursor: null,
    askedWith: null,
    phase: "loading",
  };
}

/**
 * Reduces one event into the history walk's next state.
 *
 * `page` appends the rows under the ones already held when the request in
 * flight carried a cursor (`askedWith !== null`) and the server did not
 * restart the walk; it replaces them otherwise — either because this was a
 * first page (no cursor was outstanding) or because the server refused the
 * outstanding cursor and restarted the walk from the newest page. Nothing
 * here sorts, reverses, de-duplicates or re-keys: rows are spread in the
 * order the server sent them, because the server's order is the record's
 * order.
 *
 * `failed` never touches `rows` or `nextCursor` — a request that fails does
 * not un-read the pages already read.
 *
 * `filtered` keeps nothing: it answers `initialHistory(event.filter)`, the
 * state a fresh screen starts in under the new filter, because a cursor —
 * and the rows a cursor was walking through — is bound to the filter it was
 * drawn under (`ADR-0057` §5) and none of it is safe to carry into another.
 */
export function historyReducer(
  state: HistoryState,
  event: HistoryEvent,
): HistoryState {
  switch (event.type) {
    case "asked":
      return { ...state, askedWith: event.after, phase: "loading" };

    case "page": {
      const rows =
        state.askedWith !== null && !event.restarted
          ? [...state.rows, ...event.duels]
          : event.duels;
      return {
        ...state,
        rows,
        nextCursor: event.nextCursor,
        askedWith: null,
        phase: "ready",
      };
    }

    case "failed":
      return { ...state, askedWith: null, phase: "failed" };

    case "filtered":
      return initialHistory(event.filter);
  }
}

/**
 * The query for a filter's first page: no cursor.
 */
export function firstPageQuery(filter: HistoryFilter): HistoryQuery {
  return { ...filter, after: null };
}

/**
 * The query for the page after the one held, or `null` when the server
 * named no further page — the walk has nowhere left to go.
 *
 * The cursor is passed through untouched: never parsed, never incremented,
 * and never derived from a row count (`ADR-0057` §4).
 */
export function nextPageQuery(state: HistoryState): HistoryQuery | null {
  if (state.nextCursor === null) {
    return null;
  }
  return { ...state.filter, after: state.nextCursor };
}
