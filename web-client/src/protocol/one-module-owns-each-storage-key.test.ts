/**
 * `session-token.ts` names `"pd.sessionToken"` as the one key its module writes, and
 * `device-id.ts` names `"pd.deviceId"` the same way — but nothing before this test stopped a
 * second module from writing either literal, shadowing the value the owning module reads back.
 * Two writers of the session token key means two sessions for one player, and `ADR-0030` §8's
 * "sign-out clears the token and only the token" becomes unprovable the moment a second file can
 * write or clear it. This scans production source text for a key literal and asserts the exact
 * set of files that hold it, the same shape `TASK-040709`'s `ProfileCreationIsOneStatementTest`
 * uses for `INSERT INTO player`.
 *
 * Two honest limits, so a reader does not mistake this for a stronger guard than it is:
 *
 * 1. It reads source text. A key **assembled from constants**, or split across a line break,
 *    escapes it. Every storage key in this client is one string literal on one line today, and
 *    this test is the reason to keep it that way.
 * 2. It is a **file-name set** assertion. A *second* write inside `session-token.ts` escapes it,
 *    and that is deliberate: owning the key is that module's job, and the defect guarded against
 *    is a writer somewhere else.
 */
import { describe, it, expect } from "vitest";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

function productionSourcesContaining(literal: string): string[] {
  const src = resolve(dirname(fileURLToPath(import.meta.url)), "..");
  if (!existsSync(src)) {
    throw new Error(
      `productionSourcesContaining: no source directory at ${src}`,
    );
  }

  const matches: string[] = [];

  const walk = (dir: string): void => {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const entryPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(entryPath);
        continue;
      }
      const isTypeScriptSource =
        entry.name.endsWith(".ts") || entry.name.endsWith(".tsx");
      const isTestFile =
        entry.name.endsWith(".test.ts") || entry.name.endsWith(".test.tsx");
      if (!isTypeScriptSource || isTestFile) {
        continue;
      }
      if (readFileSync(entryPath, "utf-8").includes(literal)) {
        matches.push(entry.name);
      }
    }
  };

  walk(src);
  return matches.sort();
}

describe("one module owns each storage key", () => {
  it("only the session-token module writes the session token key", () => {
    expect(productionSourcesContaining("pd.sessionToken")).toEqual([
      "session-token.ts",
    ]);
  });

  it("the scan tells two keys apart", () => {
    expect(productionSourcesContaining("pd.deviceId")).toEqual([
      "device-id.ts",
    ]);
  });

  it("only the account-offer-settled module writes the offer-settled key", () => {
    expect(productionSourcesContaining("pd.accountOfferSettled")).toEqual([
      "account-offer-settled.ts",
    ]);
  });
});
