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
