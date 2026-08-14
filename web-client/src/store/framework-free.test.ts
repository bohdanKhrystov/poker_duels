import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

// The three modules ADR-0032 keeps free of React: the reducer, the store around
// it, and the boot wiring. `duel-provider.tsx` is the one React-aware file here,
// and it is deliberately absent from this list.
const FRAMEWORK_FREE = ["duel-state.ts", "duel-store.ts", "boot.ts"];

function sourceOf(name: string): string {
  const here = dirname(fileURLToPath(import.meta.url));
  return readFileSync(join(here, name), "utf-8");
}

describe("the store's framework-free modules", () => {
  it.each(FRAMEWORK_FREE)("%s imports nothing from react", (name) => {
    // Both forms: `from "react…"` and a bare `import "react"`. The second is
    // pointless code nobody writes on purpose, but a guard that says "imports
    // nothing from react" should not have a shape of import it cannot see.
    expect(sourceOf(name)).not.toMatch(/(?:from|import)\s+"react/);
  });
});
