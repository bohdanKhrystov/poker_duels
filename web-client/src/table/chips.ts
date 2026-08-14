/**
 * A chip amount as the table writes it: digits grouped in threes.
 *
 * Deliberately not `toLocaleString` — the grouping must not change with the
 * browser's locale, because the design's mono, tabular-figure numbers are drawn
 * one way and a test asserting `"13,400"` must not depend on where it runs.
 */
export function formatChips(amount: number): string {
  return String(amount).replace(/\B(?=(\d{3})+$)/g, ",");
}
