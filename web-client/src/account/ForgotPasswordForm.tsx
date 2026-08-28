import { type ReactElement, useRef, useState } from "react";
import type { ForgotPasswordOutcome } from "./forgot-password";
import { CANCEL } from "./account-text";
import {
  ADDRESS_LABEL,
  FORGOT_PASSWORD_ACKNOWLEDGED,
  FORGOT_PASSWORD_FAILED,
  FORGOT_PASSWORD_LABEL,
  FORGOT_PASSWORD_SUBMIT,
} from "./recovery-text";

/**
 * The sentence for a settled request. `ForgotPasswordOutcome` has exactly two kinds and this is
 * the only place that switches over them — never re-derived from a status, and never a third case,
 * because the flow this form drives has no unknown-address case to add one for.
 */
function outcomeSentence(outcome: ForgotPasswordOutcome): string {
  switch (outcome.kind) {
    case "accepted":
      return FORGOT_PASSWORD_ACKNOWLEDGED;
    case "failed":
      return FORGOT_PASSWORD_FAILED;
  }
}

export function ForgotPasswordForm(props: {
  readonly forgotPassword: (address: string) => Promise<ForgotPasswordOutcome>;
  readonly onCancel: () => void;
}): ReactElement {
  const { forgotPassword, onCancel } = props;
  // What was typed, sent on submit exactly as it stands here: no trim, no
  // case change, and nothing else in this component ever rewrites it.
  const [address, setAddress] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref, not just the state above: the guard below must see the current
  // in-flight status the instant a second submit event runs, not after a
  // render has caught up. A disabled control never dispatches a submit
  // event either, so the attribute alone is a claim about the DOM, not what
  // actually stops a second request.
  const submitInFlight = useRef(false);
  // The most recently settled outcome. Replaced — never appended to — by
  // the next attempt, so asking twice reads one sentence, not a log.
  const [outcome, setOutcome] = useState<ForgotPasswordOutcome | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (submitInFlight.current) {
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    forgotPassword(address).then((result) => {
      submitInFlight.current = false;
      setIsSubmitting(false);
      setOutcome(result);
    });
  };

  return (
    <section
      aria-label="forgot password"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <h2 className="text-small">{FORGOT_PASSWORD_LABEL}</h2>
      {outcome !== null && (
        <p role="status" className="text-small">
          {outcomeSentence(outcome)}
        </p>
      )}
      <form onSubmit={handleSubmit} className="w-full">
        <div className="flex flex-col gap-3">
          <label htmlFor="forgot-password-address" className="text-small">
            {ADDRESS_LABEL}
          </label>
          <input
            id="forgot-password-address"
            type="text"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            className="w-full rounded-small border border-hairline px-3 py-2"
          />
          <div className="flex justify-center gap-3">
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-small border border-hairline bg-surface px-4 py-2 text-small"
            >
              {FORGOT_PASSWORD_SUBMIT}
            </button>
            <button
              type="button"
              onClick={onCancel}
              className="rounded-small border border-hairline bg-surface px-4 py-2 text-small"
            >
              {CANCEL}
            </button>
          </div>
        </div>
      </form>
    </section>
  );
}
