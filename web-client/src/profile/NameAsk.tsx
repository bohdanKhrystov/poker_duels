import { useRef, useState, type FormEvent, type ReactElement } from "react";

import { useReportNameWrite } from "./profile-provider";
import {
  NAME_ASK_HEADING,
  NAME_FIELD_LABEL,
  NAME_IS_FOR,
  NAME_KEEPS,
  SKIP_AND_PLAY_LABEL,
  TAKE_NAME_LABEL,
} from "./name-ask-text";
import { mayTryAgain, refusalSentence } from "./name-text";
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
  // A ref, not just state: the guard must see the current in-flight status the instant the
  // second submit runs, not after a render has caught up.
  const submitInFlight = useRef(false);
  // State tracking for the UI disabled state — the ref is for the guard, this is for rendering.
  const [isSubmitting, setIsSubmitting] = useState(false);
  // The most recently settled refusal, if any. Replaced — never appended to — by the next
  // attempt: a player who fails twice reads one sentence, not a log.
  const [refusal, setRefusal] = useState<Exclude<
    SetNameOutcome["kind"],
    "named"
  > | null>(null);
  const reportNameWrite = useReportNameWrite();

  const canTryAgain = refusal === null || mayTryAgain(refusal);

  const handleSubmit = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault();
    if (submitInFlight.current) return;
    submitInFlight.current = true;
    setIsSubmitting(true);
    const typed = value;
    void props.setName(typed).then((outcome: SetNameOutcome) => {
      // Unconditional: the hook itself is a no-op on every refusal kind, so branching on `kind`
      // here would only duplicate that decision.
      reportNameWrite(outcome);
      // `onNamed` fires only once the server has said yes, and only then — every other outcome
      // is a refusal.
      if (outcome.kind === "named") {
        props.onNamed();
        return;
      }
      // Every other outcome settles as a refusal: its sentence replaces whatever was on screen,
      // and `mayTryAgain` — not a condition kept here too — decides whether the form survives it.
      submitInFlight.current = false;
      setIsSubmitting(false);
      setRefusal(outcome.kind);
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
      {refusal !== null && (
        <p role="status" className="text-small">
          {refusalSentence(refusal)}
        </p>
      )}
      {/*
        Hidden, not unmounted: the field and button stay the same DOM node
        across a settle a player can act on, so what they typed is never
        lost to a remount, and `role` queries agree there is no form the
        instant `mayTryAgain` says there is none.
      */}
      <form
        onSubmit={handleSubmit}
        className="flex w-full flex-col items-center gap-4"
        hidden={!canTryAgain}
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
          disabled={isSubmitting}
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
