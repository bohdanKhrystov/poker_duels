import type { TurnClockState } from "../store/duel-state";
import { secondsRemaining } from "./countdown";

/**
 * The clock's figure — seconds under a minute as bare numerals, minutes and up as `m:ss`.
 *
 * Copied from the merged card `design/components/seat-and-pot.html` (ADR-0024 §3).
 * The two shapes diverge below a minute — the clock drops the `0:` prefix — and coincide
 * at a minute and above where both use `m:ss` (ADR-0108 §5).
 *
 * @param seconds Whole seconds remaining, clamped to zero if negative.
 * @returns The figure to show — `24`, `6`, `2:47`, `0`.
 */
export function clockFigure(seconds: number): string {
  const clamped = Math.max(0, seconds);
  if (clamped < 60) {
    return String(clamped);
  }
  const minutes = Math.floor(clamped / 60);
  const secs = clamped % 60;
  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

/**
 * The bank's figure — always `m:ss`, even at zero.
 *
 * Copied from the merged card `design/components/seat-and-pot.html` (ADR-0024 §3).
 * Both seats' banks are public facts of the table (ADR-0108 §5), drawn in `m:ss` format
 * to allow the player to distinguish a spent clock (which reads as `0`) from an exhausted
 * bank (which reads as `0:00`).
 *
 * @param seconds Whole seconds remaining, clamped to zero if negative.
 * @returns The figure to show — `3:00`, `1:12`, `0:00`.
 */
export function bankFigure(seconds: number): string {
  const clamped = Math.max(0, seconds);
  const minutes = Math.floor(clamped / 60);
  const secs = clamped % 60;
  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

/** The four ways a countdown can be drawn — the treatments `design/components/seat-and-pot.html` names. */
export type ClockTreatment =
  "regular" | "running-out" | "on-timebank" | "expired";

/** The store's clock and the reading to draw it against — both, or neither. */
export interface ClockReading {
  readonly clock: TurnClockState;
  readonly nowMillis: number;
}

/** What one seat's plate draws for the turn clock: a figure, its treatment, and that seat's bank. */
export interface SeatClock {
  readonly figure: string | null;
  readonly treatment: ClockTreatment;
  readonly bank: string | null;
}

/**
 * The switch from `regular` to `running-out`, in seconds left on the fresh allowance.
 *
 * No merged source fixes the point; the merged card bounds it by drawing 24 seconds `regular`
 * and 6 `running-out`, so anything from 7 through 24 agrees with the drawing already accepted.
 * Ten is the round number inside those bounds (`ADR-0102` §4) — the pane's verdict overrules it
 * in one line (`ADR-0024` §3). Named once, here; nowhere else spreads the literal.
 */
export const RUNNING_OUT_SECONDS = 10;

/**
 * One seat's whole turn-clock picture: whether to draw a countdown, which of the card's four
 * treatments it is in, and that seat's bank — chosen from the two server-stated instants the
 * store already holds, anchored at the frame's arrival (`ADR-0113` §6). Nothing here is
 * decremented and no state is held; called afresh at every render against a fresh reading.
 *
 * @param reading The store's clock and the instant to read it against — `null` before the
 *   server has sent one.
 * @param seat The seat this call is drawing.
 * @param seatToAct The seat `PlayerView` currently names to act, or `null` if none.
 * @param handNumber The hand `PlayerView` currently shows.
 * @returns The figure, treatment and bank to draw at `seat`.
 */
export function seatClock(
  reading: ClockReading | null,
  seat: number,
  seatToAct: number | null,
  handNumber: number,
): SeatClock {
  if (reading === null) {
    return { figure: null, treatment: "regular", bank: null };
  }
  const { clock, nowMillis } = reading;

  // ADR-0113 §1: handNumber and the seat to act are the pair PlayerView can compare, so a clock
  // for a decision already closed draws nothing rather than a stale countdown.
  if (clock.handNumber !== handNumber || clock.seat !== seatToAct) {
    return { figure: null, treatment: "regular", bank: null };
  }

  if (seat !== clock.seat) {
    // ADR-0108 §4: a bank only spends while its seat is on turn, so a seat the clock does not
    // name reads its bank straight from the frame.
    return {
      figure: null,
      treatment: "regular",
      bank: bankFigure(clock.bankRemainingMillis[seat] / 1000),
    };
  }

  // ADR-0113 §3's own expression, applied to the client's anchored copy of the same two
  // instants: what is left of the bank at the reading, for every treatment below.
  const bank = bankFigure(
    secondsRemaining(clock.expiresAt, Math.max(nowMillis, clock.turnEndsAt)),
  );

  if (nowMillis >= clock.expiresAt) {
    return { figure: "0", treatment: "expired", bank };
  }
  if (nowMillis >= clock.turnEndsAt) {
    const figure = clockFigure(secondsRemaining(clock.expiresAt, nowMillis));
    return { figure, treatment: "on-timebank", bank };
  }

  const secondsLeft = secondsRemaining(clock.turnEndsAt, nowMillis);
  const treatment: ClockTreatment =
    secondsLeft <= RUNNING_OUT_SECONDS ? "running-out" : "regular";
  return { figure: clockFigure(secondsLeft), treatment, bank };
}
