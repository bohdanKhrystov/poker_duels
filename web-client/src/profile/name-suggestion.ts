// A suggestion is drawn from `NAME_VOCABULARY`, synchronously, by a pure function of a random
// source (ADR-0137 §2). It takes no player fact — no profile, no id, no session, no room code —
// and reads no fact fixed at the moment the page loaded or the browser last kept a key: there is
// nowhere in this signature to put one, and nothing here is asked to remember a previous draw.
// The default source lives at the caller, never here (ADR-0137 §§2, 7).

import { NAME_VOCABULARY } from "./name-vocabulary";

/** The floor `NAME_VOCABULARY`'s arity is held to (ADR-0137 §4, `ADR-0119` §4). */
export const SUGGESTION_SPACE_FLOOR = 1_000_000;

/**
 * Draws one entry from each list in `NAME_VOCABULARY`, in the lists' own order, and joins them
 * with a single U+0020. `random` must return a number in `[0, 1)` per call.
 *
 * The index is clamped so a `random` that returns `1` still yields the list's last entry rather
 * than `undefined` — without the clamp, `undefined` could be joined into a string a player is
 * offered as a permanent name (ADR-0137 §2).
 */
export function suggestName(random: () => number): string {
  const words: string[] = [];
  for (const list of NAME_VOCABULARY) {
    const index = Math.min(list.length - 1, Math.floor(random() * list.length));
    words.push(list[index]);
  }
  return words.join(" ");
}
