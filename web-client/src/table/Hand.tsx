import type { ReactElement } from "react";
import { CardBack, CardFace } from "./PlayingCard";

/**
 * A seat's two cards, always two places wide.
 *
 * A place the view carries a card for is drawn face up; a place it does not is
 * drawn face down. An empty `holeCards` means "not entitled to see" — never "no
 * cards" — so a hand is never a gap and never narrows. Whether a seat folded is
 * `hasFolded`'s to say, and whether it is all in is `isAllIn`'s.
 */
export function Hand(props: {
  cards: readonly string[];
  hiddenLabel: string;
}): ReactElement {
  return (
    <>
      {[0, 1].map((place) => {
        const card = props.cards.at(place);
        return card === undefined ? (
          <CardBack
            key={place}
            label={place === 0 ? props.hiddenLabel : null}
          />
        ) : (
          <CardFace key={place} card={card} />
        );
      })}
    </>
  );
}
