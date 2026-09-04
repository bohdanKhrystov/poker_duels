/**
 * Whole seconds left until `deadlineMillis`, never below zero.
 *
 * `Math.ceil` so that the last whole second is shown for the whole of it and zero is reached
 * at the deadline rather than a second before it. Zero is not an event (`ADR-0108` §5): what
 * the player reads stops changing, until a server frame carries the consequence.
 */
export function secondsRemaining(
  deadlineMillis: number,
  nowMillis: number,
): number {
  return Math.max(0, Math.ceil((deadlineMillis - nowMillis) / 1000));
}
