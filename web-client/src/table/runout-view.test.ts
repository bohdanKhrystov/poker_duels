import { describe, expect, it } from "vitest";
import type { GameEvent } from "../protocol";
import { runoutView } from "./runout-view";
import { aSeat, aView } from "./view-fixture";

const started: GameEvent = {
  type: "HandStarted",
  sequence: 1,
  handNumber: 7,
  buttonSeat: 0,
  smallBlind: 50,
  bigBlind: 100,
  stacks: [10_000, 10_000],
};

describe("the runout view", () => {
  it("puts each seat's award back in the pot and takes it out of the stack", () => {
    const view = aView({
      handNumber: 7,
      street: "COMPLETE",
      pot: 0,
      viewerSeat: 0,
      seats: [
        aSeat({ index: 0, stack: 20_000, committedThisStreet: 0 }),
        aSeat({ index: 1, stack: 0, committedThisStreet: 0 }),
      ],
    });
    const before = runoutView(view, [
      started,
      { type: "PotAwarded", sequence: 9, seat: 0, amount: 19_800, hand: [] },
    ]);

    expect(before.pot).toBe(19_800);
    expect(before.seats[0].stack).toBe(200);
    expect(before.seats[1].stack).toBe(0);
  });

  it("sums a split pot's two awards and gives each seat back its own share", () => {
    const view = aView({
      handNumber: 7,
      street: "COMPLETE",
      pot: 0,
      seats: [
        aSeat({ index: 0, stack: 10_050 }),
        aSeat({ index: 1, stack: 9_950 }),
      ],
    });
    const before = runoutView(view, [
      started,
      { type: "PotAwarded", sequence: 9, seat: 0, amount: 1_000, hand: [] },
      { type: "PotAwarded", sequence: 10, seat: 1, amount: 1_000, hand: [] },
    ]);

    expect(before.pot).toBe(2_000);
    expect(before.seats[0].stack).toBe(9_050);
    expect(before.seats[1].stack).toBe(8_950);
  });

  it("clears every street commitment, since the chips are in the middle", () => {
    const view = aView({
      handNumber: 7,
      seats: [
        aSeat({ index: 0, committedThisStreet: 300 }),
        aSeat({ index: 1, committedThisStreet: 300 }),
      ],
    });
    const before = runoutView(view, [
      started,
      { type: "PotAwarded", sequence: 9, seat: 1, amount: 600, hand: [] },
    ]);

    expect(before.seats.map((seat) => seat.committedThisStreet)).toEqual([
      0, 0,
    ]);
  });

  it("leaves both hands as the snapshot shows them — a runout turns them face up first", () => {
    const view = aView({
      handNumber: 7,
      viewerSeat: 1,
      seats: [
        aSeat({ index: 0, holeCards: ["As", "Kd"] }),
        aSeat({ index: 1, holeCards: ["7h", "2c"] }),
      ],
    });
    const before = runoutView(view, [
      started,
      { type: "PotAwarded", sequence: 9, seat: 0, amount: 600, hand: [] },
    ]);

    expect(before.seats[0].holeCards).toEqual(["As", "Kd"]);
    expect(before.seats[1].holeCards).toEqual(["7h", "2c"]);
  });

  it("returns the snapshot untouched when this client holds no award for the hand", () => {
    const view = aView({ handNumber: 7 });

    expect(runoutView(view, [])).toBe(view);
    expect(
      runoutView(view, [
        { ...started, handNumber: 6 },
        { type: "PotAwarded", sequence: 9, seat: 0, amount: 600, hand: [] },
      ]),
    ).toBe(view);
  });

  it("reads only the awards of the hand the view names", () => {
    const view = aView({
      handNumber: 8,
      pot: 0,
      seats: [aSeat({ index: 0, stack: 500 }), aSeat({ index: 1, stack: 500 })],
    });
    const before = runoutView(view, [
      started,
      { type: "PotAwarded", sequence: 9, seat: 0, amount: 9_999, hand: [] },
      { ...started, sequence: 10, handNumber: 8 },
      { type: "PotAwarded", sequence: 11, seat: 1, amount: 100, hand: [] },
    ]);

    expect(before.pot).toBe(100);
    expect(before.seats[0].stack).toBe(500);
    expect(before.seats[1].stack).toBe(400);
  });
});
