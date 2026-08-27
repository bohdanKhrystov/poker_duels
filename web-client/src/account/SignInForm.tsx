import { type ReactElement, useRef, useState } from "react";
import type { SignInOutcome } from "./sign-in";
import {
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGN_IN_LABEL,
  SIGN_IN_REFUSED,
  SIGN_UP_FAILED,
} from "./account-text";

/**
 * The sentence stating why a sign-in attempt was refused.
 *
 * `refused` covers a wrong password and an unknown handle alike: the server made the two
 * indistinguishable on the wire (`ADR-0027` §6), so this function is given nothing but `kind` and
 * has nothing else to work with. Naming which field was wrong — from a hunch, from the shape of
 * the handle, from anything at all — would rebuild in words the oracle that design closed.
 * `failed` reuses `SIGN_UP_FAILED`: a broken server is a broken server on either form. `kind` has
 * only these two members to switch over — `ADR-0056` §1 gives sign-in no third, slower-down kind
 * of refusal to map, because an over-budget attempt already answers exactly like a wrong password.
 */
function refusalSentence(
  kind: Exclude<SignInOutcome["kind"], "signed-in">,
): string {
  switch (kind) {
    case "refused":
      return SIGN_IN_REFUSED;
    case "failed":
      return SIGN_UP_FAILED;
  }
}

export function SignInForm(props: {
  readonly signIn: (handle: string, password: string) => Promise<SignInOutcome>;
}): ReactElement {
  const { signIn } = props;
  const [handle, setHandle] = useState("");
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref, not just the state above: the guard in handleSubmit must see the
  // current in-flight status the instant a second submit runs, not after a
  // render has caught up — the same reason `SignUpForm` holds one, and for
  // the same reason: a credential attaches once per attempt.
  const submitInFlight = useRef(false);
  // The most recently settled outcome. Replaced — never appended to — by the
  // next attempt, so a player refused twice reads one sentence, not a log.
  const [outcome, setOutcome] = useState<SignInOutcome["kind"] | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    // A second submit before the first settles must send nothing: two
    // in-flight attempts would race the server into two different verdicts
    // for one click.
    if (submitInFlight.current) {
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    signIn(handle, password).then((result) => {
      submitInFlight.current = false;
      setIsSubmitting(false);
      setOutcome(result.kind);
    });
  };

  return (
    <section
      aria-label="sign in to an account"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      {/* `signed-in` renders nothing here — the caller reloads the document
          on that outcome, so this form has no success state of its own,
          only a submit that stops offering itself while the call is in
          flight. */}
      {outcome !== null && outcome !== "signed-in" && (
        <p role="status" className="text-small">
          {refusalSentence(outcome)}
        </p>
      )}
      <form onSubmit={handleSubmit} className="w-full">
        <div className="flex flex-col gap-3">
          <label htmlFor="sign-in-handle" className="text-small">
            {HANDLE_LABEL}
          </label>
          <input
            id="sign-in-handle"
            type="text"
            value={handle}
            onChange={(e) => setHandle(e.target.value)}
            className="w-full rounded-small border border-hairline px-3 py-2"
          />
          <label htmlFor="sign-in-password" className="text-small">
            {PASSWORD_LABEL}
          </label>
          <input
            id="sign-in-password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-small border border-hairline px-3 py-2"
          />
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-small border border-hairline bg-surface px-4 py-2 text-small"
          >
            {SIGN_IN_LABEL}
          </button>
        </div>
      </form>
    </section>
  );
}
