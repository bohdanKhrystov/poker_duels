import { readFileSync, readdirSync } from "node:fs";
import { dirname, join, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

// The shape `protocol.gen.ts` (ADR-0026) emits for every wire type: an
// exported type alias or interface at the start of a line.
const EXPORTED_TYPE = /^export (?:type|interface) (\w+)/gm;

// Requires a `=`, `<`, `{` or `extends` after the name so that prose in a
// comment — "the type Snapshot comes from the wire" — is not a violation. It
// deliberately does not match `import type { Snapshot }`: importing a wire
// type is the behaviour this guard exists to require.
const DECLARATION =
  /\b(?:interface|type|class|enum)\s+(\w+)\s*(?:[=<{]|extends\b)/g;

const RAW_FRAME = /\b(?:WebSocket|MessageEvent)\b/g;

/** Every type name the generated protocol file exports, sorted. */
export function reservedWireNames(): string[] {
  const generatedPath = join(
    dirname(fileURLToPath(import.meta.url)),
    "protocol.gen.ts",
  );
  const source = readFileSync(generatedPath, "utf-8");
  return [...source.matchAll(EXPORTED_TYPE)].map((match) => match[1]).sort();
}

/** Declared names in `source` that collide with `reserved`, in order. */
export function redeclaredWireTypes(
  source: string,
  reserved: readonly string[],
): string[] {
  const reservedNames = new Set(reserved);
  return [...source.matchAll(DECLARATION)]
    .map((match) => match[1])
    .filter((name) => reservedNames.has(name));
}

/** Every mention of a raw-frame API in `source`, in order. */
export function rawFrameMentions(source: string): string[] {
  return [...source.matchAll(RAW_FRAME)].map((match) => match[0]);
}

const GUARDED_EXTENSIONS = [".ts", ".tsx"];

// Resolved from the module's own URL, not `process.cwd()`, so the walk finds
// the same root regardless of where the test runner's working directory is.
function clientRoot(): string {
  return resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");
}

// `protocol/` is the one place a raw frame and a wire type belong — its own
// bytes, including `protocol.gen.ts` and this file, are excluded by path,
// never by a pragma inside them. Test files elsewhere are not excluded: a
// fixture may import a wire type but may not declare one.
function isUnderProtocolModule(relativePath: string): boolean {
  return relativePath.split(sep).join("/").startsWith("protocol/");
}

/** Absolute paths of every file the guard covers, sorted. */
export function guardedFiles(): string[] {
  const root = clientRoot();
  const srcDir = join(root, "src");
  const entries = readdirSync(srcDir, { recursive: true }) as string[];

  return entries
    .filter((entry) =>
      GUARDED_EXTENSIONS.some((extension) => entry.endsWith(extension)),
    )
    .filter((entry) => !isUnderProtocolModule(entry))
    .map((entry) => join(srcDir, entry))
    .sort();
}
