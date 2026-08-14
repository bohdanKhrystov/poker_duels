import { readFileSync } from "node:fs";
import { basename } from "node:path";
import { describe, expect, it } from "vitest";
import { guardedFiles } from "../protocol/boundary";

const OPENS_A_CONNECTION = /\b(?:connectToDuelServer|openConnection)\b/;
const BOOTS_A_CLIENT = /\bbootDuelClient\s*\(/;
const EVERY_BOOT = /\bbootDuelClient\s*\(/g;

// Tests drive both by design — the boot tests open a connection over a
// FakeSocket — and `boot.ts` is where `bootDuelClient` is declared. What
// ADR-0032 forbids is a second one in shipped code: one boot per tab is where
// "exactly once" lives, and a screen may never hold a connection of its own.
function shippedFiles(): string[] {
  return guardedFiles().filter(
    (file) =>
      !/\.test\.tsx?$/.test(file) &&
      basename(file) !== "boot.ts" &&
      basename(file) !== "boundary.ts",
  );
}

function shippedFilesMatching(pattern: RegExp): string[] {
  return shippedFiles()
    .filter((file) => pattern.test(readFileSync(file, "utf-8")))
    .map((file) => basename(file));
}

function sourceOf(name: string): string {
  const file = shippedFiles().find((path) => basename(path) === name);
  if (file === undefined) throw new Error(`no shipped file named ${name}`);
  return readFileSync(file, "utf-8");
}

describe("the tab's one connection", () => {
  it("is named by main.tsx and by no other shipped file", () => {
    expect(shippedFilesMatching(OPENS_A_CONNECTION)).toEqual(["main.tsx"]);
  });

  it("is booted once, in main.tsx", () => {
    expect(shippedFilesMatching(BOOTS_A_CLIENT)).toEqual(["main.tsx"]);
    expect(sourceOf("main.tsx").match(EVERY_BOOT)).toHaveLength(1);
  });
});
