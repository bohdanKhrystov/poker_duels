import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const here = dirname(fileURLToPath(import.meta.url));
const appCss = readFileSync(resolve(here, "app.css"), "utf-8");
const tokensCss = readFileSync(resolve(here, "tokens.css"), "utf-8");

const THEME_START = "@theme static {";

/**
 * The raw lines strictly between `@theme static {` and the first line that is
 * exactly `}` — the closing brace is required to stand alone, per the ticket,
 * so a literal line match is what "the block" means here.
 */
function themeBlockLines(css: string): string[] {
  const start = css.indexOf(THEME_START);
  if (start === -1) {
    throw new Error("app.css has no `@theme static {` block");
  }
  const lines = css.slice(start + THEME_START.length).split("\n");
  const end = lines.findIndex((line) => line === "}");
  if (end === -1) {
    throw new Error("`@theme static` block never closes on a line by itself");
  }
  return lines.slice(0, end);
}

/** Every `--pd-*` custom property tokens.css actually declares. */
function declaredTokens(css: string): Set<string> {
  const names = new Set<string>();
  const declaration = /^\s*(--pd-[\w-]+)\s*:/gm;
  let match: RegExpExecArray | null;
  while ((match = declaration.exec(css)) !== null) {
    names.add(match[1]);
  }
  return names;
}

const RESET_WILDCARD = /^--[\w-]+-\*:\s*initial;$/;
const RESET_SINGLE = /^--[\w-]+:\s*initial;$/;
const REFERENCE = /^--[\w-]+:\s*var\((--pd-[\w-]+)\);$/;

describe("the Tailwind theme", () => {
  it("declares nothing but resets and references to tokens that exist", () => {
    const tokens = declaredTokens(tokensCss);
    const declarations = themeBlockLines(appCss)
      .filter(
        (line) => !line.trim().startsWith("/*") && !line.trim().startsWith("*"),
      )
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    const offenders = declarations.filter((line) => {
      if (RESET_WILDCARD.test(line) || RESET_SINGLE.test(line)) {
        return false;
      }
      const reference = REFERENCE.exec(line);
      if (reference === null) {
        return true;
      }
      return !tokens.has(reference[1]);
    });

    expect(offenders).toEqual([]);
    // A parse that silently matched nothing (empty block, wrong marker, etc.)
    // would otherwise make the assertion above pass forever.
    expect(declarations.length).toBeGreaterThan(20);
  });
});
