/**
 * ADR-0031 n6.3: no endpoint in this product returns an address, so no constant here can hold
 * one. The account screen shows which addresses are live, never what they are.
 *
 * ADR-0078 Consequences: the client says one sentence naming no mailbox, no domain and no other
 * account. Every sentence below obeys this rule; if a player reads their own misspelling back
 * from the server, the client has broken the separation this ADR drew.
 */

export const RECOVERY_ON =
  "Recovery is on. A verified address can set a new password for this account.";

export const RECOVERY_OFF =
  "Recovery is off. With no verified address, a forgotten password cannot be replaced and this account is lost.";

/**
 * The only place that branches on whether recovery is on or off. A component choosing between
 * the two sentences inline is a second place able to get it wrong.
 */
export function recoveryLine(has: boolean): string {
  if (has) {
    return RECOVERY_ON;
  }
  return RECOVERY_OFF;
}

export const ATTACH_LABEL = "Attach a recovery address";

export const ADDRESS_LABEL = "Email address";

export const CURRENT_PASSWORD_LABEL = "Current password";

export const ATTACH_WHY =
  "Your password is asked for here because a browser someone else reaches would otherwise become permanent ownership of this account.";

export const ATTACH_ACKNOWLEDGED =
  "If that address can take mail, a link is on its way. Recovery stays off until you follow it.";

export const ATTACH_ADDRESS_REFUSED =
  "That is not an address mail can be sent to.";

export const ATTACH_PASSWORD_WRONG =
  "That password does not match this account.";

export const ATTACH_FAILED = "That did not go through. Try again.";

export const VERIFY_HEADING = "Finish verifying an address";

export const VERIFY_DONE =
  "That address is attached. It can now set a new password for this account.";

export const VERIFY_LINK_DEAD =
  "That link has expired or has already been used. Ask for a new one from the account screen.";

export const VERIFY_ADDRESS_TAKEN =
  "That address is already attached to another account, so it cannot be attached to this one.";

export const VERIFY_NO_LINK =
  "Open the link from your mail to finish this. There is nothing on this screen to fill in.";

export const RESET_HEADING = "Set a new password";

export const NEW_PASSWORD_LABEL = "New password";

/**
 * ADR-0031 §4: a reset issues no session and returns no token, so a client that expected one
 * is a client that hangs. The player is told before they act rather than after.
 */
export const RESET_ENDS_EVERY_SESSION =
  "Setting a new password ends every session on every browser, including this one. You will sign in again with the new password.";

export const RESET_LINK_DEAD =
  "That link has expired or has already been used. Ask for a new one and try again.";
