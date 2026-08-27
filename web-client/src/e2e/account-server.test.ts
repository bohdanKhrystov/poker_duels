import { describe, expect, it } from "vitest";

import { accountServer, type ServerPlayer } from "./account-server";

const players: readonly ServerPlayer[] = [
  {
    playerId: "player-seat-0",
    deviceId: "device-seat-0",
    coinBalance: 100,
    displayName: "Alice",
  },
  {
    playerId: "player-seat-1",
    deviceId: "device-seat-1",
    coinBalance: 37,
    displayName: "Bob",
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
});
