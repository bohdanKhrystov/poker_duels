import { describe, expect, it } from "vitest";

import { duelRowBody } from "../profile/profile-fixture";
import { accountServer, type ServerPlayer } from "./account-server";

const players: readonly ServerPlayer[] = [
  {
    playerId: "player-seat-0",
    deviceId: "device-seat-0",
    coinBalance: 100,
    displayName: "Alice",
    duels: [
      duelRowBody({ duelId: "duel-seat-0-1", outcome: "WON" }),
      duelRowBody({ duelId: "duel-seat-0-2", outcome: "LOST" }),
    ],
  },
  {
    playerId: "player-seat-1",
    deviceId: "device-seat-1",
    coinBalance: 37,
    displayName: "Bob",
    duels: [duelRowBody({ duelId: "duel-seat-1-1", outcome: "DREW" })],
  },
];

describe("accountServer", () => {
  it("answers each device id with its own player", async () => {
    const server = accountServer(players);

    // Request for player 0
    const response0 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    expect(response0.status).toBe(200);
    const body0 = await response0.json();
    expect(body0).toEqual({
      playerId: "player-seat-0",
      coinBalance: 100,
      displayName: "Alice",
      displayNameRemoved: false,
      deviceRouteLive: true,
    });

    // Request for player 1
    const response1 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    expect(response1.status).toBe(200);
    const body1 = await response1.json();
    expect(body1).toEqual({
      playerId: "player-seat-1",
      coinBalance: 37,
      displayName: "Bob",
      displayNameRemoved: false,
      deviceRouteLive: true,
    });

    // Verify they are different
    const body0Record = body0 as Record<string, unknown>;
    const body1Record = body1 as Record<string, unknown>;
    expect(body0Record.playerId).not.toBe(body1Record.playerId);
    expect(body0Record.coinBalance).not.toBe(body1Record.coinBalance);
    expect(body0Record.displayName).not.toBe(body1Record.displayName);
  });

  it("refuses a device id it has never issued", async () => {
    const server = accountServer(players);

    const response = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-nobody" },
    });
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body).toEqual({});
  });

  it("refuses a request carrying no device id at all", async () => {
    const server = accountServer(players);

    const response = await server.fetch("/api/me", {
      headers: {},
    });
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body).toEqual({});
  });

  it("writes down the path headers and body of every request", async () => {
    const server = accountServer(players);

    // First request: GET /api/me with device-seat-0
    await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });

    // Second request: unknown path
    await server.fetch("/api/unknown", {
      headers: { "X-Device-Id": "device-seat-1" },
    });

    expect(server.requests).toHaveLength(2);

    // Check first request
    const firstRequest = server.requests[0];
    expect(firstRequest.path).toBe("/api/me");
    expect(firstRequest.method).toBe("GET");
    expect(firstRequest.headers["X-Device-Id"]).toBe("device-seat-0");
    expect(firstRequest.body).toBeNull();

    // Check second request
    const secondRequest = server.requests[1];
    expect(secondRequest.path).toBe("/api/unknown");
    expect(secondRequest.method).toBe("GET");
    expect(secondRequest.headers["X-Device-Id"]).toBe("device-seat-1");
    expect(secondRequest.body).toBeNull();
  });

  it("answers an unknown path with 500", async () => {
    const server = accountServer(players);

    const response = await server.fetch("/api/unknown", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    expect(response.status).toBe(500);
  });

  it("answers each device id with its own duels", async () => {
    const server = accountServer(players);

    // Request for player 0's duels
    const response0 = await server.fetch("/api/me/duels", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    expect(response0.status).toBe(200);
    const body0 = (await response0.json()) as Record<string, unknown>;
    expect(body0).toHaveProperty("duels");
    expect(body0).toHaveProperty("nextCursor");
    const duels0 = body0.duels as Record<string, unknown>[];
    expect(duels0).toHaveLength(2);
    const duelIds0 = duels0.map((d) => d.duelId);
    expect(duelIds0).toContain("duel-seat-0-1");
    expect(duelIds0).toContain("duel-seat-0-2");

    // Request for player 1's duels
    const response1 = await server.fetch("/api/me/duels", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    expect(response1.status).toBe(200);
    const body1 = (await response1.json()) as Record<string, unknown>;
    expect(body1).toHaveProperty("duels");
    expect(body1).toHaveProperty("nextCursor");
    const duels1 = body1.duels as Record<string, unknown>[];
    expect(duels1).toHaveLength(1);
    const duelIds1 = duels1.map((d) => d.duelId);
    expect(duelIds1).toContain("duel-seat-1-1");

    // Verify they are different
    expect(duelIds0).not.toEqual(duelIds1);
  });

  it("answers the duels read with a cursor field the parser needs", async () => {
    const server = accountServer(players);

    const response = await server.fetch("/api/me/duels", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    expect(body).toHaveProperty("nextCursor");
    expect(body.nextCursor).toBeNull();
    expect(body).toHaveProperty("duels");
    expect(Array.isArray(body.duels)).toBe(true);
  });

  it("a name set on one player is not set on the other", async () => {
    // Create a fresh server instance with mutable players
    const testPlayers: ServerPlayer[] = [
      {
        playerId: "player-seat-0",
        deviceId: "device-seat-0",
        coinBalance: 100,
        displayName: "Alice",
        duels: [],
      },
      {
        playerId: "player-seat-1",
        deviceId: "device-seat-1",
        coinBalance: 37,
        displayName: "Bob",
        duels: [],
      },
    ];

    const server = accountServer(testPlayers);

    // Get initial names
    const initialResponse0 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    const initialBody0 = (await initialResponse0.json()) as Record<
      string,
      unknown
    >;
    expect(initialBody0.displayName).toBe("Alice");

    const initialResponse1 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    const initialBody1 = (await initialResponse1.json()) as Record<
      string,
      unknown
    >;
    expect(initialBody1.displayName).toBe("Bob");

    // Set name for player 0
    const putResponse = await server.fetch("/api/me/name", {
      method: "PUT",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({ name: "Charlie" }),
    });
    expect(putResponse.status).toBe(200);
    const putBody = (await putResponse.json()) as Record<string, unknown>;
    expect(putBody.displayName).toBe("Charlie");

    // Verify player 0 has the new name
    const afterResponse0 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    const afterBody0 = (await afterResponse0.json()) as Record<string, unknown>;
    expect(afterBody0.displayName).toBe("Charlie");

    // Verify player 1 still has the old name
    const afterResponse1 = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    const afterBody1 = (await afterResponse1.json()) as Record<string, unknown>;
    expect(afterBody1.displayName).toBe("Bob");
  });

  it("the name survives into the next profile read", async () => {
    // Create a fresh server instance with mutable players
    const testPlayers: ServerPlayer[] = [
      {
        playerId: "player-seat-0",
        deviceId: "device-seat-0",
        coinBalance: 100,
        displayName: "Alice",
        duels: [],
      },
    ];

    const server = accountServer(testPlayers);

    // Set name via PUT
    const putResponse = await server.fetch("/api/me/name", {
      method: "PUT",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({ name: "Diana" }),
    });
    expect(putResponse.status).toBe(200);
    const putBody = (await putResponse.json()) as Record<string, unknown>;
    expect(putBody.displayName).toBe("Diana");

    // Read the profile again via GET /api/me
    const getResponse = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    expect(getResponse.status).toBe(200);
    const getBody = (await getResponse.json()) as Record<string, unknown>;
    expect(getBody.displayName).toBe("Diana");
  });
});
