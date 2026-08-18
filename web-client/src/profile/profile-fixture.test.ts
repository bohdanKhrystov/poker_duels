import { describe, it, expect } from "vitest";
import { aProfile, aDuelLine, meBody, duelRowBody } from "./profile-fixture";

describe("the profile fixtures", () => {
  it("builds a profile carrying every field PlayerProfile declares", () => {
    expect(Object.keys(aProfile()).sort()).toEqual([
      "coinBalance",
      "displayName",
      "displayNameRemoved",
      "playerId",
    ]);
  });

  it("builds a duel line carrying every field RecentDuel declares", () => {
    expect(Object.keys(aDuelLine()).sort()).toEqual([
      "coinDelta",
      "duelId",
      "finishedAt",
      "handsPlayed",
      "opponentDisplayName",
      "outcome",
    ]);
  });

  it("builds bodies carrying every field the wire declares, opponent id included", () => {
    const me = meBody();
    expect(Object.keys(me).sort()).toEqual([
      "coinBalance",
      "displayName",
      "displayNameRemoved",
      "playerId",
    ]);

    const duelRow = duelRowBody();
    expect(Object.keys(duelRow).sort()).toEqual([
      "coinDelta",
      "duelId",
      "finishedAt",
      "handsPlayed",
      "opponentDisplayName",
      "opponentPlayerId",
      "outcome",
    ]);
    expect(duelRow).toHaveProperty("opponentPlayerId");
  });

  it("lets a test bend any field it names", () => {
    // aProfile with two different values
    expect(aProfile({ coinBalance: 5 }).coinBalance).toBe(5);
    expect(aProfile({ coinBalance: -2 }).coinBalance).toBe(-2);
    expect(aProfile({ playerId: "p-custom" }).playerId).toBe("p-custom");
    expect(aProfile({ playerId: "p-other" }).playerId).toBe("p-other");

    // aDuelLine with two different values
    expect(aDuelLine({ handsPlayed: 10 }).handsPlayed).toBe(10);
    expect(aDuelLine({ handsPlayed: 99 }).handsPlayed).toBe(99);
    expect(aDuelLine({ outcome: "LOST" }).outcome).toBe("LOST");
    expect(aDuelLine({ outcome: "DREW" }).outcome).toBe("DREW");

    // meBody with two different values
    expect(meBody({ coinBalance: 100 }).coinBalance).toBe(100);
    expect(meBody({ coinBalance: 0 }).coinBalance).toBe(0);
    expect(meBody({ playerId: "p-test" }).playerId).toBe("p-test");
    expect(meBody({ playerId: "p-another" }).playerId).toBe("p-another");

    // duelRowBody with two different values
    expect(duelRowBody({ coinDelta: 5 }).coinDelta).toBe(5);
    expect(duelRowBody({ coinDelta: -3 }).coinDelta).toBe(-3);
    expect(duelRowBody({ opponentPlayerId: "opp-1" }).opponentPlayerId).toBe(
      "opp-1",
    );
    expect(duelRowBody({ opponentPlayerId: "opp-2" }).opponentPlayerId).toBe(
      "opp-2",
    );
    expect(
      duelRowBody({ opponentDisplayName: "Alice" }).opponentDisplayName,
    ).toBe("Alice");
    expect(
      duelRowBody({ opponentDisplayName: "Bob" }).opponentDisplayName,
    ).toBe("Bob");

    // Verify that non-overridden fields keep their defaults
    expect(aProfile({ playerId: "p-custom" }).coinBalance).toBe(41);
    expect(aDuelLine({ outcome: "LOST" }).handsPlayed).toBe(23);
    expect(aDuelLine({ outcome: "LOST" }).opponentDisplayName).toBe(null);
    expect(meBody({ playerId: "p-test" }).coinBalance).toBe(41);
    expect(duelRowBody({ opponentPlayerId: "opp-1" }).handsPlayed).toBe(23);
    expect(duelRowBody({ opponentPlayerId: "opp-1" }).opponentDisplayName).toBe(
      null,
    );
  });
});
