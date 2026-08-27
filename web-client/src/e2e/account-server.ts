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

export function accountServer(players: readonly ServerPlayer[]): AccountServer {
  const requests: RecordedRequest[] = [];
  // Cast to mutable array to allow displayName updates
  const mutablePlayers = players as ServerPlayer[];

  // handle -> { password, playerId }. playerId is read from the resolved
  // player at claim time (ADR-0030 §1) — never a parameter, never the handle.
  const credentials = new Map<
    string,
    { readonly password: string; readonly playerId: string }
  >();

  // token -> playerId, minted from a counter so two sign-ins are two
  // distinct strings. Written here; read by TASK-041405.
  const tokens = new Map<string, string>();
  let nextTokenNumber = 1;

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

    // Resolve the device id from the X-Device-Id header
    const deviceId = init.headers["X-Device-Id"];
    const player = mutablePlayers.find((p) => p.deviceId === deviceId);

    // Handle GET /api/me
    if (path === "/api/me" && method === "GET") {
      if (player === undefined) {
        // Device id not found or missing
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      // Return the player's profile
      const profileBody = meBody({
        playerId: player.playerId,
        coinBalance: player.coinBalance,
        displayName: player.displayName,
        displayNameRemoved: false,
        deviceRouteLive: true,
      });

      return {
        status: 200,
        json: async () => profileBody,
      };
    }

    // Handle GET /api/me/duels
    if (path.startsWith("/api/me/duels") && method === "GET") {
      if (player === undefined) {
        return {
          status: 401,
          json: async () => ({}),
        };
      }

      return {
        status: 200,
        json: async () => ({
          duels: player.duels,
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
