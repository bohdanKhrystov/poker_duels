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
      hasRecoveryEmail: false,
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
      hasRecoveryEmail: false,
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

  it("a claim attaches the credential to the device own player", async () => {
    const server = accountServer(players);

    const signUpResponse = await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "alice-handle",
        password: "alice-password",
      }),
    });
    expect(signUpResponse.status).toBe(201);

    const signInResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "alice-handle",
        password: "alice-password",
      }),
    });
    expect(signInResponse.status).toBe(200);
    const signInBody = (await signInResponse.json()) as Record<string, unknown>;
    const token = signInBody.sessionToken as string;
    expect(typeof token).toBe("string");

    // The token names the device that claimed the handle, and not the other
    // player — both are claimable, so the right answer is not the only one.
    expect(server.tokens.get(token)).toBe("player-seat-0");
    expect(server.tokens.get(token)).not.toBe("player-seat-1");
  });

  it("a claim moves no coin and renames nobody", async () => {
    const server = accountServer(players);

    const beforeResponse = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    const beforeBody = await beforeResponse.json();

    const signUpResponse = await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "claim-handle",
        password: "claim-password",
      }),
    });
    expect(signUpResponse.status).toBe(201);

    const afterResponse = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-0" },
    });
    const afterBody = await afterResponse.json();

    // ADR-0030 §1 as an assertion over the whole body, not one field: a claim
    // names no column of `player`, so coinBalance and displayName are
    // byte-unchanged and there is no third field the claim could have moved.
    expect(afterBody).toEqual(beforeBody);
  });

  it("a claim of a handle somebody already holds is refused", async () => {
    const server = accountServer(players);

    const firstSignUp = await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "shared-handle",
        password: "first-password",
      }),
    });
    expect(firstSignUp.status).toBe(201);

    const secondSignUp = await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-1" },
      body: JSON.stringify({
        handle: "shared-handle",
        password: "second-password",
      }),
    });
    expect(secondSignUp.status).toBe(409);

    // The credential still names the first player: the first password still
    // signs in and names player-seat-0, the second player's password does not.
    const signInAsFirst = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "shared-handle",
        password: "first-password",
      }),
    });
    expect(signInAsFirst.status).toBe(200);
    const signInAsFirstBody = (await signInAsFirst.json()) as Record<
      string,
      unknown
    >;
    expect(server.tokens.get(signInAsFirstBody.sessionToken as string)).toBe(
      "player-seat-0",
    );

    const signInAsSecond = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "shared-handle",
        password: "second-password",
      }),
    });
    expect(signInAsSecond.status).toBe(401);
  });

  it("sign in issues a token for the player who claimed the handle", async () => {
    const server = accountServer(players);

    await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "repeat-handle",
        password: "repeat-password",
      }),
    });

    const firstSignIn = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "repeat-handle",
        password: "repeat-password",
      }),
    });
    expect(firstSignIn.status).toBe(200);
    const firstBody = (await firstSignIn.json()) as Record<string, unknown>;
    expect(typeof firstBody.sessionToken).toBe("string");

    const secondSignIn = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "repeat-handle",
        password: "repeat-password",
      }),
    });
    expect(secondSignIn.status).toBe(200);
    const secondBody = (await secondSignIn.json()) as Record<string, unknown>;
    expect(typeof secondBody.sessionToken).toBe("string");

    // A constant token would satisfy a laxer assertion; two successive
    // sign-ins must return two distinct strings.
    expect(firstBody.sessionToken).not.toBe(secondBody.sessionToken);

    // Both name the player who claimed the handle.
    expect(server.tokens.get(firstBody.sessionToken as string)).toBe(
      "player-seat-0",
    );
    expect(server.tokens.get(secondBody.sessionToken as string)).toBe(
      "player-seat-0",
    );
  });

  it("a wrong password and an unknown handle answer the same 401", async () => {
    const server = accountServer(players);

    await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "known-handle",
        password: "correct-password",
      }),
    });

    const wrongPasswordResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "known-handle",
        password: "wrong-password",
      }),
    });
    const unknownHandleResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "nobody-has-this-handle",
        password: "correct-password",
      }),
    });

    // Both statuses are the literal 401 the merged sign-in.ts maps, not
    // merely equal to each other.
    expect(wrongPasswordResponse.status).toBe(401);
    expect(unknownHandleResponse.status).toBe(401);

    const wrongPasswordBody = await wrongPasswordResponse.json();
    const unknownHandleBody = await unknownHandleResponse.json();
    expect(wrongPasswordBody).toEqual({});
    expect(unknownHandleBody).toEqual({});
    expect(wrongPasswordBody).toEqual(unknownHandleBody);
  });

  it("a bearer token outranks the device id on the profile read", async () => {
    const server = accountServer(players);

    await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "outranks-profile-handle",
        password: "outranks-profile-password",
      }),
    });
    const signInResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "outranks-profile-handle",
        password: "outranks-profile-password",
      }),
    });
    const signInBody = (await signInResponse.json()) as Record<string, unknown>;
    const tokenForA = signInBody.sessionToken as string;

    // Carries B's device id and A's token — the two disagree on purpose.
    const response = await server.fetch("/api/me", {
      headers: {
        "X-Device-Id": "device-seat-1",
        Authorization: `Bearer ${tokenForA}`,
      },
    });
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    expect(body).toEqual({
      playerId: "player-seat-0",
      coinBalance: 100,
      displayName: "Alice",
      displayNameRemoved: false,
      deviceRouteLive: true,
      hasRecoveryEmail: false,
    });

    // The two fixture players differ in every field below, so the wrong
    // answer under a broken precedence is a different value, not a missing
    // one.
    expect(body.playerId).not.toBe("player-seat-1");
    expect(body.coinBalance).not.toBe(37);
    expect(body.displayName).not.toBe("Bob");
  });

  it("a bearer token outranks the device id on the duels read", async () => {
    const server = accountServer(players);

    await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-0" },
      body: JSON.stringify({
        handle: "outranks-duels-handle",
        password: "outranks-duels-password",
      }),
    });
    const signInResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "outranks-duels-handle",
        password: "outranks-duels-password",
      }),
    });
    const signInBody = (await signInResponse.json()) as Record<string, unknown>;
    const tokenForA = signInBody.sessionToken as string;

    // Carries B's device id and A's token, against a different route — the
    // precedence is a property of the resolver, not of one route.
    const response = await server.fetch("/api/me/duels", {
      headers: {
        "X-Device-Id": "device-seat-1",
        Authorization: `Bearer ${tokenForA}`,
      },
    });
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    const duelIds = (body.duels as Record<string, unknown>[]).map(
      (d) => d.duelId,
    );
    expect(duelIds).toContain("duel-seat-0-1");
    expect(duelIds).toContain("duel-seat-0-2");
    expect(duelIds).not.toContain("duel-seat-1-1");
  });

  it("the device id still answers when no token is carried", async () => {
    const server = accountServer(players);

    // Same device id as the two tests above, but no Authorization header at
    // all. Without this test, a double that always answers A's player would
    // still pass the precedence tests above.
    const response = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    expect(response.status).toBe(200);
    const body = (await response.json()) as Record<string, unknown>;
    expect(body).toEqual({
      playerId: "player-seat-1",
      coinBalance: 37,
      displayName: "Bob",
      displayNameRemoved: false,
      deviceRouteLive: true,
      hasRecoveryEmail: false,
    });
  });

  it("a token naming no live session is refused rather than falling back", async () => {
    const server = accountServer(players);

    // A valid device id rides alongside a token nobody ever issued.
    const response = await server.fetch("/api/me", {
      headers: {
        "X-Device-Id": "device-seat-1",
        Authorization: "Bearer made-up-token-nobody-issued",
      },
    });
    expect(response.status).toBe(401);
    const body = await response.json();
    expect(body).toEqual({});
  });

  it("signing out returns the browser to the device it holds", async () => {
    const server = accountServer(players);

    await server.fetch("/api/auth/sign-up", {
      method: "POST",
      headers: { "X-Device-Id": "device-seat-1" },
      body: JSON.stringify({
        handle: "sign-out-handle",
        password: "sign-out-password",
      }),
    });
    const signInResponse = await server.fetch("/api/auth/sign-in", {
      method: "POST",
      headers: {},
      body: JSON.stringify({
        handle: "sign-out-handle",
        password: "sign-out-password",
      }),
    });
    const signInBody = (await signInResponse.json()) as Record<string, unknown>;
    const token = signInBody.sessionToken as string;

    const signOutResponse = await server.fetch("/api/auth/sign-out", {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(signOutResponse.status).toBe(204);

    // The same token no longer names a session.
    const afterSignOut = await server.fetch("/api/me", {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(afterSignOut.status).toBe(401);

    // The device it holds answers its own player again, unaided by any
    // token.
    const deviceOnly = await server.fetch("/api/me", {
      headers: { "X-Device-Id": "device-seat-1" },
    });
    expect(deviceOnly.status).toBe(200);
    const deviceOnlyBody = (await deviceOnly.json()) as Record<string, unknown>;
    expect(deviceOnlyBody).toEqual({
      playerId: "player-seat-1",
      coinBalance: 37,
      displayName: "Bob",
      displayNameRemoved: false,
      deviceRouteLive: true,
      hasRecoveryEmail: false,
    });
  });
});
