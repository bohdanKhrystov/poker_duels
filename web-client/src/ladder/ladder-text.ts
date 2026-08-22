import type { LadderRow } from "./ladder-page";
import { nameOrNone } from "../profile/name-text";
import { coinBalanceText } from "../profile/profile-text";

export const LADDER_HEADING = "Leaderboard";

export const MORE = "Show more";

/**
 * Converts a season string from the response into a user-facing month and year.
 *
 * The `season` parameter is a wire format string like `"2026-08"` that comes
 * from the server and must never be shown directly to the player. This function
 * derives the month name and year without consulting the browser's clock —
 * the server is authoritative about what season the response carries, and the
 * client must not work it out from the browser's local time. A timezone at
 * the international date line can see a different month than UTC, and showing
 * that divergence to the player would be wrong.
 *
 * The month array here contains the English names, not a string formatter that
 * knows about time zones. If the month in `season` is not `01`–`12`, or if the
 * `season` is not `YYYY-MM` at all, the input is handed back unchanged rather
 * than rendering an undefined month or throwing an error.
 */
export function seasonName(season: string): string {
  const months = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];

  // Match YYYY-MM format
  const match = season.match(/^(\d{4})-(\d{2})$/);
  if (!match) {
    return season;
  }

  const year = match[1];
  const month = parseInt(match[2], 10);

  // Check if month is valid (1-12)
  if (month < 1 || month > 12) {
    return season;
  }

  return `${months[month - 1]} ${year}`;
}

/**
 * Formats a leaderboard row as a string combining rank, player name, and standing.
 *
 * Builds one line of text by combining three fields separated by spaces:
 * the rank number, the player's display name (or `nameOrNone`'s fallback when it is missing),
 * and their coin standing. The display name and coin formatting are delegated
 * to their respective single-purpose functions to ensure every surface
 * that renders these fields uses the same logic.
 */
export function rowLine(row: LadderRow): string {
  return `${row.rank} ${nameOrNone(row.displayName)} ${coinBalanceText(row.coins)}`;
}
