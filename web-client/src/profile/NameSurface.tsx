import { type ReactElement, useRef, useState } from "react";
import type { PlayerProfile } from "./profile";
import type { SetNameOutcome } from "./set-name";
import {
  PERMANENCE_LINE,
  NAME_REMOVED_HEADING,
  NAME_REMOVED_BODY,
  refusalSentence,
  mayTryAgain,
} from "./name-text";

export function NameSurface(props: {
  readonly profile: PlayerProfile;
  readonly setName: (name: string) => Promise<SetNameOutcome>;
}): ReactElement {
  const { profile, setName } = props;
  const [inputValue, setInputValue] = useState("");
  // What the server sent back on a `named` outcome. Once set, this — not
  // the (by then stale) `profile` prop — is what the surface renders,
  // exactly as ADR-0029 §5 intends: the client is told the canonical name,
  // never left to assume the typed string survived unchanged.
  const [wonName, setWonName] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref, not just the state above: the guard below must see the current
  // in-flight status the instant the second submit runs, not after a render
  // has caught up.
  const submitInFlight = useRef(false);
  // The most recently settled refusal, if any. Replaced — never appended to
  // — by the next attempt: a player who fails twice reads one sentence, not
  // a log of every attempt they made.
  const [refusal, setRefusal] = useState<Exclude<
    SetNameOutcome["kind"],
    "named"
  > | null>(null);

  const displayName = wonName ?? profile.displayName;

  if (displayName !== null) {
    return (
      <section
        aria-label="your display name"
        className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
      >
        <p className="text-small">{displayName}</p>
      </section>
    );
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    // A name is permanent: a second submit while one is already in flight
    // must send nothing, not race the first and let the server pick a
    // winner (200) and a loser (403) the player never asked for.
    if (submitInFlight.current) {
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    setName(inputValue).then((outcome) => {
      if (outcome.kind === "named") {
        setWonName(outcome.profile.displayName);
        return;
      }
      // Every other outcome settles as a refusal: its sentence replaces
      // whatever was on screen, and `mayTryAgain` — not a condition kept
      // here too — decides whether the form survives it.
      submitInFlight.current = false;
      setIsSubmitting(false);
      setRefusal(outcome.kind);
    });
  };

  const canTryAgain = refusal === null || mayTryAgain(refusal);

  return (
    <section
      aria-label="your display name"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      {profile.displayName === null && profile.displayNameRemoved && (
        <div className="w-full">
          <p className="text-small">
            <em>{NAME_REMOVED_HEADING}</em>
          </p>
          <p className="text-small">{NAME_REMOVED_BODY}</p>
        </div>
      )}
      <p className="text-small">{PERMANENCE_LINE}</p>
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
      <form onSubmit={handleSubmit} className="w-full" hidden={!canTryAgain}>
        <div className="flex flex-col gap-3">
          <label htmlFor="name-input" className="text-small">
            <input
              id="name-input"
              type="text"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              className="w-full rounded-small border border-hairline px-3 py-2"
            />
          </label>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-small border border-hairline bg-surface px-4 py-2 text-small"
          >
            Set my name
          </button>
        </div>
      </form>
    </section>
  );
}
