import { useRef, useState, type FormEvent, type ReactElement } from "react";
import { PASSWORD_REFUSED } from "./account-text";
import {
  NEW_PASSWORD_LABEL,
  RESET_ENDS_EVERY_SESSION,
  RESET_HEADING,
  RESET_LINK_DEAD,
} from "./recovery-text";
import type { ResetPasswordOutcome } from "./reset-password";

/**
 * The screen a mailed reset link opens on.
 *
 * `RESET_HEADING` and `RESET_ENDS_EVERY_SESSION` render together, unconditionally, before
 * anything is typed or any control is pressed — the cost (`ADR-0031` §4: every session on
 * every browser is gone, this one included) is stated before the one request that pays it,
 * never after.
 *
 * A refused password (`422`) is answered before the token is looked at (`ADR-0080` §2), so it
 * spends nothing: the form stays enabled and the same token still works on the next submit. A
 * dead link and a failed attempt read the same sentence, because neither is a fact this screen
 * can act on differently and `ADR-0080` forbids reporting a refusal as a link still being good.
 *
 * `token` arrives as a prop rather than being read off this screen, and nothing here reads an
 * address, keeps a record of visits, or writes anywhere the browser remembers between loads —
 * a `204` carries no session to keep (`ADR-0031` §4). `onDone` runs exactly once, only once the
 * server has answered that `204`, leaving where the player goes next to the caller.
 */
export function ResetScreen(props: {
  readonly token: string | null;
  readonly reset: (
    token: string,
    newPassword: string,
  ) => Promise<ResetPasswordOutcome>;
  readonly onDone: () => void;
}): ReactElement {
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  // A ref, not state, gates re-entry. The `setBusy(true)` below schedules an update; it is not
  // guaranteed to have committed before a second, synchronous submit is dispatched, and a state
  // check alone can let two requests through for a token this product means to answer once.
  const submitting = useRef(false);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ): Promise<void> {
    event.preventDefault();
    const token = props.token;
    if (token === null || submitting.current) {
      return;
    }

    submitting.current = true;
    setBusy(true);
    const outcome = await props.reset(token, password);
    submitting.current = false;
    setBusy(false);

    switch (outcome.kind) {
      case "reset":
        // No sentence for this one: onDone runs and the caller replaces the screen. The secret
        // is about to leave with it, so nothing is left to keep it for.
        setPassword("");
        setMessage(null);
        props.onDone();
        return;
      case "password-refused":
        setMessage(PASSWORD_REFUSED);
        return;
      case "link-dead":
      case "failed":
        setMessage(RESET_LINK_DEAD);
        return;
      default: {
        const exhaustive: never = outcome;
        throw new Error(
          `unhandled reset outcome: ${JSON.stringify(exhaustive)}`,
        );
      }
    }
  }

  return (
    <section>
      <h1>{RESET_HEADING}</h1>
      <p>{RESET_ENDS_EVERY_SESSION}</p>
      <form onSubmit={handleSubmit}>
        <label htmlFor="reset-new-password">{NEW_PASSWORD_LABEL}</label>
        <input
          id="reset-new-password"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <button type="submit" disabled={props.token === null || busy}>
          Set new password
        </button>
      </form>
      {message === null ? null : <p>{message}</p>}
    </section>
  );
}
