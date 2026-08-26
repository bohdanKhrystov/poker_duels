import { readFromApi, type ApiFetch } from "./api";
import { readDeviceId } from "../protocol/device-id";

/** What `GET /api/me` answers. Contracted in `docs/protocol.md`, not generated. */
export interface PlayerProfile {
  readonly playerId: string;
  /** Signed, and negative is a correct answer (`ADR-0014`). Never clamped. */
  readonly coinBalance: number;
  /**
   * The name the player chose, or `null` when they hold none.
   * `displayName === null && !displayNameRemoved` means never set.
   * `displayName === null && displayNameRemoved` means removed by an operator (`ADR-0053`).
   * Never derived, never defaulted.
   */
  readonly displayName: string | null;
  /** `true` only when they hold no name and one was removed from them (`ADR-0053`). */
  readonly displayNameRemoved: boolean;
  /** `true` exactly when this player holds a live device binding; `false` covers revoked and never bound (`ADR-0049` §5). */
  readonly deviceRouteLive: boolean;
}

export type ProfileRead =
  | { readonly kind: "profile"; readonly profile: PlayerProfile }
  | { readonly kind: "no-profile" }
  | { readonly kind: "unavailable" };

/**
 * Parses a wire body into a `PlayerProfile`, or returns `null` if the body is invalid.
 *
 * `playerId`, `coinBalance`, `displayName`, `displayNameRemoved`, and `deviceRouteLive`
 * are required on the wire — no defaults. A missing or wrong-typed field answers `null`.
 */
export function profileFromBody(body: unknown): PlayerProfile | null {
  if (
    typeof body === "object" &&
    body !== null &&
    typeof (body as Record<string, unknown>).playerId === "string" &&
    typeof (body as Record<string, unknown>).coinBalance === "number" &&
    typeof (body as Record<string, unknown>).displayNameRemoved === "boolean" &&
    typeof (body as Record<string, unknown>).deviceRouteLive === "boolean"
  ) {
    const displayName = (body as Record<string, unknown>).displayName;
    if (typeof displayName === "string" || displayName === null) {
      return {
        playerId: (body as Record<string, unknown>).playerId as string,
        coinBalance: (body as Record<string, unknown>).coinBalance as number,
        displayName,
        displayNameRemoved: (body as Record<string, unknown>)
          .displayNameRemoved as boolean,
        deviceRouteLive: (body as Record<string, unknown>)
          .deviceRouteLive as boolean,
      };
    }
  }
  return null;
}

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
      const profile = profileFromBody(apiRead.body);
      if (profile !== null) {
        return { kind: "profile", profile };
      }
      return { kind: "unavailable" };
    }
  }
}
