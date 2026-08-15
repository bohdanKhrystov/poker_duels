import type { ReactElement } from "react";
import { cardText } from "./card-text";

// Every card is drawn from the `--w` reference width its row sets, exactly as
// design/screens/duel-table.html does: the radius and both glyph sizes are
// fractions of it, so one inherited property sizes a whole row of cards.
const SHELL =
  "aspect-[5/7] w-[var(--w)] shrink-0 rounded-[calc(var(--w)*0.0625)]";

/** A face-up card: the rank character, the suit glyph, and the suit's colour. */
export function CardFace(props: { card: string }): ReactElement {
  const text = cardText(props.card);
  if (text === null) {
    // A card the client cannot read is not one it may guess at — and it still
    // holds its place, so an unreadable card never reflows the row.
    return <CardBack label="an unreadable card" />;
  }
  return (
    <span
      role="img"
      aria-label={text.label}
      className={`${SHELL} relative bg-card-face shadow-[var(--pd-shadow-card)] ${
        text.isRed ? "text-suit-red" : "text-suit-black"
      }`}
    >
      <span
        aria-hidden="true"
        className="absolute top-[calc(var(--w)*0.07)] left-[calc(var(--w)*0.09)] leading-[1] font-bold"
      >
        <span className="block text-[calc(var(--w)*0.24)]">{text.rank}</span>
        <span className="mt-[calc(var(--w)*0.02)] block text-[calc(var(--w)*0.2)]">
          {text.suit}
        </span>
      </span>
      <span
        aria-hidden="true"
        className="absolute right-[calc(var(--w)*0.09)] bottom-[calc(var(--w)*0.07)] text-[calc(var(--w)*0.34)] leading-[1]"
      >
        {text.suit}
      </span>
    </span>
  );
}

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
