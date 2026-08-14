/**
 * Trim and upper-case, and nothing else: `ADR-0022` has the server answer an
 * unparseable code and an unknown room identically on purpose, so checking the
 * alphabet here would hand back the shape oracle it deliberately withholds.
 */
export function normalizeRoomCode(raw: string): string {
  return raw.trim().toUpperCase();
}

/** The room code this tab's URL carried, or `null` when it carried none. */
export function roomCodeFromSearch(search: string): string | null {
  const code = normalizeRoomCode(new URLSearchParams(search).get("room") ?? "");
  return code === "" ? null : code;
}

/**
 * The invite. The code is a query parameter because a path segment would 404 on
 * reload against a static host with no rewrite rule, and `EPIC-07` has not
 * chosen one.
 */
export function roomLink(origin: string, code: string): string {
  return `${origin}/?room=${code}`;
}
