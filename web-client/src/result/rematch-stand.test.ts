import { describe, it, expect } from "vitest";
import { rematchStand } from "./rematch-stand";

describe("whose rematch offer it is", () => {
  it("reads an offer from your own seat as yours", () => {
    expect(rematchStand([1], 1)).toEqual({ mine: true, theirs: false });
  });

  it("reads the very same offer from the other side as your rivals", () => {
    expect(rematchStand([1], 0)).toEqual({ mine: false, theirs: true });
  });

  it("reads an offer from seat zero the same way round", () => {
    expect(rematchStand([0], 0)).toEqual({ mine: true, theirs: false });
    expect(rematchStand([0], 1)).toEqual({ mine: false, theirs: true });
  });

  it("reads an offer from each seat as one apiece", () => {
    expect(rematchStand([0, 1], 0)).toEqual({ mine: true, theirs: true });
    expect(rematchStand([0, 1], 1)).toEqual({ mine: true, theirs: true });
  });

  it("claims neither offer for a client that holds no seat", () => {
    expect(rematchStand([0, 1], null)).toEqual({ mine: false, theirs: false });
  });

  it("claims nothing before anyone has offered", () => {
    expect(rematchStand([], 0)).toEqual({ mine: false, theirs: false });
    expect(rematchStand([], 1)).toEqual({ mine: false, theirs: false });
  });
});
