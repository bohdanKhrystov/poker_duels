import type { LadderPage, LadderRow, SelfStanding } from "./ladder-page";

/**
 * Where the ladder walk stands: fetching a page, showing what it has read,
 * or unable to fetch the page it last asked for.
 */
export type LadderPhase = "loading" | "ready" | "failed";

/**
 * The ladder screen's state: the rows read so far, the cursor naming the
 * next page, and — while a request is in flight — the cursor that request
 * carried.
 *
 * `askedWith` is the whole mechanism by which `page` tells a first page
 * from a next page apart: the reducer cannot ask the component what kind of
 * request came back, and a `page` event carries no memory of what was
 * asked, so the state has to hold it.
 *
 * `season` and `self` are the first page's, and only the first page's: the
 * walk shows one season and one reader standing for as long as it lasts, so
 * neither may move as later pages answer (`ADR-0065` §1, §3).
 */
export interface LadderState {
  readonly rows: readonly LadderRow[];
  readonly nextCursor: string | null;
  readonly askedWith: string | null;
  readonly phase: LadderPhase;
  readonly season: string | null;
  readonly self: SelfStanding | null;
}

/**
 * An event the ladder walk reduces: a request was sent, a page answered, or
 * a request failed.
 */
export type LadderEvent =
  | { readonly type: "asked"; readonly after: string | null }
  | { readonly type: "page"; readonly page: LadderPage }
  | { readonly type: "failed" };

/**
 * The state a fresh ladder screen starts in: loading, with nothing read and
 * nowhere to go until the first page answers.
 */
export function initialLadder(): LadderState {
  return {
    rows: [],
    nextCursor: null,
    askedWith: null,
    phase: "loading",
    season: null,
    self: null,
  };
}

/**
 * Reduces one event into the ladder walk's next state.
 *
 * `page` appends `event.page.rows` under the rows already held when the
 * request in flight carried a cursor (`askedWith !== null`); it replaces
 * them otherwise, because no cursor outstanding means this was a first
 * page. Nothing here sorts, reverses, de-duplicates, re-keys or renumbers:
 * rows are spread in the order the server sent them, and a repeated rank
 * across a page boundary is ordinary, not a duplicate to filter
 * (`ADR-0064` §2).
 *
 * The same no-cursor-outstanding test also settles `season` and `self`: a
 * page answering a cursorless request defines both for the whole walk, and
 * a page answering a later request leaves them exactly as they were —
 * whatever that later page itself carried, including a `self` of `null` —
 * so neither can change mid-walk for a reason the reader never asked for
 * (`ADR-0065` §1, §3).
 *
 * `failed` never touches `rows` or `nextCursor` — a request that fails does
 * not un-read the pages already read.
 */
export function ladderReducer(
  state: LadderState,
  event: LadderEvent,
): LadderState {
  switch (event.type) {
    case "asked":
      return { ...state, askedWith: event.after, phase: "loading" };

    case "page": {
      const isFirstPage = state.askedWith === null;
      const rows = isFirstPage
        ? event.page.rows
        : [...state.rows, ...event.page.rows];
      return {
        ...state,
        rows,
        season: isFirstPage ? event.page.season : state.season,
        self: isFirstPage ? event.page.self : state.self,
        nextCursor: event.page.nextCursor,
        askedWith: null,
        phase: "ready",
      };
    }

    case "failed":
      return { ...state, askedWith: null, phase: "failed" };
  }
}

/**
 * The cursor for the page after the one held, or `null` when the server
 * named no further page — the walk has nowhere left to go.
 *
 * The cursor is passed through untouched: never parsed, never incremented,
 * and never derived from a row count.
 */
export function nextPageAfter(state: LadderState): string | null {
  return state.nextCursor;
}
