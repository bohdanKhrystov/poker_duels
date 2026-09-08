// A suggestion is drawn from `NAME_VOCABULARY`, synchronously, by a pure function of a random
// source (ADR-0137 §2). It takes no player fact — no profile, no id, no session, no room code —
// and reads no fact fixed at the moment the page loaded or the browser last kept a key: there is
// nowhere in this signature to put one, and nothing here is asked to remember a previous draw.
// The default source lives at the caller, never here (ADR-0137 §§2, 7).

import { NAME_VOCABULARY } from "./name-vocabulary";

/** The floor `NAME_VOCABULARY`'s arity is held to (ADR-0137 §4, `ADR-0119` §4). */
export const SUGGESTION_SPACE_FLOOR = 1_000_000;

/**
 * The most whole draws {@link suggestName} makes before returning whatever it last drew, even a
 * repeat of `replacing`. A source stuck at one value must not spin, and at
 * {@link SUGGESTION_SPACE_FLOOR} combinations a genuine repeat is about one in a million, so four
 * draws is generous, not tight (ADR-0137 §5).
 */
const MAX_DRAWS = 4;

/** Draws one entry from each list in `NAME_VOCABULARY`, in the lists' own order, and joins them
 * with a single U+0020. `random` must return a number in `[0, 1)` per call. */
function draw(random: () => number): string {
  const words: string[] = [];
  for (const list of NAME_VOCABULARY) {
    // Clamped so a `random` that returns `1` still yields the list's last entry rather than
    // `undefined` — without the clamp, `undefined` could be joined into a string a player is
    // offered as a permanent name (ADR-0137 §2).
    const index = Math.min(list.length - 1, Math.floor(random() * list.length));
    words.push(list[index]);
  }
  return words.join(" ");
}

/**
 * Draws a name via {@link draw}. When `replacing` is given and the draw equals it, draws again,
 * up to {@link MAX_DRAWS} whole draws in total — never fewer entries per list, never a partial
 * redraw of some words and not others. The last draw is returned even when it still equals
 * `replacing`; there is no failure mode, only a suggestion the player can type over.
 */
export function suggestName(random: () => number, replacing?: string): string {
  let name = draw(random);
  for (let draws = 1; draws < MAX_DRAWS && name === replacing; draws++) {
    name = draw(random);
  }
  return name;
}
