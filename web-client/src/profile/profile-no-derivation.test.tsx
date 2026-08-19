import { readFileSync, readdirSync } from "node:fs";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { fireEvent, render, waitFor } from "@testing-library/react";
import { describe, it, expect, beforeEach } from "vitest";
import { readProfileStrip } from "./profile-strip";
import { ProfileStrip } from "./ProfileStrip";
import type { ApiFetch, ApiResponse } from "./api";
import type { RecentDuel } from "./recent-duels";
import { writeDeviceId } from "../protocol/device-id";
import { meBody, duelRowBody } from "./profile-fixture";
import { HistoryScreen } from "../history/HistoryScreen";

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

/**
 * Carried from `TASK-041114`. `name-text.ts`'s own doc comment claims to be
 * "the only place in the client that branches on a null display name"
 * (`ADR-0058` §1) — the fact `ADR-0052` §5's invisibility depends on: a
 * second surface computing its own word for "no name" could disagree with
 * `nameOrNone` and no test would notice. Path is relative to `src/`, the
 * same root `clientSources` below walks.
 */
const SOLE_DECISION_FILE = join("profile", "name-text.ts");

/**
 * A ternary that turns a null display name into a literal label, inline —
 * the shape `TASK-041115` found does *not* fail the merged tests for a
 * genuinely nameless player, because the DOM it produces matches
 * `nameOrNone`'s only for the named case. Deliberately narrow to that one
 * shape: an `if` that only type-guards a wire value (`profile.ts`) or only
 * gates whether a fixed notice renders (`NameSurface.tsx`) is not a second
 * surface deciding what a name prints as, and must not be flagged.
 */
const NULL_NAME_TERNARY =
  /[A-Za-z]*[Dd]isplay[Nn]ame\s*(?:===|==)\s*null\)?\s*\?/;

/** Every `.ts`/`.tsx` source file under `src/`, test files excluded, keyed by its path relative to `src/`. */
function clientSources(): Map<string, string> {
  const srcDir = resolve(dirname(fileURLToPath(import.meta.url)), "..");
  const entries = readdirSync(srcDir, { recursive: true }) as string[];

  const sourceEntries = entries.filter(
    (entry) =>
      (entry.endsWith(".ts") || entry.endsWith(".tsx")) &&
      !entry.includes(".test."),
  );

  const sources = new Map<string, string>();
  for (const entry of sourceEntries) {
    sources.set(entry, readFileSync(join(srcDir, entry), "utf-8"));
  }
  return sources;
}

/** Every file, other than `name-text.ts`, that decides a null-name label for itself. */
function secondDecisionPoints(sources: Map<string, string>): string[] {
  return [...sources.entries()]
    .filter(([file]) => file !== SOLE_DECISION_FILE)
    .filter(([, source]) => NULL_NAME_TERNARY.test(source))
    .map(([file]) => file);
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
        nextCursor: null,
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
        nextCursor: null,
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
        nextCursor: null,
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

  it("puts no player id on the screen, named opponent or nameless", async () => {
    // Carried from TASK-041114: assert that `nameOrNone` is the client's
    // sole decision point before checking anything about this render, so a
    // second surface printing its own word for "no name" fails here even if
    // it happens to agree with `nameOrNone` on the two rows below.
    const sources = clientSources();

    // A walk over a directory that was never found, or the wrong one,
    // returns no files, and "nothing else decides" would then be vacuously
    // true — this story has already shipped one guard that passed because
    // nothing was on screen. Prove the walk saw the real tree, and that the
    // pattern itself catches a real offender, before trusting what it found.
    expect(sources.size).toBeGreaterThan(30);
    expect([...sources.keys()]).toContain(SOLE_DECISION_FILE);
    const withPlant = new Map(sources).set(
      "planted.tsx",
      'const label = displayName === null ? "No name" : displayName;',
    );
    expect(secondDecisionPoints(withPlant)).toContain("planted.tsx");

    const violators = secondDecisionPoints(sources);
    expect(
      violators,
      `nameOrNone (${SOLE_DECISION_FILE}) must stay the client's only place that turns a null display name into a label; also found in: ${violators.join(", ")}`,
    ).toEqual([]);

    // Two rows whose bodies carry distinct, real opponentPlayerId values —
    // one named opponent, one nameless — so the scan below has both an id
    // and a null name to catch.
    writeDeviceId(storage, "d-1");
    const mock = answering(
      ok(meBody({ coinBalance: 5 })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-91", // needed for distinctness
            opponentPlayerId: "player-91",
            opponentDisplayName: "Ada",
          }),
          duelRowBody({
            duelId: "duel-92", // needed for distinctness
            opponentPlayerId: "player-92",
            opponentDisplayName: null,
          }),
        ],
        nextCursor: null,
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

    // Neither id appears anywhere: not as text, not in an aria-label or
    // title, and not in any other attribute value.
    expect(screenContent).not.toContain("player-91");
    expect(screenContent).not.toContain("player-92");

    // Sanity: the scan is looking, and both name treatments are on screen —
    // the real name for the named row, and `nameOrNone`'s own word for the
    // nameless one. Fails against a component that falls back to the id
    // when the name is null, the exact `Player-3F2A` shortcut `ADR-0029`
    // §6 forbids.
    expect(screenContent).toContain("Ada");
    expect(screenContent).toContain("No name");
  });

  it("renders a removed name and a name never set as the same pixels", async () => {
    // ADR-0052 §5: nobody but the player themselves may ever tell a
    // takedown from a name that was never set. `displayNameRemoved` is a
    // fact about the *caller's own* profile (`GET /api/me`) — a `RecentDuel`
    // never carries it for an opponent — so it is the one field that
    // legitimately differs between these two reads, and it must not reach
    // the markup of an opponent's nameless line, or anywhere else in the strip.
    writeDeviceId(storage, "d-1");
    const removedMock = answering(
      ok(meBody({ displayNameRemoved: true })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-201",
            opponentPlayerId: "player-201",
            opponentDisplayName: null,
          }),
        ],
        nextCursor: null,
      }),
    );
    const removedState = await readProfileStrip({
      fetch: removedMock.fetch,
      storage,
    });
    expect(removedState.kind).toBe("profile");
    if (removedState.kind !== "profile") {
      throw new Error("Expected profile state");
    }
    const { container: removedContainer } = render(
      <ProfileStrip state={removedState} />,
    );

    storage = inMemoryStorage();
    writeDeviceId(storage, "d-2");
    const neverSetMock = answering(
      ok(meBody({ displayNameRemoved: false })),
      ok({
        duels: [
          duelRowBody({
            duelId: "duel-202",
            opponentPlayerId: "player-202",
            opponentDisplayName: null,
          }),
        ],
        nextCursor: null,
      }),
    );
    const neverSetState = await readProfileStrip({
      fetch: neverSetMock.fetch,
      storage,
    });
    expect(neverSetState.kind).toBe("profile");
    if (neverSetState.kind !== "profile") {
      throw new Error("Expected profile state");
    }
    const { container: neverSetContainer } = render(
      <ProfileStrip state={neverSetState} />,
    );

    // Full markup, not a selective read: any badge, tooltip, class or word
    // marking a takedown anywhere in the strip would show up as a diff
    // here, and ADR-0052 §5 forbids all four. It is a criterion rather than
    // an omission because the wire cannot express the difference — a
    // `RecentDuel` has no field for it — and no screen may invent one.
    expect(removedContainer.innerHTML).toBe(neverSetContainer.innerHTML);
  });
});

