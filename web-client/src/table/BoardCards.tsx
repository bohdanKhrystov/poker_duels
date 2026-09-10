import type { ReactElement } from "react";
import { CardFace, CardSlot } from "./PlayingCard";
import { standingOf } from "./winning-cards";

const PLACES = [
  "first flop card",
  "second flop card",
  "third flop card",
  "turn card",
  "river card",
] as const;

/**
 * The five board places: a face for each card dealt, a dashed slot for each
 * not yet dealt. `marked`, when given, is the set of cards that took the pot,
 * and every face-up card is drawn in relation to it (`winning-cards.ts`).
 */
export function BoardCards(props: {
  cards: readonly string[];
  marked?: ReadonlySet<string>;
}): ReactElement {
  return (
    <div className="flex gap-3 [--w:clamp(48px,calc((100cqi-64px)/5),72px)]">
      {PLACES.map((place, index) => {
        const card = props.cards.at(index);
        return card === undefined ? (
          <CardSlot key={place} label={`${place}, not yet dealt`} />
        ) : (
          <CardFace
            key={place}
            card={card}
            standing={standingOf(card, props.marked)}
          />
        );
      })}
    </div>
  );
}
