import { readdirSync } from "node:fs";
import { dirname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

// Two shapes only: a hex triplet/quad/six/eight literal, and the opening of a
// functional colour notation. `color-mix(` is deliberately absent — it only
// ever wraps `var(--pd-*)` tokens, so matching it would flag legitimate code.
const HEX_LITERAL = /#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\b/g;
const FUNCTIONAL_LITERAL = /\b(?:rgba?|hsla?|lab|lch|oklab|oklch)\s*\(/g;

/**
 * Every colour literal in `source`, in the order it appears.
 *
 * Deliberately narrow: a hex code or a functional colour notation. Anything
 * that resolves through a `var(--pd-*)` token — including `color-mix()` — is
 * out of scope, because a token reference is exactly what this guard exists
 * to require.
 */
export function findColorLiterals(source: string): string[] {
  const found: Array<{ index: number; value: string }> = [];
  for (const pattern of [HEX_LITERAL, FUNCTIONAL_LITERAL]) {
    for (const match of source.matchAll(pattern)) {
      found.push({ index: match.index ?? 0, value: match[0] });
    }
  }
  return found.sort((a, b) => a.index - b.index).map((match) => match.value);
}

const SCANNED_EXTENSIONS = [".ts", ".tsx", ".css", ".html"];

// Paths relative to `src/`. The generated file's bytes belong to the Kotlin
// emitter (ADR-0026) and are excluded by path, never by a pragma inside it —
// the same mechanism ESLint and Prettier use for the same file. The token
// sheet is excluded because it *is* the token layer: the literals belong
// there and nowhere else.
const EXCLUDED_SRC_PATHS = ["protocol/protocol.gen.ts", "styles/tokens.css"];

function isExcluded(relativePath: string): boolean {
  if (relativePath.endsWith(".test.ts") || relativePath.endsWith(".test.tsx")) {
    // Fixtures need colour literals by construction; nothing in a test
    // reaches the bundle.
    return true;
  }
  return EXCLUDED_SRC_PATHS.includes(relativePath.split(sep).join("/"));
}

// Resolved from the module's own URL, not `process.cwd()`, so the walk finds
// the same root regardless of where the test runner's working directory is.
function clientRoot(): string {
  return resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");
}

/**
 * Absolute paths of every file the guard covers, sorted.
 *
 * Everything under `src/` with an extension a human writes colour into,
 * minus the generated protocol file, the token sheet and test fixtures, plus
 * `index.html` at the client root.
 */
export function scannedFiles(): string[] {
  const root = clientRoot();
  const srcDir = join(root, "src");
  const entries = readdirSync(srcDir, { recursive: true }) as string[];

  const files = entries
    .filter((entry) =>
      SCANNED_EXTENSIONS.some((extension) => entry.endsWith(extension)),
    )
    .filter((entry) => !isExcluded(entry))
    .map((entry) => join(srcDir, entry));

  files.push(join(root, "index.html"));

  return files.sort();
}
