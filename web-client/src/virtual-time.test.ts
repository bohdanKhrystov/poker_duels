import { readFileSync, readdirSync } from "node:fs";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const REACHES_FOR_A_TIMER =
  /\b(?:setTimeout|setInterval|requestAnimationFrame)\b/;
const INSTALLS_FAKE_ONES = /vi\.useFakeTimers\s*\(/;

/** Test files that touch a timer without first making it virtual. */
function sleepingTestFiles(sources: Map<string, string>): string[] {
  return [...sources.entries()]
    .filter(
      ([, source]) =>
        REACHES_FOR_A_TIMER.test(source) && !INSTALLS_FAKE_ONES.test(source),
    )
    .map(([name]) => name);
}

/** Every test file under src/, mapped by basename: { filename: source }. */
function testSources(): Map<string, string> {
  const srcDir = resolve(dirname(fileURLToPath(import.meta.url)));
  const entries = readdirSync(srcDir, { recursive: true }) as string[];

  const testEntries = entries.filter((entry) => /\.test\.tsx?$/.test(entry));

  const sources = new Map<string, string>();
  for (const entry of testEntries) {
    const fullPath = join(srcDir, entry);
    const source = readFileSync(fullPath, "utf-8");
    sources.set(entry, source);
  }

  return sources;
}

describe("the client's tests", () => {
  it("every test file that reaches for a timer installs fake ones first", () => {
    const sources = testSources();

    expect(sleepingTestFiles(sources)).toEqual([]);

    // A walk that found no files, or only files with no timer in them, would
    // satisfy the line above by saying nothing. Both are named here.
    expect(sources.size).toBeGreaterThan(40);
    expect(
      [...sources.values()].filter((source) =>
        REACHES_FOR_A_TIMER.test(source),
      ),
    ).not.toHaveLength(0);
  });

  it("sees the timer in a file that installs none", () => {
    const sleepWithoutFake = new Map([
      ["sleeping.test.ts", "it('x', () => { setTimeout(done, 10); });"],
    ]);

    expect(sleepingTestFiles(sleepWithoutFake)).toEqual(["sleeping.test.ts"]);

    const sleepWithFake = new Map([
      [
        "not-sleeping.test.ts",
        `vi.useFakeTimers();
it('x', () => { setTimeout(done, 10); });`,
      ],
    ]);

    expect(sleepingTestFiles(sleepWithFake)).toEqual([]);
  });
});
