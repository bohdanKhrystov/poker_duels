import type { ReactElement } from "react";

/**
 * The duel coin's mark. Steel, never gold: `docs/vision.md` says it counts
 * duels rather than glitters, and `--pd-coin-face` is the one place that face
 * is composed — the same lighting as `design/coin/duel-coin.svg`.
 *
 * Decorative on purpose. The line beside it already says what moved and by
 * how much, so a mark that named itself would make a screen reader say
 * "coin" twice.
 */
export function CoinMark(): ReactElement {
  return (
    <span
      aria-hidden="true"
      className="inline-block h-6 w-6 rounded-pill forced-colors:border"
      style={{ background: "var(--pd-coin-face)" }}
    />
  );
}
