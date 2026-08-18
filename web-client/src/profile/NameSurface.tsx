import { type ReactElement, useState } from "react";
import type { PlayerProfile } from "./profile";
import type { SetNameOutcome } from "./set-name";
import {
  PERMANENCE_LINE,
  NAME_REMOVED_HEADING,
  NAME_REMOVED_BODY,
} from "./name-text";

export function NameSurface(props: {
  readonly profile: PlayerProfile;
  readonly setName: (name: string) => Promise<SetNameOutcome>;
}): ReactElement {
  const { profile } = props;
  const [inputValue, setInputValue] = useState("");

  if (profile.displayName !== null) {
    return (
      <section
        aria-label="your display name"
        className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
      >
        <p className="text-small">{profile.displayName}</p>
      </section>
    );
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
  };

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
      <form onSubmit={handleSubmit} className="w-full">
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
            className="rounded-small border border-hairline bg-surface px-4 py-2 text-small"
          >
            Set my name
          </button>
        </div>
      </form>
    </section>
  );
}
