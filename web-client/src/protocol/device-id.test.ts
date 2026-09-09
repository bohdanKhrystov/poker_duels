import { beforeEach, describe, expect, it } from "vitest";
import {
  DEVICE_ID_STORAGE_KEY,
  readDeviceId,
  writeDeviceId,
  forgetDeviceId,
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

  it("forgetting the device id leaves nothing to read", () => {
    writeDeviceId(storage, "d-1");
    forgetDeviceId(storage);
    expect(readDeviceId(storage)).toBeNull();
    // The key check: if forgetDeviceId had written "", readDeviceId would
    // still return null due to trimming, but the raw item would not be null.
    expect(storage.getItem(DEVICE_ID_STORAGE_KEY)).toBeNull();
  });

  it("forgetting the device id leaves every other key where it was", () => {
    writeDeviceId(storage, "d-1");
    storage.setItem("pd.sessionToken", "token-1");
    storage.setItem("pd.roomCode", "room-1");
    storage.setItem("pd.nameAskSkipped", "yes");
    forgetDeviceId(storage);
    // The device id should be gone
    expect(readDeviceId(storage)).toBeNull();
    // The other three should remain
    expect(storage.getItem("pd.sessionToken")).toBe("token-1");
    expect(storage.getItem("pd.roomCode")).toBe("room-1");
    expect(storage.getItem("pd.nameAskSkipped")).toBe("yes");
    // Storage should contain exactly three items
    expect(storage.length).toBe(3);
  });

  it("forgetting an id this browser never held changes nothing", () => {
    storage.setItem("pd.sessionToken", "token-1");
    storage.setItem("pd.roomCode", "room-1");
    storage.setItem("pd.nameAskSkipped", "yes");
    const lengthBefore = storage.length;
    forgetDeviceId(storage);
    expect(readDeviceId(storage)).toBeNull();
    expect(storage.getItem("pd.sessionToken")).toBe("token-1");
    expect(storage.getItem("pd.roomCode")).toBe("room-1");
    expect(storage.getItem("pd.nameAskSkipped")).toBe("yes");
    expect(storage.length).toBe(lengthBefore);
  });

  it("removes the device id key even when this browser never held one", () => {
    // "Changes nothing" (the test above) is indistinguishable from "removed an
    // absent key" if we only look at the resulting storage contents — a
    // conditional forgetDeviceId that skips removeItem when the key is
    // missing would pass that test unchanged. ADR-0135 §2 requires the call
    // unconditionally, so we watch the call itself.
    storage.setItem("pd.sessionToken", "token-1");
    const originalRemoveItem = storage.removeItem.bind(storage);
    const removedKeys: string[] = [];
    storage.removeItem = (key: string): void => {
      removedKeys.push(key);
      originalRemoveItem(key);
    };
    try {
      forgetDeviceId(storage);
      expect(removedKeys).toContain(DEVICE_ID_STORAGE_KEY);
    } finally {
      // Never let an instrumented removeItem leak into a test that runs after.
      storage.removeItem = originalRemoveItem;
    }
  });
});
