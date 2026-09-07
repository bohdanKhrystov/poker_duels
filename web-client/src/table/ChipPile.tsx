import type { ReactElement } from "react";

const FLIGHT: Record<"above" | "below", string> = {
  above: "chip-flight-down",
  below: "chip-flight-up",
};

/**
 * A pile of chips: three discs, always. The pile never grows or shrinks with
 * the amount, so its size states no count and implies no denomination — the
 * numeral beside it is the whole fact (ADR-0115 §1, §6). It arrives on mount
 * from the side named by `from` — the flight class is looked up in
 * {@link FLIGHT} — and then stands still. Defaults to `"above"`, the
 * direction chips already travelled before a side existed.
 */
export function ChipPile(props: { from?: "above" | "below" }): ReactElement {
  return (
    <span
      aria-hidden="true"
      className={`chip-pile ${FLIGHT[props.from ?? "above"]}`}
    >
      <span className="chip-disc" />
      <span className="chip-disc" />
      <span className="chip-disc" />
    </span>
  );
}
