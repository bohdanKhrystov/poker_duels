import type { ReactElement } from "react";
import type { ProfileStripState } from "../profile/profile-strip";
import type { SignUpOutcome } from "./sign-up";
import type { SignOutOutcome } from "./sign-out";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";
import type { SetNameOutcome } from "../profile/set-name";
import { SignUpForm } from "./SignUpForm";
import { RecoveryEmailForm } from "./RecoveryEmailForm";
import { SignOutControl } from "./SignOutControl";
import { NameSurface } from "../profile/NameSurface";
import {
  ACCOUNT_HEADING,
  PASSWORD_ROUTE_LIVE,
  SIGN_IN_HEADING,
  ANONYMOUS_STATE,
  ANONYMOUS_COST,
  ANONYMOUS_WAY_OUT,
  deviceRouteLine,
} from "./account-text";
import { recoveryLine } from "./recovery-text";

/**
 * The account screen: states, in words, which routes currently sign in to
 * this profile, and carries the forms a browser needs on either side of a
 * session — give this profile a password or reach an account already made
 * with no session, and sign out with one. It also carries the name form
 * (`ADR-0130` §6).
 *
 * `ADR-0037` requires the account screens to state which routes are live, and
 * `ADR-0050` §4 makes `deviceRouteLive` the whole of what this screen reads to
 * say so — `deviceRouteLine` is the only place that field becomes words.
 *
 * Renders as a prop-driven presentation over `ProfileStripState`, with no
 * hooks, fetching or provider read, so it is renderable in a test alone — the
 * same shape `HistoryScreen` and `LadderScreen` use (`ADR-0060` §4). `signUp`,
 * `signOut` and `onSignIn` are optional so a caller that only cares about the
 * route facts (`AccountScreen.test.tsx`) need not supply any of them.
 * `SignOutControl` gates its own visibility on `signedIn` internally
 * (`TASK-041221`), so this screen only has to withhold the prop where no
 * carrier for it exists. The revoke control (`RevokeControl`, `TASK-041220`)
 * still exists but is not placed on this screen yet.
 */
export function AccountScreen(props: {
  readonly profile: ProfileStripState | null;
  readonly signedIn: boolean;
  readonly signUp?: (
    handle: string,
    password: string,
  ) => Promise<SignUpOutcome>;
  readonly signOut?: () => Promise<SignOutOutcome>;
  readonly onSignIn?: () => void;
  readonly attachRecoveryEmail?: (
    address: string,
    currentPassword: string,
  ) => Promise<AttachRecoveryOutcome>;
  readonly setName?: (name: string) => Promise<SetNameOutcome>;
}): ReactElement {
  const {
    profile,
    signedIn,
    signUp,
    signOut,
    onSignIn,
    attachRecoveryEmail,
    setName,
  } = props;

  // With no profile in hand — still loading, no-profile, or unavailable — the
  // screen asserts neither route (`ADR-0037`): a sentence built from a read
  // the client never got back is not a fact the client was told.
  const deviceLine =
    profile !== null && profile.kind === "profile"
      ? deviceRouteLine(profile.profile.deviceRouteLive)
      : null;

  // With no profile in hand — still loading, no-profile, or unavailable — the
  // screen asserts neither fact about recovery (`ADR-0037`): a sentence built
  // from a read the client never got back is not a fact the client was told.
  // `ADR-0031` §6.3: no endpoint in this product returns an address, so this
  // screen never shows one.
  const recoveryText =
    profile !== null && profile.kind === "profile"
      ? recoveryLine(profile.profile.hasRecoveryEmail)
      : null;

  // `POST /api/auth/sign-in` is the only endpoint in `docs/protocol.md` that
  // ever returns a `sessionToken` — sign-up issues none and reset-password
  // issues none — so a browser holding a live session is a browser whose
  // player has a password, by construction. That derivation is why no
  // `hasCredential` field was asked for: `signedIn` alone carries it, and
  // only once a profile is actually in hand to state a fact about.
  const showPasswordRoute =
    signedIn && profile !== null && profile.kind === "profile";

  // ADR-0012 mints a profile on the first Welcome, so a browser with no live
  // session and a profile in hand is the state almost every real player is
  // in. That browser gets the form that gives its profile a password.
  const showSignUp =
    !signedIn &&
    profile !== null &&
    profile.kind === "profile" &&
    signUp !== undefined;

  // POST /api/auth/recovery-email answers 401 for a browser the server
  // cannot resolve, and offering a form whose only possible answer is *that
  // did not go through* is worse than offering none. The form is offered
  // whether recovery is already on or off — attaching a second address
  // replaces the pending claim on the server (ADR-0031 §3), and a screen that
  // hid the form once recovery was on would strand a player whose address
  // stopped working.
  const showAttach =
    profile !== null &&
    profile.kind === "profile" &&
    attachRecoveryEmail !== undefined;

  // `ADR-0083` §3: the one door to the sign-in screen, offered only when this
  // browser holds no session — never on whether a profile read answered,
  // since a browser whose only need is reaching an account it already has
  // may hold no usable profile at all.
  const showSignInDoor = !signedIn;

  // This is the **told** fact, negated: the server said the player holds no
  // password. It is not `!signedIn` — a browser holding no token is not a
  // browser whose player holds no password (`ADR-0125` §4, `ADR-0132` §4).
  const anonymous =
    profile !== null &&
    profile.kind === "profile" &&
    !profile.profile.hasPassword;

  // The name form is rendered when a profile is in hand, it is of kind
  // `profile`, and the caller has provided the `setName` callback. This
  // mirrors the condition on the front door which this form will eventually
  // replace (`TASK-140917`).
  const showNameForm =
    profile !== null && profile.kind === "profile" && setName !== undefined;

  return (
    <section
      aria-label="account"
      className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-7 text-center"
    >
      <h2 className="text-small">{ACCOUNT_HEADING}</h2>
      {showNameForm &&
      profile !== null &&
      profile.kind === "profile" &&
      setName !== undefined ? (
        <NameSurface profile={profile.profile} setName={setName} />
      ) : null}
      {deviceLine !== null ? <p className="text-small">{deviceLine}</p> : null}
      {recoveryText !== null ? (
        <p className="text-small">{recoveryText}</p>
      ) : null}
      {showPasswordRoute ? (
        <p className="text-small">{PASSWORD_ROUTE_LIVE}</p>
      ) : null}
      {anonymous ? (
        <>
          <p className="text-small">{ANONYMOUS_STATE}</p>
          <p className="text-small">{ANONYMOUS_COST}</p>
          <p className="text-small">{ANONYMOUS_WAY_OUT}</p>
        </>
      ) : null}
      {showSignUp && signUp !== undefined && <SignUpForm signUp={signUp} />}
      {showAttach && attachRecoveryEmail !== undefined && (
        <RecoveryEmailForm attach={attachRecoveryEmail} />
      )}
      {showSignInDoor && (
        <button
          type="button"
          className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
          onClick={onSignIn}
        >
          {SIGN_IN_HEADING}
        </button>
      )}
      {signOut !== undefined && (
        // A literal until `TASK-141013` wires this screen's own answer through.
        <SignOutControl
          signedIn={signedIn}
          signOutHandsANewProfile={false}
          signOut={signOut}
        />
      )}
    </section>
  );
}
