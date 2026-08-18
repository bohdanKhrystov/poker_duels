import { render } from "@testing-library/react";
import { describe, it, expect, beforeEach } from "vitest";
import { readProfileStrip } from "./profile-strip";
import { ProfileStrip } from "./ProfileStrip";
import type { ApiFetch, ApiResponse } from "./api";
import { writeDeviceId } from "../protocol/device-id";
import { meBody, duelRowBody } from "./profile-fixture";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. Depending on that
 * global would make these tests a property of the Node version rather than of
 * this module. `readProfileStrip` takes the `Storage` it acts on as a parameter, so
 * the tests hand it one and rely on no global at all.
 */
function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

/** Records the details of each fetch call made. */
interface FetchCall {
  readonly path: string;
  readonly headers: Record<string, string>;
}

/**
 * Creates a mock fetch that records every call and answers them in order.
 * No network, no globals — purely synchronous recording and predetermined answers.
 */
function answering(...answers: readonly ApiResponse[]): {
  readonly calls: FetchCall[];
  readonly fetch: ApiFetch;
} {
  const calls: FetchCall[] = [];
  let answerIndex = 0;

  return {
    calls,
    fetch: async (
      path: string,
      init: { readonly headers: Readonly<Record<string, string>> },
    ): Promise<ApiResponse> => {
      calls.push({
        path,
        headers: { ...init.headers },
      });
      if (answerIndex >= answers.length) {
        throw new Error(
          `No more answers available (called ${answerIndex + 1} times)`,
        );
      }
      return answers[answerIndex++];
    },
  };
}

/** Constructs a successful response with the given body. */
function ok(body: unknown): ApiResponse {
  return {
    status: 200,
    json: async () => body,
  };
}

/**
 * Every text and attribute value on the screen, from text nodes, aria-label,
 * title, and every attribute value of every element.
 *
 * `aria-label` is read aloud and `title` is shown on hover, so content in either
 * reaches a player from exactly as surely as printed text does. Nor is everything
 * a player receives a word: `min`, `max`, `value` and other attributes reach the
 * DOM and nothing prints them, so content the client worked out for itself is
 * invisible to a text-and-aria scan. The scan must see everything a player
 * receives: every attribute of every element.
 */
function allContentOnScreen(container: HTMLElement): string {
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  const textParts: string[] = [];
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    textParts.push(node.textContent ?? "");
  }

  const spokenAndHovered = [
    ...container.querySelectorAll("[aria-label], [title]"),
  ]
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null);

  const allAttributes: string[] = [];
  container.querySelectorAll("*").forEach((element) => {
    for (const attr of element.attributes) {
      const value = attr.value;
      if (value) {
        allAttributes.push(value);
      }
    }
  });

  return [...textParts, ...spokenAndHovered, ...allAttributes].join(" ");
}

describe("the profile strip's surface", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("puts no opponent identifier anywhere on the screen", async () => {
    // Two rows whose bodies carry distinct opponentPlayerId values.
    // The wire carries them, but readRecentDuels drops them — so if they
    // appear on screen, the guard catches it.
    writeDeviceId(storage, "d-1");
    const mock = answering(
      ok(meBody({ coinBalance: 5 })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-1", // needed for distinctness
            opponentPlayerId: "player-77",
            outcome: "WON",
          }),
          duelRowBody({
            duelId: "duel-2", // needed for distinctness
            opponentPlayerId: "player-88",
            outcome: "LOST",
          }),
        ],
      }),
    );

    const state = await readProfileStrip({
      fetch: mock.fetch,
      storage,
    });

    expect(state.kind).toBe("profile");
    if (state.kind !== "profile") {
      throw new Error("Expected profile state");
    }

    const { container } = render(<ProfileStrip state={state} />);
    const screenContent = allContentOnScreen(container);

    // The opponent identifiers must not appear anywhere.
    expect(screenContent).not.toContain("player-77");
    expect(screenContent).not.toContain("player-88");

    // Sanity check: the scan found the outcome words, so it's working.
    expect(screenContent).toContain("Won");
    expect(screenContent).toContain("Lost");
  });

  it("states the balance the server sent, never the sum of the deltas", async () => {
    // Case 1: balance of 5 beside duels of +1 and −1 (sum = 0).
    // A client that added the deltas would print 0, not 5.
    writeDeviceId(storage, "d-1");
    const mock1 = answering(
      ok(meBody({ coinBalance: 5 })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-1", // needed for distinctness
            opponentPlayerId: "player-1",
          }),
          duelRowBody({
            duelId: "duel-2", // needed for distinctness
            opponentPlayerId: "player-2",
          }),
        ],
      }),
    );

    const state1 = await readProfileStrip({
      fetch: mock1.fetch,
      storage,
    });

    expect(state1.kind).toBe("profile");
    if (state1.kind !== "profile") {
      throw new Error("Expected profile state");
    }

    const { container: container1 } = render(<ProfileStrip state={state1} />);
    const content1 = allContentOnScreen(container1);

    // The balance is 5, not the sum of deltas (0).
    expect(content1).toContain("5");
    expect(content1).not.toContain("0 Duel coins");

    // Case 2: balance of −2 beside duels of +1, +1, −1 (sum = +1).
    // A client that added the deltas would print 1, not −2.
    storage = inMemoryStorage();
    writeDeviceId(storage, "d-2");
    const mock2 = answering(
      ok(meBody({ coinBalance: -2 })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-3", // needed for distinctness
            opponentPlayerId: "player-3",
          }),
          duelRowBody({
            duelId: "duel-4", // needed for distinctness
            opponentPlayerId: "player-4",
          }),
          duelRowBody({
            duelId: "duel-5", // needed for distinctness
            opponentPlayerId: "player-5",
          }),
        ],
      }),
    );

    const state2 = await readProfileStrip({
      fetch: mock2.fetch,
      storage,
    });

    expect(state2.kind).toBe("profile");
    if (state2.kind !== "profile") {
      throw new Error("Expected profile state");
    }

    const { container: container2 } = render(<ProfileStrip state={state2} />);
    const content2 = allContentOnScreen(container2);

    // The balance is −2 (shown as a minus sign + 2), not the sum of deltas (+1).
    // Use the minus sign U+2212 as in the display.
    expect(content2).toContain("−2");
    expect(content2).not.toContain("1 Duel coins");
  });
});
