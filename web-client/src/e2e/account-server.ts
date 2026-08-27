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
}

export function accountServer(players: readonly ServerPlayer[]): AccountServer {
  const requests: RecordedRequest[] = [];
  // Cast to mutable array to allow displayName updates
  const mutablePlayers = players as ServerPlayer[];

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
  };
}
