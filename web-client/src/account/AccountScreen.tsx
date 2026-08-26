import type { ReactElement } from "react";
import type { ProfileStripState } from "../profile/profile-strip";
import {
  ACCOUNT_HEADING,
  PASSWORD_ROUTE_LIVE,
  deviceRouteLine,
} from "./account-text";

/**
 * The account screen: states, in words, which routes currently sign in to
 * this profile.
 *
 * `ADR-0037` requires the account screens to state which routes are live, and
 * `ADR-0050` §4 makes `deviceRouteLive` the whole of what this screen reads to
 * say so — `deviceRouteLine` is the only place that field becomes words.
 *
 * Renders as a prop-driven presentation over `ProfileStripState`, with no
 * hooks, fetching or provider read, so it is renderable in a test alone — the
 * same shape `HistoryScreen` and `LadderScreen` use (`ADR-0060` §4). The
 * sign-up form, the revoke control and the sign-out control are later tickets
 * (`TASK-041218`, `TASK-041220`, `TASK-041221`); this screen renders only the
 * heading and the two route facts, and leaves the rest empty.
 */
export function AccountScreen(props: {
  readonly profile: ProfileStripState | null;
  readonly signedIn: boolean;
}): ReactElement {
  const { profile, signedIn } = props;

  // With no profile in hand — still loading, no-profile, or unavailable — the
  // screen asserts neither route (`ADR-0037`): a sentence built from a read
  // the client never got back is not a fact the client was told.
  const deviceLine =
    profile !== null && profile.kind === "profile"
      ? deviceRouteLine(profile.profile.deviceRouteLive)
      : null;

  // `POST /api/auth/sign-in` is the only endpoint in `docs/protocol.md` that
  // ever returns a `sessionToken` — sign-up issues none and reset-password
  // issues none — so a browser holding a live session is a browser whose
  // player has a password, by construction. That derivation is why no
  // `hasCredential` field was asked for: `signedIn` alone carries it, and
  // only once a profile is actually in hand to state a fact about.
  const showPasswordRoute =
    signedIn && profile !== null && profile.kind === "profile";

  return (
    <section
      aria-label="account"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <h2 className="text-small">{ACCOUNT_HEADING}</h2>
      {deviceLine !== null ? <p className="text-small">{deviceLine}</p> : null}
      {showPasswordRoute ? (
        <p className="text-small">{PASSWORD_ROUTE_LIVE}</p>
      ) : null}
    </section>
  );
}
