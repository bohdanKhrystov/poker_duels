import { readFromApi, type ApiFetch } from "./api";
import { readDeviceId } from "../protocol/device-id";

/** What `GET /api/me` answers. Contracted in `docs/protocol.md`, not generated. */
export interface PlayerProfile {
  readonly playerId: string;
  /** Signed, and negative is a correct answer (`ADR-0014`). Never clamped. */
  readonly coinBalance: number;
}

export type ProfileRead =
  | { readonly kind: "profile"; readonly profile: PlayerProfile }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Reads the player profile from the API.
 *
 * Uses the device ID stored in the given storage to authenticate with /api/me.
 * Returns the profile if the server answers with a valid body, or no-profile/unavailable otherwise.
 */
export async function readProfile(deps: {
  readonly fetch: ApiFetch;
  readonly storage: Storage;
}): Promise<ProfileRead> {
  const deviceId = readDeviceId(deps.storage);
  const apiRead = await readFromApi({
    fetch: deps.fetch,
    deviceId,
    path: "/api/me",
  });

  switch (apiRead.kind) {
    case "no-profile":
      return { kind: "no-profile" };

    case "unavailable":
      return { kind: "unavailable" };

    case "body": {
      const body = apiRead.body;
      // Validate that body has the required fields with correct types
      if (
        typeof body === "object" &&
        body !== null &&
        typeof (body as Record<string, unknown>).playerId === "string" &&
        typeof (body as Record<string, unknown>).coinBalance === "number"
      ) {
        const profile: PlayerProfile = {
          playerId: (body as Record<string, unknown>).playerId as string,
          coinBalance: (body as Record<string, unknown>).coinBalance as number,
        };
        return { kind: "profile", profile };
      }

      return { kind: "unavailable" };
    }
  }
}
