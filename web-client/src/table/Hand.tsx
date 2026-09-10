import type { ReactElement } from "react";
import { CardBack, CardFace } from "./PlayingCard";
import { standingOf } from "./winning-cards";

/**
 * Two places for a seat's hole cards: a face for each card the view carries,
 * a back for each it does not. `marked`, when given, is the set of cards that
 * took the pot, and every face-up card is drawn in relation to it
 * (`winning-cards.ts`).
 */
export function Hand(props: {
  cards: readonly string[];
  hiddenLabel: string;
  marked?: ReadonlySet<string>;
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
          <CardFace
            key={place}
            card={card}
            standing={standingOf(card, props.marked)}
          />
        );
      })}
    </>
  );
}
