import { type ReactElement, useRef, useState } from "react";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";
import {
  ADDRESS_LABEL,
  CURRENT_PASSWORD_LABEL,
  ATTACH_LABEL,
  ATTACH_WHY,
  ATTACH_ACKNOWLEDGED,
  ATTACH_ADDRESS_REFUSED,
  ATTACH_PASSWORD_WRONG,
  ATTACH_FAILED,
} from "./recovery-text";

/**
 * The five kinds `attach` can settle to, named independently of the imported outcome type: this
 * component switches on a `kind`, never on a status, and the ticket that shaped this file pins the
 * import and the prop annotation as the only two places the outcome type's name appears.
 */
type AttachOutcomeKind =
  "accepted" | "address-refused" | "no-profile" | "password-refused" | "failed";

/**
 * The sentence for a settled attach attempt.
 *
 * `no-profile` and `failed` share `ATTACH_FAILED` deliberately: a browser the server does not
 * recognise and a request that never arrived are the same thing to the player, and neither is
 * their fault to be told apart from the other.
 */
function outcomeSentence(kind: AttachOutcomeKind): string {
  switch (kind) {
    case "accepted":
      return ATTACH_ACKNOWLEDGED;
    case "address-refused":
      return ATTACH_ADDRESS_REFUSED;
    case "password-refused":
      return ATTACH_PASSWORD_WRONG;
    case "no-profile":
    case "failed":
      return ATTACH_FAILED;
  }
}

export function RecoveryEmailForm(props: {
  readonly attach: (
    address: string,
    currentPassword: string,
  ) => Promise<AttachRecoveryOutcome>;
}): ReactElement {
  const { attach } = props;
  // Independent state for the two fields: an address and a password mean
  // different things on this screen even though both are typed into text
  // boxes, and nothing here ever reads one to fill the other.
  const [address, setAddress] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  // A ref, not just the state above: the guard in handleSubmit must see the
  // current in-flight status the instant a second submit runs, not after a
  // render has caught up — the same reason `SignUpForm` holds one, and for
  // the same reason: an over-budget attempt still spends one of `ADR-0079`'s
  // five attempts a minute.
  const submitInFlight = useRef(false);
  // The most recently settled outcome. Replaced — never appended to — by the
  // next attempt, so a player who is refused twice reads one sentence, not a
  // log.
  const [outcome, setOutcome] = useState<AttachOutcomeKind | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    // A second submit before the first settles must send nothing: two
    // in-flight requests would race the server, and a double click must not
    // cost two of the caller's five attempts a minute for one press.
    if (submitInFlight.current) {
      return;
    }
    submitInFlight.current = true;
    setIsSubmitting(true);
    attach(address, currentPassword).then((result) => {
      submitInFlight.current = false;
      setIsSubmitting(false);
      setOutcome(result.kind);
      if (result.kind === "accepted") {
        // The address is what the player looks at to check they typed it
        // right; the password is a secret with no reason to stay in the DOM.
        setCurrentPassword("");
      }
    });
  };

  return (
    <section
      aria-label="attach a recovery address"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <p className="text-small">{ATTACH_WHY}</p>
      {outcome !== null && (
        <p role="status" className="text-small">
          {outcomeSentence(outcome)}
        </p>
      )}
      <form onSubmit={handleSubmit} className="w-full">
        <div className="flex flex-col gap-3">
          <div className="text-left">
            <label
              htmlFor="attach-recovery-address"
              className="text-small text-text-muted"
            >
              {ADDRESS_LABEL}
            </label>
            <input
              id="attach-recovery-address"
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full rounded-small border border-hairline px-3 py-2"
            />
          </div>
          <div className="text-left">
            <label
              htmlFor="attach-recovery-current-password"
              className="text-small text-text-muted"
            >
              {CURRENT_PASSWORD_LABEL}
            </label>
            <input
              id="attach-recovery-current-password"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full rounded-small border border-hairline px-3 py-2"
            />
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-medium border border-transparent bg-accent-fill px-5 py-4 leading-tight font-medium text-on-accent"
          >
            {ATTACH_LABEL}
          </button>
        </div>
      </form>
    </section>
  );
}
