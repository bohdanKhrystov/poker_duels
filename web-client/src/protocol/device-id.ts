/**
 * The one key this browser's device id is stored under.
 *
 * Exported because `STORY-0311` sends the same value as the `X-Device-Id`
 * header on `GET /api/me`. Two keys would mean two identities for one player.
 */
export const DEVICE_ID_STORAGE_KEY = "pd.deviceId";

/** The device id this browser holds, or `null` on a first visit. */
export function readDeviceId(storage: Storage): string | null {
  const value = storage.getItem(DEVICE_ID_STORAGE_KEY);
  if (value === null) {
    return null;
  }
  // Trim to test, but return the stored value verbatim
  if (value.trim() === "") {
    return null;
  }
  return value;
}

/** Remember the device id the server issued. */
export function writeDeviceId(storage: Storage, deviceId: string): void {
  storage.setItem(DEVICE_ID_STORAGE_KEY, deviceId);
}

/**
 * Abandon this browser's ownership of its profile by removing its device id from storage.
 *
 * This function is reachable from `signOut` and from nowhere else, in the abandoning case alone
 * (ADR-0135 §2). ADR-0027 §5's stale-client harm stays forbidden: no reload, no failed handshake,
 * no refused session, no `Welcome` carrying a null `deviceId` and no error path may reach it.
 *
 * This mechanism enumerates what it forgets and never sweeps: `storage.clear()` is not what this
 * is.
 */
export function forgetDeviceId(storage: Storage): void {
  storage.removeItem(DEVICE_ID_STORAGE_KEY);
}
