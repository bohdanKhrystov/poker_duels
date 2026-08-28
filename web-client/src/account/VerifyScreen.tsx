import { useEffect, useRef, useState, type ReactElement } from "react";
import type { VerifyEmailOutcome } from "./verify-email";
import {
  VERIFY_HEADING,
  VERIFY_DONE,
  VERIFY_LINK_DEAD,
  VERIFY_ADDRESS_TAKEN,
  VERIFY_NO_LINK,
} from "./recovery-text";

/**
 * The screen a mailed verification link opens onto. It is handed a token and a `verify` call, and
 * asks nothing else of its surroundings — no navigation and no read of where the player already is
 * (`ADR-0060` §4). Deciding what a missing token or a stale link means for what happens next belongs
 * to whoever renders this screen; here, the token only ever becomes one of five sentences.
 *
 * The token is submitted once, in a mount effect, guarded by a ref held on the component instance
 * rather than a flag shared by every instance. React's Strict Mode runs a mount effect twice in
 * development, and a token that is single-use by construction must not be spent by the second of
 * those two calls — its answer would simply be discarded, and the player would see whatever the
 * second call happened to return.
 *
 * A `null` token sends no call at all and renders `VERIFY_NO_LINK`. `ADR-0081` §6 makes the bare
 * address a known state a reload lands on, not a failure, so asking a question whose answer changes
 * nothing here would spend nothing but still read as something having gone wrong.
 *
 * `failed` and `link-dead` render the same sentence on purpose — see `recovery-text.ts`. The two are
 * indistinguishable from here, and the player's next move is identical either way: ask for a new
 * link rather than retry a token that a second use would only spend again.
 */
export function VerifyScreen(props: {
  readonly token: string | null;
  readonly verify: (token: string) => Promise<VerifyEmailOutcome>;
}): ReactElement {
  const { token, verify } = props;
  const [outcome, setOutcome] = useState<VerifyEmailOutcome | null>(null);
  const askedRef = useRef(false);

  useEffect(() => {
    if (token === null || askedRef.current) {
      return;
    }
    askedRef.current = true;
    verify(token).then(setOutcome);
  }, [token, verify]);

  let sentence: string | null = null;
  if (token === null) {
    sentence = VERIFY_NO_LINK;
  } else if (outcome !== null) {
    sentence = outcomeSentence(outcome);
  }

  return (
    <section
      aria-label="finish verifying an address"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"
    >
      <h2>{VERIFY_HEADING}</h2>
      {sentence !== null && <p>{sentence}</p>}
    </section>
  );
}

function outcomeSentence(outcome: VerifyEmailOutcome): string {
  if (outcome.kind === "verified") {
    return VERIFY_DONE;
  }
  if (outcome.kind === "address-taken") {
    return VERIFY_ADDRESS_TAKEN;
  }
  // "failed" and "link-dead" collapse into one sentence: see the KDoc above.
  return VERIFY_LINK_DEAD;
}
