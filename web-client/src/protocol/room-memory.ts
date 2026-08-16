/**
 * The one key this browser's room code is stored under.
 *
 * Exported because the room code needs to be shared with any module that might
 * need to access it, ensuring one identity for one room per browser.
 */
export const ROOM_CODE_STORAGE_KEY = "pd.roomCode";

/** The room code this browser is in, or `null` if it has never joined a room. */
export function readRoomCode(storage: Storage): string | null {
  const value = storage.getItem(ROOM_CODE_STORAGE_KEY);
  if (value === null) {
    return null;
  }
  // Trim to test, but return the stored value verbatim
  if (value.trim() === "") {
    return null;
  }
  return value;
}

/** Remember the room code this browser is seated in. */
export function writeRoomCode(storage: Storage, code: string): void {
  storage.setItem(ROOM_CODE_STORAGE_KEY, code);
}

/** Forget the room code, making this browser appear as if it has never been in a room. */
export function forgetRoomCode(storage: Storage): void {
  storage.removeItem(ROOM_CODE_STORAGE_KEY);
}
