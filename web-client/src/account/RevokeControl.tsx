import { useState, type ReactElement } from "react";
import {
  CANCEL,
  REVOKE_LABEL,
  REVOKE_ONLY_WAY_BACK,
  REVOKE_OTHER_SESSIONS,
  REVOKE_PERMANENT,
  SIGN_UP_FAILED,
  SIGN_UP_LABEL,
  deviceRouteLine,
} from "./account-text";
import type { RevokeOutcome } from "./revoke-device";

type Step =
  | { readonly kind: "offered" }
  | { readonly kind: "confirming"; readonly pending: boolean }
  | { readonly kind: "revoked" }
  | { readonly kind: "refused"; readonly message: string };

/**
 * Maps one outcome of `props.revoke` to the step this control shows next.
 *
 * `no-credential` and `failed` are `ADR-0050` §3's two refusals, and they must read as different
 * sentences — `SIGN_UP_LABEL` names the fix (a credential is what is missing), `SIGN_UP_FAILED`
 * is the register-independent "did not go through" sentence already used for a request that
 * simply did not succeed. Neither string is declared here; both are `account-text.ts`'s.
 * `no-session` reads as `failed` too: this control is offered only while `signedIn` is true, so a
 * server disagreeing at the moment of the call is the same kind of surprise as any other failure.
 */
function nextStep(outcome: RevokeOutcome): Step {
  switch (outcome.kind) {
    case "revoked":
      return { kind: "revoked" };
    case "no-credential":
      return { kind: "refused", message: SIGN_UP_LABEL };
    case "no-session":
    case "failed":
      return { kind: "refused", message: SIGN_UP_FAILED };
  }
}

/**
 * `ADR-0050`'s one stopping path for this device's sign-in route.
 *
 * Offered only where `ADR-0037` says a credential exists (`signedIn`, the only fact a session
 * token can mean) and the device route is still live (`deviceRouteLive`). Pressing `REVOKE_LABEL`
 * only asks: it shows `ADR-0050` §3's three facts and a confirming control, and `props.revoke` is
 * called from nowhere else. An in-page step, never a native dialog — three facts do not fit one.
 */
export function RevokeControl(props: {
  readonly deviceRouteLive: boolean;
  readonly signedIn: boolean;
  readonly revoke: () => Promise<RevokeOutcome>;
}): ReactElement | null {
  const [step, setStep] = useState<Step>({ kind: "offered" });

  if (!props.signedIn || !props.deviceRouteLive) {
    return null;
  }

  if (step.kind === "revoked") {
    return <p>{deviceRouteLine(false)}</p>;
  }

  if (step.kind === "confirming") {
    const confirm = (): void => {
      setStep({ kind: "confirming", pending: true });
      void props.revoke().then((outcome) => setStep(nextStep(outcome)));
    };
    return (
      <div>
        <p>{REVOKE_PERMANENT}</p>
        <p>{REVOKE_OTHER_SESSIONS}</p>
        <p>{REVOKE_ONLY_WAY_BACK}</p>
        <button type="button" disabled={step.pending} onClick={confirm}>
          {REVOKE_LABEL}
        </button>
        <button
          type="button"
          disabled={step.pending}
          onClick={() => setStep({ kind: "offered" })}
        >
          {CANCEL}
        </button>
      </div>
    );
  }

  return (
    <div>
      {step.kind === "refused" ? <p role="status">{step.message}</p> : null}
      <button
        type="button"
        onClick={() => setStep({ kind: "confirming", pending: false })}
      >
        {REVOKE_LABEL}
      </button>
    </div>
  );
}
