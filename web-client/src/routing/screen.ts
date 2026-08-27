/**
 * A named screen in the duel client. The first screen is the one a player sees when
 * the client boots; the duels, leaderboard and account screens are chosen by the player.
 */
export type Screen = "first" | "duels" | "leaderboard" | "account";

/**
 * The screen named by a URL fragment, reading only the first segment.
 * `#/duels/2026` returns `"duels"` (ADR-0081 §1); anything unrecognised
 * defaults to `"first"`. A bare fragment is an element identifier and must
 * resolve to `"first"` rather than to the screen it looks like (ADR-0076 §4).
 */
export function screenFromHash(hash: string): Screen {
  // Remove leading # if present
  const fragment = hash.startsWith("#") ? hash.slice(1) : hash;

  // Empty fragment or just "/" is the first screen
  if (!fragment || fragment === "/") {
    return "first";
  }

  // A bare fragment like "duels" (no /) is an element identifier, treat as "first"
  if (!fragment.startsWith("/")) {
    return "first";
  }

  // Extract first segment (skip the leading /)
  const segments = fragment.slice(1).split("/");
  const firstSegment = segments[0];

  // Match against known screens (case-sensitive, lowercase only)
  switch (firstSegment) {
    case "duels":
      return "duels";
    case "leaderboard":
      return "leaderboard";
    case "account":
      return "account";
    default:
      return "first";
  }
}

/**
 * The URL fragment for a screen. `hashForScreen("first")` returns `"/"`, which
 * indicates the first screen's address carries no fragment (ADR-0076 §3, §7).
 * The other screens return `"#/duels"`, `"#/leaderboard"` and the account
 * screen's own fragment.
 */
export function hashForScreen(screen: Screen): string {
  // The slugs "duels", "leaderboard" and "account" are literals in this file,
  // not derived from heading names at runtime, because a URL that changed
  // when a heading was restyled would break every link that ever worked
  // (ADR-0076 §1). "account" is not coined here: it is the lowercase ASCII
  // form of a word the product already says to a player — ADR-0050 §3's
  // merged confirmation sentence ("…will never sign in to this account
  // again"), ADR-0036's decision, and ADR-0056 §2's "no account was created".
  switch (screen) {
    case "first":
      return "/";
    case "duels":
      return "#/duels";
    case "leaderboard":
      return "#/leaderboard";
    case "account":
      return "#/account";
  }
}
