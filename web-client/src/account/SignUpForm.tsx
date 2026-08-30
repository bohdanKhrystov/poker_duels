import { type ReactElement, useRef, useState } from "react";
import type { SignUpOutcome } from "./sign-up";
import {
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGN_UP_LABEL,
  SIGNED_UP,
  HANDLE_REFUSED,
  HANDLE_UNAVAILABLE,
  PASSWORD_REFUSED,
  NO_PROFILE_YET,
  SIGN_UP_THROTTLED,
  SIGN_UP_FAILED,
} from "./account-text";

/**
 * The sentence stating why a sign-up attempt was refused.
 *
 * `throttled` renders here too (`SIGN_UP_THROTTLED`), and nothing else does. `ADR-0056` §§1-3's
 * preserved-fields and no-follow-up rules hold without any code of their own: nothing on this
 * form ever clears a field, marks one invalid, or schedules a further request for any outcome,
 * throttled included.
 */
function refusalSentence(
  kind: Exclude<SignUpOutcome["kind"], "signed-up">,
): string {
  switch (kind) {
    case "handle-refused":
      return HANDLE_REFUSED;
    case "unavailable-handle":
      return HANDLE_UNAVAILABLE;
    case "password-refused":
      return PASSWORD_REFUSED;
    case "no-profile":
      return NO_PROFILE_YET;
    case "throttled":
      return SIGN_UP_THROTTLED;
    case "failed":
      return SIGN_UP_FAILED;
  }
}

export function SignUpForm(props: {
  readonly signUp: (handle: string, password: string) => Promise<SignUpOutcome>;
}): ReactElement {
  const { signUp } = props;
  // Independent from the day this form is composed beside a profile: neither
  // starts from a prop, and nothing here ever reads a display name (`ADR-0031`
  // §1, `ADR-0029`) — the two credential fields and the account's public name
  // are three separate strings.
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref, not just the state below: the guard in handleSubmit must see the
  // current in-flight status the instant a second submit runs, not after a
  // render has caught up — the same reason `NameSurface` holds one, and for
  // the same reason: a credential is attached once.
  const submitInFlight = useRef(false);
  // The most recently settled outcome. Replaced — never appended to — by the
  // next attempt, so a player who fails twice reads one sentence, not a log.
  const [outcome, setOutcome] = useState<SignUpOutcome["kind"] | null>(null);

  if (outcome === "signed-up") {
    return (
      <section
        aria-label="sign up for an account"
        className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
      >
        <p role="status" className="text-small">
          {SIGNED_UP}
        </p>
      </section>
    );
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    // A second submit before the first settles must send nothing: two
    // in-flight requests would race the server into attaching two
    // credentials, or one success and one confusing refusal.
    if (submitInFlight.current) {
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    signUp(handle, password).then((result) => {
      submitInFlight.current = false;
      setIsSubmitting(false);
      setOutcome(result.kind);
    });
  };

  return (
    <section
      aria-label="sign up for an account"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      {outcome !== null && (
        <p role="status" className="text-small">
          {refusalSentence(outcome)}
        </p>
      )}
      <form onSubmit={handleSubmit} className="w-full">
        <div className="flex flex-col gap-3">
          <label htmlFor="sign-up-handle" className="text-small">
            {HANDLE_LABEL}
          </label>
          <input
            id="sign-up-handle"
            type="text"
            value={handle}
            onChange={(e) => setHandle(e.target.value)}
            className="w-full rounded-small border border-hairline px-3 py-2"
          />
          <label htmlFor="sign-up-password" className="text-small">
            {PASSWORD_LABEL}
          </label>
          <input
            id="sign-up-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-small border border-hairline px-3 py-2"
          />
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
          >
            {SIGN_UP_LABEL}
          </button>
        </div>
      </form>
    </section>
  );
}
