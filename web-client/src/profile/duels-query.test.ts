import { describe, it, expect } from "vitest";
import {
  NO_FILTER,
  WHOLE_RECORD,
  isFiltered,
  duelsPath,
  type HistoryFilter,
  type HistoryQuery,
} from "./duels-query";

describe("the duels query", () => {
  it("asks the plain path when nothing narrows the record", () => {
    const path = duelsPath(WHOLE_RECORD);
    expect(path).toBe("/api/me/duels");
    expect(path).not.toContain("?");
  });

  it("names each axis it was given, and only those", () => {
    // outcome alone
    const outcomeOnly: HistoryQuery = {
      outcome: "WON",
      opponent: "",
      after: null,
    };
    expect(duelsPath(outcomeOnly)).toBe("/api/me/duels?outcome=WON");

    // opponent alone
    const opponentOnly: HistoryQuery = {
      outcome: null,
      opponent: "Alice",
      after: null,
    };
    expect(duelsPath(opponentOnly)).toBe("/api/me/duels?opponent=Alice");

    // cursor alone
    const cursorOnly: HistoryQuery = {
      outcome: null,
      opponent: "",
      after: "MjAyNi0wMi0wM",
    };
    expect(duelsPath(cursorOnly)).toBe("/api/me/duels?after=MjAyNi0wMi0wM");

    // all three — verifies the order is outcome, opponent, after
    const allThree: HistoryQuery = {
      outcome: "LOST",
      opponent: "Bob",
      after: "eHl6",
    };
    expect(duelsPath(allThree)).toBe(
      "/api/me/duels?outcome=LOST&opponent=Bob&after=eHl6",
    );
  });

  it("encodes an opponent term so it cannot forge a parameter", () => {
    // A name with & must be encoded so it cannot forge outcome=WON
    const withAmpersand: HistoryQuery = {
      outcome: null,
      opponent: "a&outcome=WON",
      after: null,
    };
    expect(duelsPath(withAmpersand)).toBe(
      "/api/me/duels?opponent=a%26outcome%3DWON",
    );

    // A name with % must be encoded
    const withPercent: HistoryQuery = {
      outcome: null,
      opponent: "100%Sure",
      after: null,
    };
    expect(duelsPath(withPercent)).toBe("/api/me/duels?opponent=100%25Sure");

    // A name with a space must be encoded
    const withSpace: HistoryQuery = {
      outcome: null,
      opponent: "Ada Lovelace",
      after: null,
    };
    expect(duelsPath(withSpace)).toBe("/api/me/duels?opponent=Ada%20Lovelace");
  });

  it("hands the cursor back byte for byte", () => {
    // An unpadded base64url cursor with both - and _ must survive unchanged
    const cursor = "MjAyNi0wMi0wM-BkLTEx_Qw";
    const query: HistoryQuery = {
      outcome: null,
      opponent: "",
      after: cursor,
    };
    const path = duelsPath(query);
    expect(path).toContain(cursor);
    expect(path).toBe(`/api/me/duels?after=${cursor}`);
  });

  it("sends no opponent parameter for an empty box, and sends a space unmodified", () => {
    // Empty string should not emit opponent parameter
    const empty: HistoryQuery = {
      outcome: null,
      opponent: "",
      after: null,
    };
    expect(duelsPath(empty)).toBe("/api/me/duels");
    expect(duelsPath(empty)).not.toContain("opponent");

    // A single space should be encoded and sent
    const space: HistoryQuery = {
      outcome: null,
      opponent: " ",
      after: null,
    };
    expect(duelsPath(space)).toBe("/api/me/duels?opponent=%20");
    expect(duelsPath(space)).toContain("opponent");
  });

  it("asks for no page size of its own", () => {
    // Empty query should not have limit
    expect(duelsPath(WHOLE_RECORD)).not.toContain("limit");

    // Fully populated query should not have limit
    const full: HistoryQuery = {
      outcome: "DREW",
      opponent: "Charlie",
      after: "c3RhcnRlcg",
    };
    expect(duelsPath(full)).not.toContain("limit");
  });

  it("says whether a filter narrows anything at all", () => {
    // NO_FILTER should be false
    expect(isFiltered(NO_FILTER)).toBe(false);

    // outcome alone should be true
    const outcomeFilter: HistoryFilter = { outcome: "WON", opponent: "" };
    expect(isFiltered(outcomeFilter)).toBe(true);

    // opponent alone should be true
    const opponentFilter: HistoryFilter = { outcome: null, opponent: "David" };
    expect(isFiltered(opponentFilter)).toBe(true);
  });
});
