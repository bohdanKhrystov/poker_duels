import type { DuelOutcomeWord } from "./recent-duels";

/**
 * Describes which axes of the duel history are narrowed.
 *
 * The client's history screen has two filter controls: an outcome drop-down
 * (picking all, won, lost, or drew) and an opponent name text box.
 * Each axis is optional. A filter narrows the history when at least one axis
 * is set.
 */
export interface HistoryFilter {
  readonly outcome: DuelOutcomeWord | null;
  readonly opponent: string;
}

/**
 * A duel history query with no axes set — the unfiltered starting point.
 */
export const NO_FILTER: HistoryFilter = { outcome: null, opponent: "" };

/**
 * Whether the filter narrows the history at all.
 *
 * True when either axis is set (outcome selected or opponent text entered).
 * Used to choose which of two empty states the screen displays when no duels
 * match: "No duels yet" (not filtered) or "No duels match" (filtered).
 */
export function isFiltered(filter: HistoryFilter): boolean {
  return filter.outcome !== null || filter.opponent !== "";
}

/**
 * A duel history query including pagination state.
 *
 * The query combines the two filter axes (outcome and opponent name) with
 * an opaque cursor for pagination. The cursor names a position in the
 * finishedAt/duelId order within the filtered set.
 */
export interface HistoryQuery extends HistoryFilter {
  readonly after: string | null;
}

/**
 * The unfiltered, unpaginated starting query.
 */
export const WHOLE_RECORD: HistoryQuery = { ...NO_FILTER, after: null };

/**
 * The GET /api/me/duels path for a query.
 *
 * Emits an exact path: no query string at all when no parameters are present,
 * and a `?` followed by parameters in the order outcome, opponent, after.
 * Each parameter's value is percent-encoded to prevent a display name or
 * cursor from forging additional parameters.
 *
 * Uses `encodeURIComponent` to encode every value — this prevents a name
 * containing `&outcome=WON` from forging a second parameter that the server
 * would honor. The function also leaves `-` and `_` alone, which makes an
 * unpadded base64url cursor survive round-tripping unchanged.
 *
 * A blank opponent box emits no parameter — the opponent control must not send
 * an empty string to the server. Every other value is sent as the player typed
 * it, untrimmed. The server refuses a blank term with 400, so a mishandled
 * empty box would fail on first render; and trimming would break the rule that
 * the search term is sent exactly as the player entered it.
 */
export function duelsPath(query: HistoryQuery): string {
  const params: Array<[string, string]> = [];

  if (query.outcome !== null) {
    params.push(["outcome", encodeURIComponent(query.outcome)]);
  }

  if (query.opponent !== "") {
    params.push(["opponent", encodeURIComponent(query.opponent)]);
  }

  if (query.after !== null) {
    params.push(["after", encodeURIComponent(query.after)]);
  }

  if (params.length === 0) {
    return "/api/me/duels";
  }

  return (
    "/api/me/duels?" + params.map(([key, value]) => `${key}=${value}`).join("&")
  );
}
