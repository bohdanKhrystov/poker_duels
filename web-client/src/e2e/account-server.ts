import type { ApiFetch, ApiResponse } from "../profile/api";
import { meBody } from "../profile/profile-fixture";

export interface ServerPlayer {
  readonly playerId: string;
  readonly deviceId: string;
  readonly coinBalance: number;
  displayName: string | null;
  readonly duels: readonly Record<string, unknown>[];
}

export interface RecordedRequest {
  readonly path: string;
  readonly method: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly body: string | null;
}

export interface AccountServer {
  readonly fetch: ApiFetch;
  readonly requests: readonly RecordedRequest[];
  /** `token → playerId`, written by sign-in here and read by `TASK-041405`. */
  readonly tokens: ReadonlyMap<string, string>;
}

// Strips the "Bearer " prefix every caller in this codebase sends
// (authorized-fetch.ts, sign-out.ts), or returns undefined when the request
// carries no Authorization header at all.
function bearerTokenFrom(
  headers: Readonly<Record<string, string>>,
): string | undefined {
  const header = headers["Authorization"];
  if (header === undefined) {
    return undefined;
  }
  return header.startsWith("Bearer ") ? header.slice("Bearer ".length) : header;
}

export function accountServer(players: readonly ServerPlayer[]): AccountServer {
  const requests: RecordedRequest[] = [];
  // A shallow copy per player: PUT /api/me/name writes displayName in place,
  // and the double must not mutate the caller's array or its objects — two
  // accountServer(players) calls over the same fixture must not see each
  // other's writes.
  const mutablePlayers: ServerPlayer[] = players.map((p) => ({ ...p }));

  // handle -> { password, playerId }. playerId is read from the resolved
  // player at claim time (ADR-0030 §1) — never a parameter, never the handle.
  const credentials = new Map<
    string,
    { readonly password: string; readonly playerId: string }
  >();

  // token -> playerId, minted from a counter so two sign-ins are two
  // distinct strings. Written by sign-in and deleted by sign-out, below;
  // resolved by resolveSessionPlayer. Own Map per accountServer(...)
  // instance — never shared with the caller's fixture, for the same reason
  // mutablePlayers is copied above.
  const tokens = new Map<string, string>();
  let nextTokenNumber = 1;

  // ADR-0027 §4: a live session outranks the device id, unconditionally —
  // any device id presented alongside a resolving token is not even
  // inspected. A token that names no live session resolves to nothing, not
  // the device id, so the caller refuses rather than silently downgrading
  // to anonymous. Used by GET /api/me and GET /api/me/duels only: sign-up
  // resolves by device id alone (TASK-041404) and sign-in carries no
  // identity to resolve yet.
  const resolveSessionPlayer = (
    headers: Readonly<Record<string, string>>,
  ): ServerPlayer | undefined => {
    const token = bearerTokenFrom(headers);
    if (token !== undefined) {
      const playerId = tokens.get(token);
      return playerId === undefined
        ? undefined
        : mutablePlayers.find((p) => p.playerId === playerId);
    }

    const deviceId = headers["X-Device-Id"];
    return mutablePlayers.find((p) => p.deviceId === deviceId);
  };

  const fetch: ApiFetch = async (
    path: string,
    init: {
      readonly headers: Readonly<Record<string, string>>;
      readonly method?: string;
      readonly body?: string;
    },
  ): Promise<ApiResponse> => {
    const method = init.method ?? "GET";
    const body = init.body ?? null;

    // Record the request immediately, before any routing decision
    requests.push({
      path,
      method,
      headers: init.headers,
      body,
    });

    // Resolve the device id from the X-Device-Id header. Used as-is by
    // PUT /api/me/name and POST /api/auth/sign-up below, which stay
    // device-only — TASK-041405's scope names only the two GET routes.
    // GET /api/me and GET /api/me/duels resolve via resolveSessionPlayer
    // instead, which a live bearer token outranks.
    const deviceId = init.headers["X-Device-Id"];
    const player = mutablePlayers.find((p) => p.deviceId === deviceId);

    // Handle GET /api/me
    if (path === "/api/me" && method === "GET") {
      const sessionPlayer = resolveSessionPlayer(init.headers);
      if (sessionPlayer === undefined) {
        // No live session token and no known device id, or a token naming
        // no live session — which refuses rather than falling back.
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      // Return the player's profile
      const profileBody = meBody({
        playerId: sessionPlayer.playerId,
        coinBalance: sessionPlayer.coinBalance,
        displayName: sessionPlayer.displayName,
        displayNameRemoved: false,
        deviceRouteLive: true,
        hasRecoveryEmail: false,
      });

      return {
        status: 200,
        json: async () => profileBody,
      };
    }

    // Handle GET /api/me/duels
    if (path.startsWith("/api/me/duels") && method === "GET") {
      const sessionPlayer = resolveSessionPlayer(init.headers);
      if (sessionPlayer === undefined) {
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      return {
        status: 200,
        json: async () => ({
          duels: sessionPlayer.duels,
          nextCursor: null,
        }),
      };
    }

    // Handle PUT /api/me/name
    if (path === "/api/me/name" && method === "PUT") {
      if (player === undefined) {
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      // Parse the name from the body
      let name: string | null = null;
      try {
        const bodyObj = JSON.parse(body ?? "{}");
        name = typeof bodyObj.name === "string" ? bodyObj.name : null;
      } catch {
        // If parsing fails, name stays null
      }

      // Update the player's display name
      if (name !== null) {
        player.displayName = name;
      }

      // Return the full profile body with the updated name
      const profileBody = meBody({
        playerId: player.playerId,
        coinBalance: player.coinBalance,
        displayName: player.displayName,
        displayNameRemoved: false,
        deviceRouteLive: true,
        hasRecoveryEmail: false,
      });

      return {
        status: 200,
        json: async () => profileBody,
      };
    }

    // Handle POST /api/auth/sign-up
    if (path === "/api/auth/sign-up" && method === "POST") {
      if (player === undefined) {
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      // Parse the handle and password from the body
      let handle: string | null = null;
      let password: string | null = null;
      try {
        const bodyObj = JSON.parse(body ?? "{}");
        handle = typeof bodyObj.handle === "string" ? bodyObj.handle : null;
        password =
          typeof bodyObj.password === "string" ? bodyObj.password : null;
      } catch {
        // If parsing fails, both stay null
      }

      if (handle !== null && credentials.has(handle)) {
        return {
          status: 409,
          json: async () => ({}),
        };
      }

      // The credential names the device's own player, read now — a claim
      // adds a credential and moves nothing else (ADR-0030 §1). coinBalance,
      // displayName and duels are never touched by this branch.
      if (handle !== null && password !== null) {
        credentials.set(handle, { password, playerId: player.playerId });
      }

      return {
        status: 201,
        json: async () => ({}),
      };
    }

    // Handle POST /api/auth/sign-in
    if (path === "/api/auth/sign-in" && method === "POST") {
      // Parse the handle and password from the body
      let handle: string | null = null;
      let password: string | null = null;
      try {
        const bodyObj = JSON.parse(body ?? "{}");
        handle = typeof bodyObj.handle === "string" ? bodyObj.handle : null;
        password =
          typeof bodyObj.password === "string" ? bodyObj.password : null;
      } catch {
        // If parsing fails, both stay null
      }

      const credential = handle !== null ? credentials.get(handle) : undefined;

      // A wrong password and an unknown handle answer the same 401.
      if (credential === undefined || credential.password !== password) {
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      // Minted from a counter: opaque to every caller, carries no player id.
      const token = `session-token-${nextTokenNumber}`;
      nextTokenNumber += 1;
      tokens.set(token, credential.playerId);

      return {
        status: 200,
        json: async () => ({ sessionToken: token }),
      };
    }

    // Handle POST /api/auth/sign-out
    if (path === "/api/auth/sign-out" && method === "POST") {
      const token = bearerTokenFrom(init.headers);
      if (token !== undefined) {
        // Keyed by token: deletes exactly the presented session and no
        // other. A second token for the same player, from another sign-in,
        // is a different key and is untouched.
        tokens.delete(token);
      }

      // 204 is the only status docs/protocol.md documents for this route
      // (sign-out.ts's own contract comment) — an unknown or missing token
      // answers it too, rather than surfacing whether a session existed.
      return {
        status: 204,
        json: async () => ({}),
      };
    }

    // Unknown path
    return {
      status: 500,
      json: async () => ({}),
    };
  };

  return {
    fetch,
    get requests() {
      return [...requests];
    },
    get tokens() {
      return new Map(tokens);
    },
  };
}
