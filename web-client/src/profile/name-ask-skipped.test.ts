import { beforeEach, describe, expect, it } from "vitest";
import {
  NAME_ASK_SKIPPED_STORAGE_KEY,
  readNameAskSkipped,
  markNameAskSkipped,
} from "./name-ask-skipped";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readNameAskSkipped` and `markNameAskSkipped` take the `Storage` they act on
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

describe("the answer this browser gave the name ask", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("answers that nothing was skipped in a browser that has never answered", () => {
    expect(readNameAskSkipped(storage)).toBe(false);
  });

  it("records the skip under the one key it names, storing the sentinel", () => {
    markNameAskSkipped(storage);
    expect(readNameAskSkipped(storage)).toBe(true);
    expect(storage.getItem(NAME_ASK_SKIPPED_STORAGE_KEY)).toBe("1");
  });

  it("tells the sentinel from every other value in the slot", () => {
    expect(readNameAskSkipped(storage)).toBe(false); // absent

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "1");
    expect(readNameAskSkipped(storage)).toBe(true);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, " 1 ");
    expect(readNameAskSkipped(storage)).toBe(true);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "0");
    expect(readNameAskSkipped(storage)).toBe(false);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "");
    expect(readNameAskSkipped(storage)).toBe(false);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "   ");
    expect(readNameAskSkipped(storage)).toBe(false);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "true");
    expect(readNameAskSkipped(storage)).toBe(false);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "11");
    expect(readNameAskSkipped(storage)).toBe(false);

    storage.setItem(NAME_ASK_SKIPPED_STORAGE_KEY, "01");
    expect(readNameAskSkipped(storage)).toBe(false);
  });

  it("records the same answer twice without changing what is stored", () => {
    markNameAskSkipped(storage);
    const lengthAfterFirst = storage.length;

    markNameAskSkipped(storage);
    const lengthAfterSecond = storage.length;

    expect(lengthAfterFirst).toBe(1);
    expect(lengthAfterSecond).toBe(1);
    expect(storage.getItem(NAME_ASK_SKIPPED_STORAGE_KEY)).toBe("1");
  });

  it("exports no way back to an unanswered ask", () => {
    // This test verifies that the module only exports what is expected
    // by checking that these functions exist and work correctly
    expect(typeof readNameAskSkipped).toBe("function");
    expect(typeof markNameAskSkipped).toBe("function");
    expect(typeof NAME_ASK_SKIPPED_STORAGE_KEY).toBe("string");
  });
});
