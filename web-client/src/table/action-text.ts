import type { ActionType, LegalActions } from "../protocol";

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
