import type { ReactElement } from "react";
import { CardFace, CardSlot } from "./PlayingCard";

// Five places, named by where they sit and not by the street: a place is what
// the row reserves, and which street the hand is on is `view.street`'s to say.
const PLACES = [
  "first flop card",
  "second flop card",
  "third flop card",
  "turn card",
  "river card",
] as const;

/**
 * The community cards: five places, every one of them always drawn.
 *
 * A place the server has dealt shows its card; a place it has not shows the
 * design's dashed outline, so the row's width never changes between streets.
 */
export function BoardCards(props: { cards: readonly string[] }): ReactElement {
  return (
    <div className="flex gap-3 [--w:clamp(48px,calc((100cqi-64px)/5),72px)]">
      {PLACES.map((place, index) => {
        const card = props.cards.at(index);
        return card === undefined ? (
          <CardSlot key={place} label={`${place}, not yet dealt`} />
        ) : (
          <CardFace key={place} card={card} />
        );
      })}
    </div>
  );
}
