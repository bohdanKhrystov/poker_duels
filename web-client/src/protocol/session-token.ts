/**
 * The one key this browser's session token is stored under.
 *
 * Exported because `STORY-0412` sends the same value in the `Authorization`
 * header. Two keys would mean two sessions for one player.
 */
export const SESSION_TOKEN_STORAGE_KEY = "pd.sessionToken";

/** The session token this browser holds, or `null` on a first visit. */
export function readSessionToken(storage: Storage): string | null {
  const value = storage.getItem(SESSION_TOKEN_STORAGE_KEY);
  if (value === null) {
    return null;
  }
  // Trim to test, but return the stored value verbatim
  if (value.trim() === "") {
    return null;
  }
  return value;
}

/** Remember the session token the server issued. */
export function writeSessionToken(storage: Storage, token: string): void {
  storage.setItem(SESSION_TOKEN_STORAGE_KEY, token);
}

/** Forget the session token, restoring the device to anonymous. */
export function forgetSessionToken(storage: Storage): void {
  storage.removeItem(SESSION_TOKEN_STORAGE_KEY);
}
