import { beforeEach, describe, expect, it } from "vitest";
import * as module from "./account-offer-settled";
import {
  ACCOUNT_OFFER_SETTLED_STORAGE_KEY,
  readOfferSettled,
  markOfferSettled,
} from "./account-offer-settled";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readOfferSettled` and `markOfferSettled` take the `Storage` they
 * act on as a parameter, so the tests hand them one and rely on no global at all.
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

describe("the answer this browser gave the account offer", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("answers that nothing is settled in a browser that has never answered", () => {
    expect(readOfferSettled(storage)).toBe(false);
  });

  it("settles the offer under the one key it names, storing the sentinel", () => {
    markOfferSettled(storage);
    expect(readOfferSettled(storage)).toBe(true);
    expect(storage.getItem("pd.accountOfferSettled")).toBe("1");
  });

  it("tells the sentinel from every other value in the slot", () => {
    // Values written by the test, not by the module
    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "1");
    expect(readOfferSettled(storage)).toBe(true);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, " 1 ");
    expect(readOfferSettled(storage)).toBe(true);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "0");
    expect(readOfferSettled(storage)).toBe(false);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "");
    expect(readOfferSettled(storage)).toBe(false);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "   ");
    expect(readOfferSettled(storage)).toBe(false);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "true");
    expect(readOfferSettled(storage)).toBe(false);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "11");
    expect(readOfferSettled(storage)).toBe(false);

    storage.setItem(ACCOUNT_OFFER_SETTLED_STORAGE_KEY, "01");
    expect(readOfferSettled(storage)).toBe(false);
  });

  it("records the same answer twice without changing what is stored", () => {
    markOfferSettled(storage);
    markOfferSettled(storage);
    expect(readOfferSettled(storage)).toBe(true);
    expect(storage.length).toBe(1);
  });

  it("exports no way back to an unanswered offer", () => {
    expect(Object.keys(module).sort()).toEqual([
      "ACCOUNT_OFFER_SETTLED_STORAGE_KEY",
      "markOfferSettled",
      "readOfferSettled",
    ]);
  });
});
