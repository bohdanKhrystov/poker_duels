import { describe, it, expect } from "vitest";
import { clockFigure, bankFigure, seatClock, RUNNING_OUT_SECONDS } from "./turn-clock";
import type { ClockReading } from "./turn-clock";
import type { TurnClockState } from "../store/duel-state";

/** No test asserts against these defaults alone — every test overrides what it means to prove. */
function aClock(overrides: Partial<TurnClockState> = {}): TurnClockState {
  return {
    seat: 0,
    handNumber: 1,
    actionSequence: 0,
    turnEndsAt: 30_000,
    expiresAt: 30_000 + 180_000,
    bankRemainingMillis: [180_000, 180_000],
    ...overrides,
  };
}

describe("turn-clock", () => {
  it("writes the clock as bare seconds under a minute", () => {
    expect(clockFigure(24)).toBe("24");
    expect(clockFigure(6)).toBe("6");
  });

  it("writes the clock as m:ss from a minute up", () => {
    expect(clockFigure(167)).toBe("2:47");
    expect(clockFigure(72)).toBe("1:12");
  });

  it("switches shape at exactly a minute", () => {
    expect(clockFigure(59)).toBe("59");
    expect(clockFigure(60)).toBe("1:00");
  });

  it("writes a spent clock as a bare zero", () => {
    expect(clockFigure(0)).toBe("0");
  });

  it("writes the bank as m:ss, at every size", () => {
    expect(bankFigure(180)).toBe("3:00");
    expect(bankFigure(72)).toBe("1:12");
  });

  it("writes an empty bank as 0:00 and not as 0", () => {
    expect(bankFigure(0)).toBe("0:00");
  });

  it("pads the bank's and the clock's seconds to two figures", () => {
    expect(bankFigure(65)).toBe("1:05");
    expect(bankFigure(61)).toBe("1:01");
    expect(clockFigure(65)).toBe("1:05");
  });

  describe("seatClock", () => {
    it("draws no clock and no bank before the server has sent one", () => {
      const result = seatClock(null, 0, 0, 1);
      expect(result.figure).toBeNull();
      expect(result.bank).toBeNull();
    });

    it("draws the countdown only at the seat the clock names", () => {
      const clockAtSeat0 = aClock({ seat: 0 });
      const readingAtSeat0: ClockReading = { clock: clockAtSeat0, nowMillis: 0 };
      expect(seatClock(readingAtSeat0, 0, 0, 1).figure).not.toBeNull();
      expect(seatClock(readingAtSeat0, 1, 0, 1).figure).toBeNull();

      // The mirror: a hard-coded seat 0 in the implementation would still pass the block above.
      const clockAtSeat1 = aClock({ seat: 1 });
      const readingAtSeat1: ClockReading = { clock: clockAtSeat1, nowMillis: 0 };
      expect(seatClock(readingAtSeat1, 1, 1, 1).figure).not.toBeNull();
      expect(seatClock(readingAtSeat1, 0, 1, 1).figure).toBeNull();
    });

    it("draws nothing for a decision the view has moved past", () => {
      const clock = aClock({ seat: 0, handNumber: 4 });
      const reading: ClockReading = { clock, nowMillis: 0 };

      const wrongHand = seatClock(reading, 0, 0, 5);
      expect(wrongHand.figure).toBeNull();
      expect(wrongHand.bank).toBeNull();

      const wrongSeatToAct = seatClock(reading, 0, 1, 4);
      expect(wrongSeatToAct.figure).toBeNull();
      expect(wrongSeatToAct.bank).toBeNull();
    });

    it("is regular until the last ten seconds, and running out after", () => {
      const clock = aClock({ turnEndsAt: 30_000 });

      const justOutside = seatClock(
        { clock, nowMillis: clock.turnEndsAt - (RUNNING_OUT_SECONDS + 1) * 1000 },
        0,
        0,
        1,
      );
      expect(justOutside.treatment).toBe("regular");
      expect(justOutside.figure).toBe("11");

      const justInside = seatClock(
        { clock, nowMillis: clock.turnEndsAt - RUNNING_OUT_SECONDS * 1000 },
        0,
        0,
        1,
      );
      expect(justInside.treatment).toBe("running-out");
      expect(justInside.figure).toBe("10");
    });

    it("is on timebank once the allowance is spent, and expired once the bank is", () => {
      const clock = aClock({ turnEndsAt: 30_000, expiresAt: 30_000 + 180_000 });

      const onTimebank = seatClock({ clock, nowMillis: clock.turnEndsAt + 13_000 }, 0, 0, 1);
      expect(onTimebank.treatment).toBe("on-timebank");
      expect(onTimebank.figure).toBe("2:47");

      const expired = seatClock({ clock, nowMillis: clock.expiresAt + 5_000 }, 0, 0, 1);
      expect(expired.treatment).toBe("expired");
      expect(expired.figure).toBe("0");
    });

    it("reads the acting seat's bank down as the bank spends", () => {
      const clock = aClock({ turnEndsAt: 30_000, expiresAt: 30_000 + 180_000 });

      const insideAllowance = seatClock({ clock, nowMillis: 0 }, 0, 0, 1);
      expect(insideAllowance.bank).toBe("3:00");

      const onTimebank = seatClock({ clock, nowMillis: clock.turnEndsAt + 13_000 }, 0, 0, 1);
      expect(onTimebank.bank).toBe(onTimebank.figure);
      expect(onTimebank.bank).toBe("2:47");

      const pastExpiry = seatClock({ clock, nowMillis: clock.expiresAt + 5_000 }, 0, 0, 1);
      expect(pastExpiry.bank).toBe("0:00");
    });

    it("reads a seat that is not on turn from the frame's own number", () => {
      const clock = aClock({
        seat: 0,
        turnEndsAt: 30_000,
        expiresAt: 210_000,
        bankRemainingMillis: [180_000, 72_000],
      });
      const reading: ClockReading = { clock, nowMillis: 0 };

      expect(seatClock(reading, 1, 0, 1).bank).toBe("1:12");
      expect(seatClock(reading, 0, 0, 1).bank).toBe("3:00");
    });

    it("stays on timebank, never running-out, even seconds from expiring", () => {
      const clock = aClock({ turnEndsAt: 30_000, expiresAt: 30_000 + 180_000 });

      // Eight seconds left on the bank is inside RUNNING_OUT_SECONDS, but "running-out" names
      // only the fresh allowance (ADR-0113 §6) — the bank falling is its own treatment, so an
      // implementation that reused the same threshold on the bank would fail this.
      const result = seatClock({ clock, nowMillis: clock.expiresAt - 8_000 }, 0, 0, 1);
      expect(result.treatment).toBe("on-timebank");
      expect(result.figure).toBe("8");
    });
  });
});