describe("the history screen's surface", () => {
  it("puts no player id on the history screen, named opponent or nameless", async () => {
    // Prove the walk reaches the new directory before trusting what it does
    // (or does not) find in it: `clientSources` walks the whole `src/` tree
    // from this one place, and `history/` must show up in the result rather
    // than be assumed to.
    const sources = clientSources();
    expect(sources.size).toBeGreaterThan(30);
    const historyScreenPath = join("history", "HistoryScreen.tsx");
    expect([...sources.keys()]).toContain(historyScreenPath);

    // Two rows whose bodies carry distinct, real opponentPlayerId values —
    // one named opponent, one nameless — so the scan below has both an id
    // and a null name to catch.
    const rows = [
      duelRowBody({
        duelId: "duel-81",
        opponentPlayerId: "player-81",
        opponentDisplayName: "Ada",
      }),
      duelRowBody({
        duelId: "duel-82",
        opponentPlayerId: "player-82",
        opponentDisplayName: null,
      }),
    ] as unknown as RecentDuel[];

    const { container } = render(
      <HistoryScreen
        read={async () => ({
          kind: "page",
          duels: rows,
          nextCursor: null,
          restarted: false,
        })}
      />,
    );

    // Prove the render is not empty before trusting a scan that found
    // nothing on it: both rows landed, and both name treatments are on
    // screen — the real name for the named row, and `nameOrNone`'s own word
    // for the nameless one.
    await waitFor(() => {
      expect(container.querySelectorAll("li").length).toBe(2);
    });
    const screenContent = allContentOnScreen(container);
    expect(screenContent).toContain("Ada");
    expect(screenContent).toContain("No name");

    // Neither id appears anywhere: not as text, not in an aria-label or
    // title, and not in any other attribute value. Fails against a row that
    // falls back to an id for a nameless opponent.
    expect(screenContent).not.toContain("player-81");
    expect(screenContent).not.toContain("player-82");
  });

  it("sends no player id to the server across a whole walk", async () => {
    const paths: string[] = [];
    let calls = 0;

    const { container } = render(
      <HistoryScreen
        read={async (query) => {
          calls += 1;
          paths.push(JSON.stringify(query));
          const row = duelRowBody({
            duelId: `duel-30${calls}`,
            opponentPlayerId: `player-30${calls}`,
          }) as unknown as RecentDuel;
          return {
            kind: "page",
            duels: [row],
            nextCursor: calls === 1 ? "cursor-1" : null,
            restarted: false,
          };
        }}
      />,
    );

    const moreButton = (): HTMLElement | undefined =>
      [...container.querySelectorAll("button")].find(
        (button) => button.textContent === "Show more",
      );

    // First page landed, and asked the server for a second.
    await waitFor(() => {
      expect(moreButton()).toBeTruthy();
    });
    fireEvent.click(moreButton() as HTMLElement);
    await waitFor(() => {
      expect(paths.length).toBeGreaterThan(1);
    });

    // Then narrows to an outcome. The radios render All, Won, Lost, Drew,
    // in that fixed order, so the second one is Won.
    const wonRadio = [
      ...container.querySelectorAll('input[type="radio"]'),
    ][1] as HTMLInputElement;
    fireEvent.click(wonRadio);

    // Three requests — first page, next page, narrowed page — so a walk
    // that recorded nothing cannot pass by saying nothing.
    await waitFor(() => {
      expect(paths.length).toBeGreaterThan(2);
    });

    // Every request handed to `read` carries no player id. Fails against a
    // client that correlates on an id it was told to drop — `TASK-041105`
    // drops it at the parse, and this is what keeps that cheap to keep true.
    for (const path of paths) {
      expect(path).not.toContain("player-");
    }
  });
});
