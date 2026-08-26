export const ACCOUNT_HEADING = "Account";

export const DEVICE_ROUTE_LIVE = "This device signs in to this account.";

export const DEVICE_ROUTE_REVOKED =
  "This device no longer signs in to this account.";

export const PASSWORD_ROUTE_LIVE = "Your password signs in to this account.";

export const REVOKE_LABEL = "Stop this device signing in";

export const REVOKE_PERMANENT =
  "This device will never sign in to this account again. This cannot be undone.";

export const REVOKE_OTHER_SESSIONS =
  "You will be signed out on every other device. You stay signed in here.";

export const REVOKE_ONLY_WAY_BACK =
  "Your password becomes the only way back to this account.";

export const SIGN_OUT_LABEL = "Sign out";

export const SIGN_OUT_WARNING =
  "Signing out leaves any duel room this browser is in, and a duel left this way can be lost. " +
  "This browser goes back to the profile it had before.";

export const SIGN_UP_LABEL = "Give this profile a password";

export const HANDLE_LABEL = "Handle";

export const PASSWORD_LABEL = "Password";

export const SIGNED_UP =
  "This profile now has a password. Sign in with it on any other browser.";

export const HANDLE_REFUSED =
  "A handle is 3 to 32 of a–z, 0–9, dot, dash or underscore, and starts with a letter or a number.";

export const HANDLE_UNAVAILABLE =
  "That handle is taken, or this profile already has a password.";

export const PASSWORD_REFUSED = "A password is 8 to 128 characters.";

export const NO_PROFILE_YET =
  "This browser has no profile yet. Reload the page and try again.";

export const SIGN_UP_FAILED = "That did not go through. Try again.";

/**
 * `ADR-0056` §2's three permitted facts, and none of its five prohibited ones: no digit, no
 * duration, no count, no verdict on either field, no claim about the handle's availability, no
 * accusation, and no word naming a mechanism or a fault. Written in the third person on purpose —
 * §2 also refuses the second person, and every other refusal on this screen may address the
 * player directly because none of them carries the risk of accusing a bystander.
 */
export const SIGN_UP_THROTTLED =
  "Sign-up did not go through this time, and that is about the connection, not the player. " +
  "Nothing typed was refused, and no account was created. The profile is unchanged, with the " +
  "same duel coins and the same duels, and it can keep playing now and sign up again later.";

export const SIGN_IN_LABEL = "Sign in";

export const SIGN_IN_REFUSED =
  "That handle and password do not match an account.";

export const CANCEL = "Cancel";

/**
 * The sentence stating whether this device still signs in to this account.
 *
 * `ADR-0037` requires the account screens to state which routes are live: a player who has not
 * revoked is entitled to know the device still signs in, and a player who has revoked is entitled
 * to see that it does not. These are two different facts about the world, and this function is the
 * single place that branches on them — just as `emptyLine` is the only place that branches on
 * whether a history filter matched. A component choosing between the two sentences inline would be
 * a second place able to get it wrong.
 */
export function deviceRouteLine(live: boolean): string {
  if (live) {
    return DEVICE_ROUTE_LIVE;
  }
  return DEVICE_ROUTE_REVOKED;
}
