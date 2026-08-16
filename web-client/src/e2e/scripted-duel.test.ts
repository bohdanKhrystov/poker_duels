import { describe, expect, it } from "vitest";

import { SERVER_MESSAGE_TYPES } from "../protocol/frames";
import type { DuelOutcome, YourTurn } from "../protocol/protocol.gen";

import { scriptedDuel } from "./scripted-duel";
import type {
  ClientStep,
  RivalHand,
  ScriptedSeat,
  ServerStep,
} from "./scripted-duel";

const duel = scriptedDuel();

function serverSteps(seat: ScriptedSeat): readonly ServerStep[] {
  return seat.steps.filter(
    (step): step is ServerStep => step.from === "server",
  );
}

function clientSteps(seat: ScriptedSeat): readonly ClientStep[] {
  return seat.steps.filter(
    (step): step is ClientStep => step.from === "client",
  );
}

function seatOf(viewerSeat: number): ScriptedSeat {
  const seat = duel.seats.find(
    (candidate) => candidate.viewerSeat === viewerSeat,
  );
  if (seat === undefined) {
    throw new Error(`no seat recorded with viewerSeat ${viewerSeat}`);
  }
  return seat;
}

function finishedOutcome(seat: ScriptedSeat): DuelOutcome {
  const last = seat.steps[seat.steps.length - 1];
  if (
    last === undefined ||
    last.from !== "server" ||
    last.message.type !== "DuelFinished"
  ) {
    throw new Error(
      `seat ${seat.viewerSeat} does not end on a DuelFinished server step`,
    );
  }
  return last.message.outcome;
}

function previousYourTurn(seat: ScriptedSeat, index: number): YourTurn {
  const previous = seat.steps[index - 1];
  if (
    previous === undefined ||
    previous.from !== "server" ||
    previous.message.type !== "YourTurn"
  ) {
    throw new Error(
      `seat ${seat.viewerSeat} step ${index} is a client step whose predecessor is not YourTurn`,
    );
  }
  return previous.message;
}

// The only place a seat's true hole cards appear is its *own* `Snapshot.view`:
// the projection hides them from the other seat until a `HandRevealed`. Reading
// them here, off the rival's own frames, is the ground truth `rivalHoleCards`
// is checked against below.
function ownHoleCardsByHand(seat: ScriptedSeat): readonly RivalHand[] {
  const byHand = new Map<number, readonly string[]>();

  for (const step of serverSteps(seat)) {
    if (step.message.type !== "Snapshot") {
      continue;
    }
    const view = step.message.view;
    const own = view.seats.find(
      (seatView) => seatView.index === seat.viewerSeat,
    );
    if (own === undefined) {
      throw new Error(
        `seat ${seat.viewerSeat} Snapshot carries no SeatView for its own seat`,
      );
    }
    byHand.set(view.handNumber, own.holeCards);
  }

  return [...byHand.entries()]
    .sort(([a], [b]) => a - b)
    .map(([handNumber, cards]) => ({ handNumber, cards }));
}

// `handNumber` -> the seats a `HandRevealed` named during that hand.
// `HandRevealed` itself carries no `handNumber` — the enclosing `HandStarted`,
// scanned in the order events actually arrived, is the only source of it.
function revealsByHand(
  seat: ScriptedSeat,
): ReadonlyMap<number, ReadonlySet<number>> {
  const byHand = new Map<number, Set<number>>();
  let current: Set<number> | null = null;

  for (const step of serverSteps(seat)) {
    if (step.message.type !== "Events") {
      continue;
    }
    for (const event of step.message.events) {
      if (event.type === "HandStarted") {
        current = new Set<number>();
        byHand.set(event.handNumber, current);
      } else if (event.type === "HandRevealed") {
        if (current === null) {
          throw new Error(
            `seat ${seat.viewerSeat} has a HandRevealed before any HandStarted`,
          );
        }
        current.add(event.seat);
      }
    }
  }

  return byHand;
}

describe("the committed duel script", () => {
  it("decodes every server frame it carries", () => {
    for (const seat of duel.seats) {
      const steps = serverSteps(seat);
      expect(steps.length).toBeGreaterThan(40);
      for (const step of steps) {
        expect(step.message).not.toBeNull();
        expect(SERVER_MESSAGE_TYPES).toContain(step.message.type);
      }
    }
  });

  it("carries one Act for every YourTurn, each answering that turn", () => {
    for (const seat of duel.seats) {
      const turnCount = serverSteps(seat).filter(
        (step) => step.message.type === "YourTurn",
      ).length;
      const acts = clientSteps(seat);

      expect(acts.length).toBe(turnCount);
      expect(acts.length).toBeGreaterThan(1);

      seat.steps.forEach((step, index) => {
        if (step.from !== "client") {
          return;
        }
        const turn = previousYourTurn(seat, index);
        expect(step.act.handNumber).toBe(turn.handNumber);
        expect(step.act.actionSequence).toBe(turn.actionSequence);
      });
    }
  });

  it("ends both seats on the same DuelFinished, with a winner", () => {
    const outcomes = duel.seats.map(finishedOutcome);

    for (const outcome of outcomes) {
      expect([0, 1]).toContain(outcome.winner);
      expect(outcome.handsPlayed).toBeGreaterThan(5);
    }
    expect(outcomes[1]).toEqual(outcomes[0]);
  });

  it("names the rival's two cards for every hand, from the rival's own frames", () => {
    for (const seat of duel.seats) {
      const handNumbers = seat.rivalHoleCards.map((hand) => hand.handNumber);
      expect(handNumbers).toEqual(
        Array.from({ length: seat.rivalHoleCards.length }, (_, i) => i + 1),
      );
      for (const hand of seat.rivalHoleCards) {
        expect(hand.cards).toHaveLength(2);
      }
    }

    // The cross-check: seat 0's claim about the rival's cards must match what
    // seat 1 recorded about itself, and the mirror for seat 1 — a script that
    // accidentally recorded one seat's session twice would fail this, since a
    // seat's own Snapshot never shows the *other* seat's hidden hole cards.
    const viewer0 = seatOf(0);
    const viewer1 = seatOf(1);

    expect(viewer0.rivalHoleCards).toEqual(ownHoleCardsByHand(viewer1));
    expect(viewer1.rivalHoleCards).toEqual(ownHoleCardsByHand(viewer0));
  });

  it("contains a showdown that revealed the rival, and a hand that revealed nobody", () => {
    for (const seat of duel.seats) {
      const rivalSeat = 1 - seat.viewerSeat;
      const byHand = revealsByHand(seat);

      const revealedRival = [...byHand.entries()]
        .filter(([, seats]) => seats.has(rivalSeat))
        .map(([handNumber]) => handNumber);
      const revealedNobody = [...byHand.entries()]
        .filter(([, seats]) => seats.size === 0)
        .map(([handNumber]) => handNumber);

      const message =
        `seat ${seat.viewerSeat}: revealed the rival in ${JSON.stringify(revealedRival)}, ` +
        `revealed nobody in ${JSON.stringify(revealedNobody)}`;

      expect(revealedRival.length, message).toBeGreaterThan(0);
      expect(revealedNobody.length, message).toBeGreaterThan(0);
      expect(
        revealedRival.filter((hand) => revealedNobody.includes(hand)),
        message,
      ).toEqual([]);
    }
  });
});
