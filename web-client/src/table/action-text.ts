import type { ActionType, LegalActions } from "../protocol";
import type { ActEvent } from "../store/duel-state";

/** What a button says: the verb, and the figure beside it when there is one. */
export interface ActionText {
  readonly verb: string;
  readonly amount: number | null;
}

/**
 * One action, in the player's language.
 *
 * A translation of the server's own token and nothing more: no action gains a
 * verb the server did not name, and none loses one.
 */
export function actionVerb(type: ActionType): string {
  switch (type) {
    case "FOLD":
      return "Fold";
    case "CHECK":
      return "Check";
    case "CALL":
      return "Call";
    case "BET":
      return "Bet";
    case "RAISE":
      return "Raise to";
    case "ALL_IN":
      return "All in";
  }
}

/**
 * What one button says, given the turn the server opened and the total the
 * player has dialled in.
 *
 * Every figure here is the server's or the player's own: `callTo` and `allInTo`
 * came off the wire, and `to` is what the player set on the amount control.
 * Nothing is priced, netted or worked out.
 */
export function actionText(
  type: ActionType,
  actions: LegalActions,
  to: number,
): ActionText {
  switch (type) {
    case "CALL":
      return { verb: actionVerb(type), amount: actions.callTo };
    case "ALL_IN":
      return { verb: actionVerb(type), amount: actions.allInTo };
    case "BET":
    case "RAISE":
      return { verb: actionVerb(type), amount: to };
    default:
      return { verb: actionVerb(type), amount: null };
  }
}

/**
 * `lastActText` translates an act event into what the mark should say.
 *
 * The mark translates the event's own token and carries the event's own `to`
 * total — it is the actor's own button's words and the server's own figure,
 * nothing worked out or invented. The verb is what `actionVerb` names the act;
 * the figure is the event's own total for a call, bet, raise or all-in, and
 * null for a fold or check. Per `ADR-0109` §2, the mark says what the actor's
 * own button said, no more and no less.
 */
export function lastActText(event: ActEvent): ActionText {
  switch (event.type) {
    case "PlayerFolded":
      return { verb: actionVerb("FOLD"), amount: null };
    case "PlayerChecked":
      return { verb: actionVerb("CHECK"), amount: null };
    case "PlayerCalled":
      return { verb: actionVerb("CALL"), amount: event.to };
    case "PlayerBet":
      return { verb: actionVerb("BET"), amount: event.to };
    case "PlayerRaised":
      return { verb: actionVerb("RAISE"), amount: event.to };
    case "PlayerAllIn":
      return { verb: actionVerb("ALL_IN"), amount: event.to };
  }
}
