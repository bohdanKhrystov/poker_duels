import { useState, type FormEvent, type ReactElement } from "react";

import {
  NAME_ASK_HEADING,
  NAME_FIELD_LABEL,
  NAME_IS_FOR,
  NAME_KEEPS,
  SKIP_AND_PLAY_LABEL,
  TAKE_NAME_LABEL,
} from "./name-ask-text";
import { suggestName } from "./name-suggestion";
import type { SetNameOutcome } from "./set-name";

/**
 * `ADR-0119` §1's ask: the screen between a player's own first press and the duel it started.
 * One field, carrying a drawn suggestion, and two equally reachable ways out — take the name it
 * offers (or one typed over it), or skip and duel without one (`ADR-0119` §2).
 *
 * Props in, no storage, no navigation: this surface asks the question and reports the answer.
 * What the caller does with either — mounting it at all, and what happens after `onNamed` or
 * `onSkip` fires — is `TASK-140715`'s.
 */
export function NameAsk(props: {
  readonly setName: (name: string) => Promise<SetNameOutcome>;
  readonly onNamed: () => void;
  readonly onSkip: () => void;
  readonly random?: () => number;
}): ReactElement {
  const { random = Math.random } = props;
  // One draw per mount, in the initialiser — never a render body (`ADR-0137` §5).
  const [drawn] = useState(() => suggestName(random));
  // The field's own state, seeded from the draw. Nothing ever writes over it besides the player
  // typing (`ADR-0119` §4) — a reroll on `conflict` is `TASK-140713`'s.
  const [value, setValue] = useState(drawn);

  const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    const typed = value;
    void props.setName(typed).then((outcome: SetNameOutcome) => {
      // `onNamed` fires only once the server has said yes, and only then — every other outcome
      // is `TASK-140712`'s to answer.
      if (outcome.kind === "named") {
        props.onNamed();
      }
    });
  };

  return (
    <section
      aria-label="choose your name"
      className="mx-auto flex w-full flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <h2 className="text-display leading-tight font-bold text-text">
        {NAME_ASK_HEADING}
      </h2>
      <p className="text-small text-text-muted">{NAME_IS_FOR}</p>
      <p className="text-small text-text-muted">{NAME_KEEPS}</p>
      <form
        onSubmit={handleSubmit}
        className="flex w-full flex-col items-center gap-4"
      >
        <label
          htmlFor="name-ask-field"
          className="flex w-full flex-col gap-2 text-left text-small text-text-muted"
        >
          {NAME_FIELD_LABEL}
          <input
            id="name-ask-field"
            type="text"
            value={value}
            onChange={(event) => setValue(event.target.value)}
            className="rounded-small border border-hairline bg-surface px-4 py-3 text-text"
          />
        </label>
        <button
          type="submit"
          className="w-full rounded-medium bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
        >
          {TAKE_NAME_LABEL}
        </button>
      </form>
      {/*
        Outside the form, deliberately (`ADR-0119` §2): inside it, Enter over the field would
        reach this control, and a player pressing return over a name they typed must not land in
        a duel unnamed.
      */}
      <button
        type="button"
        onClick={props.onSkip}
        className="w-full rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
      >
        {SKIP_AND_PLAY_LABEL}
      </button>
    </section>
  );
}
