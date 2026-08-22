export interface LadderRow {
  readonly rank: number;
  readonly playerId: string;
  readonly displayName: string | null;
  readonly coins: number;
}

export interface LadderPage {
  readonly season: string;
  readonly rows: readonly LadderRow[];
  readonly nextCursor: string | null;
}

/**
 * Parses a standings page response body into typed rows and metadata.
 *
 * Returns the parsed page (rows in order, season, and cursor) if the body is
 * valid, or null if the body is invalid in any way — missing fields, wrong
 * types, invalid season format, missing cursor, or rows missing required fields.
 * A partially parsed page is never returned: all-or-nothing validation ensures
 * the caller either gets a complete page or nothing.
 *
 * Rows are kept in the order the server listed them. The `rank` field is copied
 * from the row and never derived from array position — ADR-0064 §2 forbids
 * renumbering on the client.
 */
export function parseLadderPage(body: unknown): LadderPage | null {
  if (typeof body !== "object" || body === null) {
    return null;
  }

  const bodyRecord = body as Record<string, unknown>;

  // Validate season exists and matches YYYY-MM format
  const season = bodyRecord.season;
  if (typeof season !== "string" || !/^\d{4}-\d{2}$/.test(season)) {
    return null;
  }

  // Validate that nextCursor exists and is either a string or null
  if (!("nextCursor" in bodyRecord)) {
    return null;
  }

  const nextCursor = bodyRecord.nextCursor;
  if (typeof nextCursor !== "string" && nextCursor !== null) {
    return null;
  }

  // Validate that rows is an array
  if (!Array.isArray(bodyRecord.rows)) {
    return null;
  }

  const rowsList = bodyRecord.rows as unknown[];
  const rows: LadderRow[] = [];

  for (const row of rowsList) {
    if (typeof row !== "object" || row === null) {
      return null;
    }

    const rowRecord = row as Record<string, unknown>;
    const rank = rowRecord.rank;
    const playerId = rowRecord.playerId;
    const displayName = rowRecord.displayName;
    const coins = rowRecord.coins;

    // Validate all required fields exist and have correct types
    if (
      typeof rank !== "number" ||
      typeof playerId !== "string" ||
      (typeof displayName !== "string" && displayName !== null) ||
      typeof coins !== "number"
    ) {
      return null;
    }

    const ladderRow: LadderRow = {
      rank,
      playerId,
      displayName,
      coins,
    };
    rows.push(ladderRow);
  }

  return { season, rows, nextCursor };
}
