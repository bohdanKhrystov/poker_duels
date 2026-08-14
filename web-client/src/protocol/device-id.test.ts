import { beforeEach, describe, expect, it } from "vitest";
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
 * this module. `readDeviceId` and `writeDeviceId` take the `Storage` they act on
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

describe("the device id store", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("reads nothing on a first visit", () => {
    expect(readDeviceId(storage)).toBeNull();
  });

  it("reads back what was written", () => {
    writeDeviceId(storage, "d-1");
    expect(readDeviceId(storage)).toBe("d-1");
  });

  it("treats a blank stored value as no device id", () => {
    storage.setItem(DEVICE_ID_STORAGE_KEY, "   ");
    expect(readDeviceId(storage)).toBeNull();
  });

  it("writes under the one key the profile endpoint will read", () => {
    writeDeviceId(storage, "d-1");
    // The literal, not DEVICE_ID_STORAGE_KEY: asserting a constant against
    // itself proves nothing, and this string is what STORY-0311 must match.
    expect(storage.getItem("pd.deviceId")).toBe("d-1");
  });
});
