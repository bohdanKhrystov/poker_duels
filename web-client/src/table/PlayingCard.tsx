import type { ReactElement } from "react";

// Every card is drawn from the `--w` reference width its row sets, exactly as
// design/screens/duel-table.html does: the radius and both glyph sizes are
// fractions of it, so one inherited property sizes a whole row of cards.
const SHELL =
  "aspect-[5/7] w-[var(--w)] shrink-0 rounded-[calc(var(--w)*0.0625)]";

/**
 * A face-down card. It carries no rank and no suit at all — not in its text,
 * not in an attribute — because an empty `holeCards` means "not entitled to
 * see", and a placeholder that knew the card would be the leak itself.
 */
export function CardBack(props: { label?: string | null }): ReactElement {
  const label = props.label ?? null;
  return (
    <span
      {...(label === null
        ? { "aria-hidden": true }
        : { role: "img", "aria-label": label })}
      className={`${SHELL} bg-card-back [background-image:var(--pd-card-back-stripes)] shadow-[var(--pd-shadow-card)] forced-colors:border forced-colors:border-[CanvasText]`}
    />
  );
}

/** A board place the server has not dealt: the design's dashed outline. */
export function CardSlot(props: { label: string }): ReactElement {
  return (
    <span
      role="img"
      aria-label={props.label}
      className={`${SHELL} border border-dashed border-hairline`}
    />
  );
}
