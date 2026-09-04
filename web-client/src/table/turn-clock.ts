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
