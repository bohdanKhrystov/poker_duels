import { act, render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { openConnection } from "../protocol";
import { FakeSocket } from "../protocol/fake-socket";
import type { Snapshot } from "../protocol/protocol.gen";
import { bootDuelClient } from "../store/boot";
import { DuelProvider } from "../store/duel-provider";
import { Lobby } from "../lobby/Lobby";
import { cardText } from "../table/card-text";
import { driveScriptedDuel } from "./drive-duel";
import { scriptedDuel } from "./scripted-duel";
import type { ScriptedSeat } from "./scripted-duel";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage` — the
 * identical rationale as `drive-duel.tsx`'s own copy. Not imported from there:
 * that copy is not exported, and exporting it purely for one test file here
 * would be the production change this ticket does not make.
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

/**
 * Every card in `cards` that `container` currently names, by any of three
 * independent routes. The third test below plants against each route on its
 * own, so a route this function stopped checking would go uncaught by every
 * other test in this file — that plant is what makes this function itself
 * trustworthy, not just the claim it is used to prove.
 */
function cardsShown(
  container: HTMLElement,
  cards: readonly string[],
): string[] {
  // Route 1: a card element proper — the one a sighted player sees.
  const cardImageLabels = new Set(
    [...container.querySelectorAll('[role="img"]')]
      .map((element) => element.getAttribute("aria-label"))
      .filter((value): value is string => value !== null),
  );

  // Route 2: the same words spoken or printed outside a card element — a
  // screen reader reads an aria-label or a title exactly as it reads a card's,
  // and a text node is read the same way no matter what element holds it.
  // `role="img"` elements are excluded from this route on purpose: route 1
  // already owns them, so narrowing this function to route 1 alone (see the
  // PR's proof) still misses a plant made through this route, and nothing
  // here is ever caught twice.
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  const textParts: string[] = [];
  for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
    textParts.push(node.textContent ?? "");
  }
  const namedOutsideCards = [
    ...container.querySelectorAll("[aria-label], [title]"),
  ]
    .filter((element) => element.getAttribute("role") !== "img")
    .flatMap((element) => [
      element.getAttribute("aria-label"),
      element.getAttribute("title"),
    ])
    .filter((value): value is string => value !== null);
  const named = [...textParts, ...namedOutsideCards].join(" ");

  return cards.filter((card) => {
    const text = cardText(card);
    if (text !== null && cardImageLabels.has(text.label)) {
      return true;
    }
    if (text !== null && new RegExp(`\\b${text.label}\\b`, "i").test(named)) {
      return true;
    }
    // Route 3: the raw wire string, case-sensitive — belt and braces for a
    // leak that printed "Ah" without ever going through `cardText`.
    return new RegExp(`\\b${card}\\b`).test(named);
  });
}

/** One step's judgement, built only from the frames the viewer actually received. */
interface JudgedStep {
  readonly index: number;
  readonly handNumber: number;
  /** Whether an `Events` frame this hand has named the rival in a `HandRevealed`, as of this step. */
  readonly revealed: boolean;
  /** The step index at which `revealed` first turned true this hand, or `null` if it has not yet. */
  readonly revealedAtIndex: number | null;
  readonly shown: readonly string[];
}

/**
 * Replays `seat`'s whole half of the committed script through the real
 * client, judging every step from the moment a `Snapshot` first establishes a
 * hand. The live hand number and the reveal flag are read only from frames
 * this seat actually received: the hand resets on a `Snapshot` whose
 * `view.handNumber` changed, and the reveal flag is set from the step being
 * replayed before that step's DOM is judged, because the server sends
 * `Events` then `Snapshot` and the reveal is legitimate from the `Events`
 * frame onwards.
 */
function judgeRivalCards(seat: ScriptedSeat): readonly JudgedStep[] {
  const rivalSeat = 1 - seat.viewerSeat;
  let handNumber: number | null = null;
  let revealed = false;
  let revealedAtIndex: number | null = null;
  const judged: JudgedStep[] = [];

  driveScriptedDuel({
    viewerSeat: seat.viewerSeat,
    onStep: (step, index, container) => {
      if (step.from === "server") {
        if (step.message.type === "Snapshot") {
          if (step.message.view.handNumber !== handNumber) {
            handNumber = step.message.view.handNumber;
            revealed = false;
            revealedAtIndex = null;
          }
        } else if (step.message.type === "Events") {
          const rivalRevealedHere = step.message.events.some(
            (event) =>
              event.type === "HandRevealed" && event.seat === rivalSeat,
          );
          if (rivalRevealedHere && !revealed) {
            revealed = true;
            revealedAtIndex = index;
          }
        }
      }

      if (handNumber === null) {
        return;
      }

      const hand = seat.rivalHoleCards.find(
        (candidate) => candidate.handNumber === handNumber,
      );
      if (hand === undefined) {
        throw new Error(
          `duel-secrecy: seat ${seat.viewerSeat} step ${index} is in hand ` +
            `${handNumber}, which rivalHoleCards does not carry`,
        );
      }

      judged.push({
        index,
        handNumber,
        revealed,
        revealedAtIndex,
        shown: cardsShown(container, hand.cards),
      });
    },
  });

  return judged;
}

/**
 * Turns one raw wire `Snapshot` frame into one that carries `cards` as the
 * hole cards of `rivalSeat` — the fourth plant's doctoring, kept to exactly
 * what it claims: decode, copy the seat, re-encode.
 */
function doctorRivalHoleCards(
  frame: string,
  rivalSeat: number,
  cards: readonly string[],
): string {
  const message = JSON.parse(frame) as Snapshot;
  const seats = message.view.seats.map((seatView) =>
    seatView.index === rivalSeat ? { ...seatView, holeCards: cards } : seatView,
  );
  const doctored: Snapshot = { ...message, view: { ...message.view, seats } };
  return JSON.stringify(doctored);
}

/**
 * `driveScriptedDuel`'s own wiring (`bootDuelClient` over a `FakeSocket`,
 * `Lobby` mounted through `DuelProvider`), echoed here because that function
 * has no hook to alter a frame before `socket.receive` sees it — adding one
 * there would be the production change this ticket does not make. Replays
 * only a server-only prefix of `viewerSeat`'s steps, up to and including
 * `doctorAt`, doctoring that one frame with `doctor` first.
 */
function driveDoctoredPrefix(
  viewerSeat: number,
  doctorAt: number,
  doctor: (frame: string) => string,
): HTMLElement {
  const duel = scriptedDuel();
  const seat = duel.seats.find(
    (candidate) => candidate.viewerSeat === viewerSeat,
  );
  if (seat === undefined) {
    throw new Error(`duel-secrecy: the script carries no seat ${viewerSeat}`);
  }

  const storage = inMemoryStorage();
  const socket = new FakeSocket();
  const client = bootDuelClient({
    connect: (onMessage) =>
      openConnection({ socket: socket.asWebSocket(), storage, onMessage }),
    joinRoomCode: duel.roomCode,
    storage,
  });

  const { container } = render(
    <DuelProvider store={client.store} send={client.send}>
      <Lobby />
    </DuelProvider>,
  );

  act(() => {
    socket.open();
  });

  for (let index = 0; index <= doctorAt; index += 1) {
    const step = seat.steps[index];
    if (step.from !== "server") {
      throw new Error(
        `duel-secrecy: step ${index} is a client step; driveDoctoredPrefix ` +
          "only replays a server-only prefix",
      );
    }
    const frame = index === doctorAt ? doctor(step.frame) : step.frame;
    act(() => {
      socket.receive(frame);
    });
  }

  return container;
}

describe("the rival's cards", () => {
  it("never reach the screen before the frame that revealed them", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const judged = judgeRivalCards(seat);
      const unrevealed = judged.filter((step) => !step.revealed);

      for (const step of unrevealed) {
        expect(
          step.shown,
          `seat ${seat.viewerSeat} step ${step.index} hand ${step.handNumber}: ` +
            `rival cards on screen before the reveal: ${step.shown.join(", ")}`,
        ).toEqual([]);
      }

      // A walk that never established a hand, or that treated every step as
      // already revealed, would satisfy the loop above by never running it.
      expect(
        unrevealed.length,
        `seat ${seat.viewerSeat}: only ${unrevealed.length} steps were judged`,
      ).toBeGreaterThan(40);
    }
  });

  it("do reach it once the reveal has arrived", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const judged = judgeRivalCards(seat);
      const bothShown = judged.filter((step) => step.shown.length === 2);

      // Without this, the test above would pass on a client that rendered no
      // cards at all.
      expect(
        bothShown.length,
        `seat ${seat.viewerSeat}: the rival's cards were never both shown on screen`,
      ).toBeGreaterThan(0);

      for (const step of bothShown) {
        expect(
          step.revealedAtIndex,
          `seat ${seat.viewerSeat} step ${step.index} hand ${step.handNumber}: ` +
            `both cards (${step.shown.join(", ")}) shown with no reveal recorded this hand`,
        ).not.toBeNull();
        expect(
          step.index,
          `seat ${seat.viewerSeat} step ${step.index} hand ${step.handNumber}: ` +
            `both cards (${step.shown.join(", ")}) shown before the reveal at step ${step.revealedAtIndex}`,
        ).toBeGreaterThanOrEqual(step.revealedAtIndex as number);
      }
    }
  });

  it("are found when a frame is doctored to carry them, and when the DOM is", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const rivalSeat = 1 - seat.viewerSeat;
      // Hand 1 never reaches a showdown for either seat (both fold or fold to
      // an all-in — see `scripted-duel.test.ts`'s "revealed nobody" guard), so
      // it stays pre-reveal for the whole run: every plant below is honest.
      const firstHand = seat.rivalHoleCards[0];
      if (firstHand === undefined) {
        throw new Error(
          `duel-secrecy: seat ${seat.viewerSeat} carries no rivalHoleCards`,
        );
      }
      const [cardA, cardB] = firstHand.cards;
      const labelA = cardText(cardA);
      if (labelA === null || cardB === undefined) {
        throw new Error(
          `duel-secrecy: seat ${seat.viewerSeat} hand 1's rival cards ` +
            `(${firstHand.cards.join(", ")}) are not two readable cards`,
        );
      }

      let planted = false;
      driveScriptedDuel({
        viewerSeat: seat.viewerSeat,
        onStep: (step, index, container) => {
          if (
            planted ||
            step.from !== "server" ||
            step.message.type !== "Snapshot"
          ) {
            return;
          }
          planted = true;

          // Plant 1 (route 1): a role="img" element carrying the spoken name.
          const image = document.createElement("span");
          image.setAttribute("role", "img");
          image.setAttribute("aria-label", labelA.label);
          container.appendChild(image);
          expect(
            cardsShown(container, [cardA]),
            `seat ${seat.viewerSeat} step ${index}: role="img" plant of "${labelA.label}" was not found`,
          ).toEqual([cardA]);
          container.removeChild(image);

          // Plant 2 (route 2): the same name as a plain text node, no card element.
          const text = document.createTextNode(labelA.label);
          container.appendChild(text);
          expect(
            cardsShown(container, [cardA]),
            `seat ${seat.viewerSeat} step ${index}: plain-text plant of "${labelA.label}" was not found`,
          ).toEqual([cardA]);
          container.removeChild(text);

          // Plant 3 (route 3): the raw wire string, unprocessed by `cardText`.
          const raw = document.createTextNode(cardB);
          container.appendChild(raw);
          expect(
            cardsShown(container, [cardB]),
            `seat ${seat.viewerSeat} step ${index}: raw-string plant "${cardB}" was not found`,
          ).toEqual([cardB]);
          container.removeChild(raw);
        },
      });
      expect(
        planted,
        `seat ${seat.viewerSeat}: no pre-reveal Snapshot ever arrived`,
      ).toBe(true);

      // Plant 4: the real one — a wire frame doctored before `socket.receive`,
      // not the DOM. Decoder, store, table and sweep all run for real, and
      // the run fails the first test's rule: a card shown while unrevealed.
      const snapshotIndex = seat.steps.findIndex(
        (step) => step.from === "server" && step.message.type === "Snapshot",
      );
      expect(snapshotIndex).toBeGreaterThanOrEqual(0);

      const doctoredContainer = driveDoctoredPrefix(
        seat.viewerSeat,
        snapshotIndex,
        (frame) => doctorRivalHoleCards(frame, rivalSeat, firstHand.cards),
      );

      expect(
        cardsShown(doctoredContainer, firstHand.cards),
        `seat ${seat.viewerSeat} step ${snapshotIndex} hand ${firstHand.handNumber}: ` +
          `doctored Snapshot did not surface ${firstHand.cards.join(", ")}`,
      ).toEqual(firstHand.cards);
    }
  });
});
