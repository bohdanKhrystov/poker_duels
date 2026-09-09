/* @vitest-environment node */

import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync } from "fs";
import { fileURLToPath } from "url";
import path from "path";

import * as nameAskText from "../profile/name-ask-text";
import * as accountText from "../account/account-text";

/**
 * `ADR-0142` §1's mechanism: a text module is checked against the card it was transcribed from,
 * in the client's own suite, so the check outlives the ticket's `verify:` block that first
 * compared them.
 */

const SRC_DIR = fileURLToPath(new URL("..", import.meta.url));
const SCREENS_DIR = new URL("../../../design/screens/", import.meta.url);

function cardPath(fileName: string): string {
  return fileURLToPath(new URL(fileName, SCREENS_DIR));
}

// `ADR-0142` §2: the separator is NUL, deliberately not a newline. A newline separator also
// splits at a card's own line wraps, which cut `account.html`'s `<p class="line">` sentences in
// half and reported all three as absent from a card that plainly carries them.
const SEPARATOR = "\0";

function decodeEntities(text: string): string {
  return text
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&nbsp;/g, " ")
    .replace(/&#x([0-9a-fA-F]+);/g, (_match, hex: string) =>
      String.fromCodePoint(parseInt(hex, 16)),
    )
    .replace(/&#([0-9]+);/g, (_match, dec: string) =>
      String.fromCodePoint(parseInt(dec, 10)),
    );
}

/**
 * A card's rendered text, as `ADR-0142` §2 defines it: every comment, `<script>`/`<style>` body
 * and tag replaced by the NUL separator, entities decoded, then split on that separator with
 * whitespace runs collapsed and empty pieces dropped. A comment is not card text — comments are
 * stripped before the split, on purpose, so a marker comment can never satisfy this gate.
 */
function cardTextUnits(fileName: string): readonly string[] {
  const html = readFileSync(cardPath(fileName), "utf-8");
  const stripped = html
    .replace(/<!--[\s\S]*?-->/g, SEPARATOR)
    .replace(/<script[\s\S]*?<\/script>/gi, SEPARATOR)
    .replace(/<style[\s\S]*?<\/style>/gi, SEPARATOR)
    .replace(/<[^>]+>/g, SEPARATOR);
  return decodeEntities(stripped)
    .split(SEPARATOR)
    .map((unit) => unit.replace(/\s+/g, " ").trim())
    .filter((unit) => unit.length > 0);
}

