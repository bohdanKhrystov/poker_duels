import type { Rejection } from "../protocol";
import { rejectionText } from "./rejection-text";

/**
 * What reading a typed entry against this turn's own bounds comes to: a
 * sendable total, or a refusal already worded the way the server itself
 * would word it.
 *
 * A discriminated union, so a caller cannot reach `to` without first handling
 * the refusal — there is no third, silent outcome.
 */
export type TypedAmount =
  | { readonly kind: "amount"; readonly to: number }
  | { readonly kind: "refused"; readonly sentence: string };

/**
 * The sentence for an entry with nothing in it to compare against a bound at
 * all, by name (`ADR-0111` §3) — never a rule explained, never a blame named.
 */
const NOT_AN_AMOUNT = "That is not an amount.";

/** A bare run of ASCII digits: `1200`, `0`. `\d` is `[0-9]`, so a non-ASCII digit never matches. */
const PLAIN_DIGITS = /^\d+$/;

/** The grouping `formatChips` itself prints: `1,200`, `13,400` — never a grouping the table would not print. */
const GROUPED_DIGITS = /^\d{1,3}(?:,\d{3})+$/;

/**
 * Reads a typed entry the way `ADR-0111` §4 requires: a reading, never a
 * rule. `floor` and `ceiling` are literal fields off this turn's own
 * `LegalActions` — the bar's merged `amountFloor` and `allInTo`, unchanged —
 * and this function adds nothing to them, subtracts nothing from them, and
 * never rounds, steps or otherwise rewrites the entry toward either one: the
 * only numbers a caller can ever get back are the one the player typed and
 * the two the server sent.
 *
 * The reading goes in order. First the entry is trimmed — trimming reads the
 * same entry, it does not rewrite it. Unless what remains is a plain digit
 * run or the table's own grouping, the entry is refused as {@link
 * NOT_AN_AMOUNT} before it is ever converted to a number, because a wrong
 * reading here costs chips while a wrong refusal only costs a retype
 * (`ADR-0111` §3) — this is also why `Number` is only ever applied to a
 * string this function has already shape-checked with a regular expression,
 * never to the entry itself: a prefix, an exponent or a hex spelling would
 * read a number nobody typed.
 *
 * Only once the entry is a number is it compared, and both ends are
 * inclusive: exactly `floor` and exactly `ceiling` are sendable. Below
 * `floor` and above `ceiling` are refused with the violated bound quoted, in
 * the sentence `rejection-text.ts` already merges for the server's own
 * `AmountTooSmall`/`AmountTooLarge` refusals. That `Rejection` is built right
 * here, locally, as an argument to `rejectionText` — a formatting value only,
 * never sent, never stored, never a frame of its own — so the two voices
 * cannot drift apart even in principle (`ADR-0111` §§2, 4).
 */
export function readTypedAmount(
  entry: string,
  floor: number,
  ceiling: number,
): TypedAmount {
  const trimmed = entry.trim();
  if (!PLAIN_DIGITS.test(trimmed) && !GROUPED_DIGITS.test(trimmed)) {
    return { kind: "refused", sentence: NOT_AN_AMOUNT };
  }

  const to = Number(trimmed.replace(/,/g, ""));

  if (to < floor) {
    const rejection: Rejection = {
      type: "AmountTooSmall",
      attempted: to,
      minimum: floor,
    };
    return { kind: "refused", sentence: rejectionText(rejection) };
  }

  if (to > ceiling) {
    const rejection: Rejection = {
      type: "AmountTooLarge",
      attempted: to,
      maximum: ceiling,
    };
    return { kind: "refused", sentence: rejectionText(rejection) };
  }

  return { kind: "amount", to };
}
