import { useState, type ReactElement } from "react";
import { CANCEL, SIGN_OUT_LABEL, SIGN_OUT_WARNING } from "./account-text";
import type { SignOutOutcome } from "./sign-out";

type Step = { readonly kind: "offered" } | { readonly kind: "confirming" };

/**
 * `STORY-0412`'s seventh criterion asks that signing out during a live duel warns before it
 * acts. `ADR-0076` §3 fixes `Lobby.tsx`'s branch order as `outcome`, then `view`, then
 * `roomCode`, and only then the chosen screen — so the account screen, and this control with
 * it, can never be on display while a frame has seated this tab. A branch that read a store for
 * a live duel would therefore be a branch no fixture in this client can ever reach. The warning
 * below is unconditional instead: true in every state this control can be mounted in, including
 * the one the criterion names, so this is a refusal of an unreachable branch, not an omission
 * of one.
 *
 * Offered only while `signedIn` — there is nothing to sign out of otherwise. Pressing
 * `SIGN_OUT_LABEL` only asks: it shows `SIGN_OUT_WARNING` and a confirming control, and
 * `props.signOut` is called from nowhere else. An in-page step, never a native dialog, exactly
 * as `RevokeControl` does — one shape for both confirmations on this screen.
 */
export function SignOutControl(props: {
  readonly signedIn: boolean;
  readonly signOut: () => Promise<SignOutOutcome>;
}): ReactElement | null {
  const [step, setStep] = useState<Step>({ kind: "offered" });

  if (!props.signedIn) {
    return null;
  }

  if (step.kind === "confirming") {
    const confirm = (): void => {
      void props.signOut();
    };
    return (
      <div>
        <p>{SIGN_OUT_WARNING}</p>
        <button type="button" onClick={confirm}>
          {SIGN_OUT_LABEL}
        </button>
        <button type="button" onClick={() => setStep({ kind: "offered" })}>
          {CANCEL}
        </button>
      </div>
    );
  }

  return (
    <button type="button" onClick={() => setStep({ kind: "confirming" })}>
      {SIGN_OUT_LABEL}
    </button>
  );
}
