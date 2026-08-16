import { beforeEach, describe, expect, it } from "vitest";
import {
  ROOM_CODE_STORAGE_KEY,
  readRoomCode,
  writeRoomCode,
  forgetRoomCode,
} from "./room-memory";
import {
  DEVICE_ID_STORAGE_KEY,
  readDeviceId,
  writeDeviceId,
} from "./device-id";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readRoomCode` and `writeRoomCode` take the `Storage` they act on
 * as a parameter, so the tests hand them one and rely on no global at all.
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

describe("the remembered room code", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("reads back each room code it was told to remember", () => {
    writeRoomCode(storage, "ABCDEFGH");
    expect(readRoomCode(storage)).toBe("ABCDEFGH");

    writeRoomCode(storage, "ZYXWVUTS");
    expect(readRoomCode(storage)).toBe("ZYXWVUTS");
  });

  it("answers null when this browser has never been in a room", () => {
    expect(readRoomCode(storage)).toBeNull();
  });

  it("answers null when the stored code is blank", () => {
    storage.setItem(ROOM_CODE_STORAGE_KEY, "   ");
    expect(readRoomCode(storage)).toBeNull();
  });

  it("forgets the room it was remembering", () => {
    writeRoomCode(storage, "ABCDEFGH");
    forgetRoomCode(storage);
    expect(readRoomCode(storage)).toBeNull();
    expect(storage.getItem(ROOM_CODE_STORAGE_KEY)).toBeNull();
  });

  it("keeps the room code under a key of its own", () => {
    writeDeviceId(storage, "d-9");
    writeRoomCode(storage, "ABCDEFGH");
    expect(ROOM_CODE_STORAGE_KEY).not.toBe(DEVICE_ID_STORAGE_KEY);
    expect(readDeviceId(storage)).toBe("d-9");
  });
});
