import { render, screen, cleanup } from "@testing-library/react";
import { describe, it, expect } from "vitest";
import type { ServerMessage } from "../protocol";
import { Lobby } from "./Lobby";
import { DuelProvider } from "../store/duel-provider";
import { createDuelStore } from "../store/duel-store";
import { aView } from "../table/view-fixture";

const SEAT = 1;

const ROOM_JOINED: ServerMessage = {
  type: "RoomJoined",
  code: "ABCDEFGH",
  seat: SEAT,
};

const SNAPSHOT: ServerMessage = {
  type: "Snapshot",
  view: aView({ viewerSeat: SEAT }),
};

const AWAY: ServerMessage = {
  type: "OpponentPresence",
  presence: "AWAY",
};

const ABSENT: ServerMessage = {
  type: "OpponentPresence",
  presence: "ABSENT",
};

const PRESENT: ServerMessage = {
  type: "OpponentPresence",
  presence: "PRESENT",
};

// ADR-0046 §2, quoted verbatim. Literals, not an import from `presence-text.ts`:
// a constant shared by the encoder and its test would let one typo pass both
// sides at once.
const AWAY_LINE = "Your rival is away. The duel is paused.";
const ABSENT_LINE =
  "Your rival did not come back. The duel continues, and the server acts for them.";
const BACK_LINE = "Your rival is back.";
const THE_THREE_LINES = [AWAY_LINE, ABSENT_LINE, BACK_LINE] as const;

/**
 * The four states `ADR-0046` §2 distinguishes, each driven by the frames a
 * real server would send this client at seat 1 — never by a prop passed
 * straight to a component. `expectedLine` is `null` exactly once: a fresh
 * `PRESENT` with no `AWAY` or `ABSENT` ever held renders none of the three.
 */
const FOUR_STATES: readonly {
  readonly name: string;
  readonly frames: readonly ServerMessage[];
  readonly expectedLine: string | null;
}[] = [
  { name: "AWAY", frames: [SNAPSHOT, AWAY], expectedLine: AWAY_LINE },
  { name: "ABSENT", frames: [SNAPSHOT, ABSENT], expectedLine: ABSENT_LINE },
  {
    name: "PRESENT after an AWAY",
    frames: [SNAPSHOT, AWAY, PRESENT],
    expectedLine: BACK_LINE,
  },
  {
    name: "PRESENT on a fresh store",
    frames: [SNAPSHOT, PRESENT],
    expectedLine: null,
  },
];

/**
 * `ADR-0046` §5, one regex per phrase, case-insensitive, with word
 * boundaries. Phrases, not fragments: `\bout\b` would also catch `Timed
 * out`, the one place those words are correct (§5's own row for it).
 * `sample` is a sentence this pattern must match, used to prove the pattern
 * itself is live rather than a typo that never fires.
 */
const REFUSED: readonly {
  readonly pattern: RegExp;
  readonly sample: string;
}[] = [
  { pattern: /\bopponent\b/i, sample: "Your opponent is away." },
  { pattern: /\bdisconnected\b/i, sample: "Your rival disconnected." },
  { pattern: /\bconnection lost\b/i, sample: "Connection lost." },
  { pattern: /\boffline\b/i, sample: "Your rival is offline." },
  { pattern: /\bleft\b/i, sample: "Your rival left." },
  { pattern: /\bquit\b/i, sample: "Your rival quit." },
  { pattern: /\babandoned\b/i, sample: "Your rival abandoned the duel." },
  { pattern: /\bgave up\b/i, sample: "Your rival gave up." },
  { pattern: /\bforfeit\b/i, sample: "Your rival will forfeit." },
  { pattern: /\bforfeited\b/i, sample: "Your rival forfeited." },
  { pattern: /\bsitting out\b/i, sample: "Your rival is sitting out." },
  { pattern: /\bsit out\b/i, sample: "Your rival will sit out." },
  { pattern: /\bauto-fold\b/i, sample: "This was an auto-fold." },
  { pattern: /\bauto-check\b/i, sample: "This was an auto-check." },
  { pattern: /!/, sample: "Your rival is away!" },
];

/** The whole duel screen, driven from a fresh store by the given frames. */
function renderDuelScreen(frames: readonly ServerMessage[]): HTMLElement {
  const store = createDuelStore();
  store.apply(ROOM_JOINED);
  for (const frame of frames) store.apply(frame);
  const { container } = render(
    <DuelProvider store={store} send={() => {}}>
      <Lobby />
    </DuelProvider>,
  );
  return container;
}

/**
 * Every text node under `container`, joined by a space.
 *
 * Copied from `no-derivation.test.tsx`, not imported (that file exports
 * nothing): `textContent` runs the last word of one element into the first
 * of the next, and `\b` then misses a boundary the eye sees plainly. A
 * `TreeWalker` over every text node also reaches a `<span>` or a `<div>`
 * exactly as it reaches a `<p>` — nothing here is limited to a named list of
 * tag types.
 */
function wordsOnScreen(container: HTMLElement): string {
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  const parts: string[] = [];
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    parts.push(node.textContent ?? "");
  }
  return parts.join(" ");
}

/**
 * Every string the table speaks without printing it: `aria-label` and
 * `title`. Copied from `no-derivation.test.tsx`: a screen reader reads the
 * first aloud and a browser shows the second on hover, so a word reaches a
 * player from either exactly as surely as from print — and neither is a
 * text node, so `wordsOnScreen` cannot see it.
 */
function spokenOnScreen(container: HTMLElement): string {
  return [...container.querySelectorAll("[aria-label], [title]")]
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null)
    .join(" ");
}

describe("the duel screen's presence copy", () => {
  it("says exactly one of the four lines, in each of the four states", () => {
    for (const state of FOUR_STATES) {
      renderDuelScreen(state.frames);

      if (state.expectedLine !== null) {
        expect(screen.getByText(state.expectedLine)).toBeDefined();
      }
      for (const line of THE_THREE_LINES) {
        if (line === state.expectedLine) continue;
        expect(screen.queryByText(line)).toBeNull();
      }

      cleanup();
    }
  });

  it("puts none of the refused words in front of a player", () => {
    for (const state of FOUR_STATES) {
      const container = renderDuelScreen(state.frames);
      const words = wordsOnScreen(container);
      const spoken = spokenOnScreen(container);

      for (const { pattern } of REFUSED) {
        // Both sweeps, asserted separately: a word that reaches only a
        // screen reader must still fail this test.
        expect(words).not.toMatch(pattern);
        expect(spoken).not.toMatch(pattern);
      }

      cleanup();
    }
  });

  it("reports every refused word when one is planted", () => {
    const container = renderDuelScreen([SNAPSHOT]);
    let reported = 0;

    for (const { pattern, sample } of REFUSED) {
      const plant = document.createElement("p");
      plant.textContent = sample;
      container.appendChild(plant);

      if (pattern.test(wordsOnScreen(container))) reported += 1;

      container.removeChild(plant);
    }

    // Tied to the pattern count, not a literal: a loop reduced to one word
    // would still report 1, and only comparing against `REFUSED.length`
    // catches that a single pass says nothing about the other fourteen.
    expect(reported).toBe(REFUSED.length);
  });
});
