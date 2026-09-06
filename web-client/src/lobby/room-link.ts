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

/**
 * The code the front door's field yields for what was typed or pasted into it:
 * the `room` a pasted invite link carries, and otherwise the text normalised
 * exactly as before. `""` for an empty field, which the caller refuses.
 *
 * One parse, not two: the code a paste yields and the code the same link yields
 * when a tab navigates to it both come from `roomCodeFromSearch`. Text that is
 * neither is passed on untouched but for case — `ADR-0022` leaves the server the
 * only judge of whether a code names a room.
 */
export function roomCodeFromField(text: string): string {
  try {
    const carried = roomCodeFromSearch(new URL(text.trim()).search);
    if (carried !== null) return carried;
  } catch {
    // Not a URL: the ordinary paste, a bare code.
  }
  return normalizeRoomCode(text);
}
