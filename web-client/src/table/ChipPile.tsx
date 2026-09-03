import type { ReactElement } from "react";

/**
 * A pile of chips: three discs, always. The pile never grows or shrinks with
 * the amount, so its size states no count and implies no denomination — the
 * numeral beside it is the whole fact (ADR-0115 §1, §6). It arrives on mount
 * with the `chip-flight` animation and then stands still.
 */
export function ChipPile(): ReactElement {
  return (
    <span aria-hidden="true" className="chip-pile chip-flight">
      <span className="chip-disc" />
      <span className="chip-disc" />
      <span className="chip-disc" />
    </span>
  );
}