function normalizeWhitespace(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

/** Every exported string constant of a module namespace, by name. */
function stringExportsOf(moduleNamespace: object): ReadonlyMap<string, string> {
  const result = new Map<string, string>();
  for (const [name, value] of Object.entries(moduleNamespace)) {
    if (typeof value === "string") {
      result.set(name, value);
    }
  }
  return result;
}

interface Pair {
  /** The module's path, relative to `web-client/src`, exactly as `findTextModules` reports it. */
  readonly modulePath: string;
  readonly moduleNamespace: object;
  /** The card file, under `design/screens/`. */
  readonly card: string;
  /** Export names whose value must appear on the card. */
  readonly carded: readonly string[];
  /** Export names that do not appear on the card, and the one-line reason each does not. */
  readonly notCarded: Readonly<Record<string, string>>;
}

/**
 * `ADR-0142` §4 and §5: the unit of correspondence is a module and a card, paired one to one,
 * with every exported string constant of the module classified `carded` or `notCarded`.
 *
 * Bootstrapped against the two modules `DEC-155` named: `name-ask-text.ts` (`TASK-140710`, all
 * six exports carded) and `account-text.ts` (`TASK-140811`, fourteen of twenty-seven carded).
 * `account-text.ts`'s thirteen `notCarded` reasons are each the state or response this card draws
 * no frame for.
 */
const PAIRS: readonly Pair[] = [
  {
    modulePath: "profile/name-ask-text.ts",
    moduleNamespace: nameAskText,
    card: "name-ask.html",
    carded: [
      "NAME_ASK_HEADING",
      "NAME_IS_FOR",
      "NAME_KEEPS",
      "NAME_FIELD_LABEL",
      "TAKE_NAME_LABEL",
      "SKIP_AND_PLAY_LABEL",
    ],
    notCarded: {},
  },
  {
    modulePath: "account/account-text.ts",
    moduleNamespace: accountText,
    card: "account.html",
    carded: [
      "ACCOUNT_HEADING",
      "DEVICE_ROUTE_LIVE",
      "PASSWORD_ROUTE_LIVE",
      "SIGN_OUT_LABEL",
      "SIGN_OUT_WARNING",
      "SIGN_UP_LABEL",
      "HANDLE_LABEL",
      "PASSWORD_LABEL",
      "SIGN_IN_HEADING",
      "SIGN_IN_LABEL",
      "CANCEL",
      "ANONYMOUS_STATE",
      "ANONYMOUS_COST",
      "ANONYMOUS_WAY_OUT",
    ],
    notCarded: {
      DEVICE_ROUTE_REVOKED: "no frame on this card draws a revoked device",
      REVOKE_LABEL: "no frame on this card draws device revocation yet",
      REVOKE_PERMANENT: "no frame on this card draws device revocation yet",
      REVOKE_OTHER_SESSIONS:
        "no frame on this card draws device revocation yet",
      REVOKE_ONLY_WAY_BACK: "no frame on this card draws device revocation yet",
      SIGNED_UP: "no frame on this card draws a just-completed sign-up",
      HANDLE_REFUSED: "no frame on this card draws a refused handle",
      HANDLE_UNAVAILABLE: "no frame on this card draws an unavailable handle",
      PASSWORD_REFUSED: "no frame on this card draws a refused password",
      NO_PROFILE_YET: "no frame on this card draws a missing-profile failure",
      SIGN_UP_FAILED: "no frame on this card draws a failed sign-up",
      SIGN_UP_THROTTLED: "no frame on this card draws a throttled sign-up",
      SIGN_IN_REFUSED: "no frame on this card draws a refused sign-in",
    },
  },
];

/**
 * `ADR-0142` §6: coverage is total over `*-text.ts`, so a new module cannot escape the register.
 * Every module not in `PAIRS` above must be here, with the one-line reason it carries no card
 * correspondence yet.
 */
const NO_CARD: Readonly<Record<string, string>> = {
  "account/recovery-text.ts":
    "shares account.html with account-text.ts; its own transcription is a future ticket's, not this bootstrap's",
  "history/history-text.ts":
    "duels.html exists as a card; this module's transcription is a future ticket's, not this bootstrap's",
  "ladder/ladder-text.ts":
    "leaderboard.html exists as a card; this module's transcription is a future ticket's, not this bootstrap's",
  "profile/name-text.ts":
    "no frame anywhere draws these three strings; name-ask.html's own NAME_KEEPS has superseded PERMANENCE_LINE's wording",
  "profile/profile-text.ts":
    "exports functions only, no string constant to classify",
  "result/outcome-text.ts":
    "exports functions only, no string constant to classify",
  "table/absent-action-text.ts":
    "exports functions only; duel-table-states.html's correspondence is check-frame-cards.sh's job (TASK-141107)",
  "table/action-text.ts":
    "exports functions and an interface only, no string constant to classify",
  "table/card-text.ts":
    "exports functions only, no string constant to classify",
  "table/presence-text.ts":
    "exports functions only; duel-table-states.html's correspondence is check-frame-cards.sh's job (TASK-141107)",
  "table/rejection-text.ts":
    "exports functions only, no string constant to classify",
};

/** Every `*-text.ts` module under `web-client/src`, `*.test.ts` excluded, relative to `src`. */
function findTextModules(dir: string): readonly string[] {
  const modules: string[] = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const entryPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      modules.push(...findTextModules(entryPath));
    } else if (
      entry.name.endsWith("-text.ts") &&
      !entry.name.endsWith(".test.ts")
    ) {
      modules.push(path.relative(SRC_DIR, entryPath).split(path.sep).join("/"));
    }
  }
  return modules;
}

describe("card text", () => {
  describe.each(PAIRS)("$modulePath", (pair) => {
    it("classifies every exported string constant as carded or notCarded", () => {
      for (const name of stringExportsOf(pair.moduleNamespace).keys()) {
        const classified = pair.carded.includes(name) || name in pair.notCarded;
        expect(
          classified,
          `${name} is exported by ${pair.modulePath} but is neither carded nor notCarded`,
        ).toBe(true);
      }
    });

    it("registers no name that is not an actual export", () => {
      const exported = stringExportsOf(pair.moduleNamespace);
      for (const name of [...pair.carded, ...Object.keys(pair.notCarded)]) {
        expect(
          exported.has(name),
          `${name} is registered against ${pair.modulePath} but is not an exported string constant of it`,
        ).toBe(true);
      }
    });

    it("carries every carded export's imported value on its card", () => {
      const exported = stringExportsOf(pair.moduleNamespace);
      const units = cardTextUnits(pair.card);
      for (const name of pair.carded) {
        const value = exported.get(name);
        expect(
          value,
          `${name} is not an exported string constant of ${pair.modulePath}`,
        ).toBeDefined();
        const normalized = normalizeWhitespace(value as string);
        const found = units.some((unit) => unit.includes(normalized));
        expect(
          found,
          `${name}'s value is not on ${pair.card}: ${JSON.stringify(normalized)}`,
        ).toBe(true);
      }
    });
  });

  it("classifies every *-text.ts module as paired or NO_CARD", () => {
    const paired = new Set(PAIRS.map((pair) => pair.modulePath));
    for (const modulePath of findTextModules(SRC_DIR)) {
      const classified = paired.has(modulePath) || modulePath in NO_CARD;
      expect(classified, `${modulePath} is neither in PAIRS nor NO_CARD`).toBe(
        true,
      );
    }
  });
});
