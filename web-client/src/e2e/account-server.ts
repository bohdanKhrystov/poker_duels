import type { ApiFetch, ApiResponse } from "../profile/api";
import { meBody } from "../profile/profile-fixture";

export interface ServerPlayer {
  readonly playerId: string;
  readonly deviceId: string;
  readonly coinBalance: number;
  readonly displayName: string | null;
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
    const player = players.find((p) => p.deviceId === deviceId);

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
