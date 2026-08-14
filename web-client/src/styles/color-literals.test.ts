import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { findColorLiterals, scannedFiles } from "./color-literals";

describe("the colour literal guard", () => {
  it("flags a hex literal", () => {
    expect(findColorLiterals("color: #ece9e3;")).toEqual(["#ece9e3"]);
  });

  it("flags rgb and hsl functions", () => {
    expect(
      findColorLiterals("a{color:rgb(1 2 3)}b{color:hsl(0 0% 0%)}"),
    ).toHaveLength(2);
  });

  it("flags an oklch literal", () => {
    expect(findColorLiterals("color: oklch(63.7% 0.237 25.331)")).toHaveLength(
      1,
    );
  });

  it("passes a token reference", () => {
    expect(
      findColorLiterals(
        "color: var(--pd-text); background: color-mix(in oklab, var(--pd-accent) 40%, transparent)",
      ),
    ).toEqual([]);
  });

  it("scans the app source and skips the token layer and the generated file", () => {
    const files = scannedFiles();

    expect(files.some((file) => file.endsWith("/src/App.tsx"))).toBe(true);
    expect(
      files.some((file) => file.endsWith("/src/styles/color-literals.ts")),
    ).toBe(true);
    expect(
      files.some((file) => file.endsWith("/src/protocol/protocol.gen.ts")),
    ).toBe(false);
    expect(files.some((file) => file.endsWith("/src/styles/tokens.css"))).toBe(
      false,
    );
  });

  it("finds no colour literal in the client source", () => {
    const offenders = scannedFiles()
      .map((file) => ({
        file,
        literals: findColorLiterals(readFileSync(file, "utf-8")),
      }))
      .filter((entry) => entry.literals.length > 0);

    expect(offenders).toEqual([]);
  });
});
