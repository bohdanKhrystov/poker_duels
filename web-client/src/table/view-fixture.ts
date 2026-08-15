import type { PlayerView, SeatView } from "../protocol";

/**
 * A `SeatView` carrying every field the wire declares, for a test to bend.
 *
 * A hand-written literal that misses a field is a `tsc` error the test runner
 * never sees, so the fixture is written once — in the tradition of the protocol
 * module's `FakeSocket`, which is also source a test drives rather than a test.
 */
export function aSeat(overrides: Partial<SeatView> = {}): SeatView {
  return {
    index: 0,
    stack: 500,
    committedThisStreet: 0,
    committedThisHand: 0,
    hasFolded: false,
    isAllIn: false,
    holeCards: [],
    ...overrides,
  };
}

/** A `PlayerView` carrying every field the wire declares, for a test to bend. */
export function aView(overrides: Partial<PlayerView> = {}): PlayerView {
  return {
    viewerSeat: 0,
    handNumber: 1,
    buttonSeat: 0,
    street: "PREFLOP",
    board: { cards: [] },
    pot: 30,
    betToMatch: 20,
    minRaiseTo: 40,
    seatToAct: 0,
    smallBlind: 10,
    bigBlind: 20,
    seats: [aSeat({ index: 0 }), aSeat({ index: 1 })],
    ...overrides,
  };
}
