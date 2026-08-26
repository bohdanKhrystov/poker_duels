import { beforeEach, describe, expect, it } from "vitest";
import {
  SESSION_TOKEN_STORAGE_KEY,
  readSessionToken,
  writeSessionToken,
  forgetSessionToken,
} from "./session-token";
import { readDeviceId, DEVICE_ID_STORAGE_KEY } from "./device-id";
import { readRoomCode, ROOM_CODE_STORAGE_KEY } from "./room-memory";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readSessionToken`, `writeSessionToken` and `forgetSessionToken`
 * take the `Storage` they act on as a parameter, so the tests hand them one and
 * rely on no global at all.
 */
function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

describe("the session token this browser holds", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("reads back the token it was given, byte for byte", () => {
    writeSessionToken(storage, "  tok-en  ");
    expect(readSessionToken(storage)).toBe("  tok-en  ");

    writeSessionToken(storage, "tok-en");
    expect(readSessionToken(storage)).toBe("tok-en");
  });

  it("answers with nothing when this browser holds none", () => {
    expect(readSessionToken(storage)).toBeNull();
  });

  it("answers with nothing for a blank token", () => {
    storage.setItem(SESSION_TOKEN_STORAGE_KEY, "");
    expect(readSessionToken(storage)).toBeNull();

    storage.setItem(SESSION_TOKEN_STORAGE_KEY, "   ");
    expect(readSessionToken(storage)).toBeNull();
  });

  it("forgetting the token leaves the device id and the room code exactly where they were", () => {
    storage.setItem(DEVICE_ID_STORAGE_KEY, "d-1");
    storage.setItem(ROOM_CODE_STORAGE_KEY, "room-123");
    writeSessionToken(storage, "token-abc");

    forgetSessionToken(storage);

    expect(readDeviceId(storage)).toBe("d-1");
    expect(readRoomCode(storage)).toBe("room-123");
    expect(readSessionToken(storage)).toBeNull();
  });

  it("writes under the one key the module names", () => {
    writeSessionToken(storage, "token-abc");
    // The literal, not SESSION_TOKEN_STORAGE_KEY: asserting a constant against
    // itself proves nothing, and this string is what the browser must match.
    expect(storage.getItem("pd.sessionToken")).toBe("token-abc");
  });
});
