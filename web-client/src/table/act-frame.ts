import type { Act, ActionType, PlayerAction } from "../protocol";
import type { PendingTurn } from "../store/duel-state";

/**
 * The one frame in which this client asserts anything.
 *
 * `handNumber` and `actionSequence` are copied from the `YourTurn` that opened
 * the turn — never from the view, which the server may have replaced since —
 * and the seat is `legalActions.seat`, which is the server's own word for who
 * is being asked. The client never names its seat from anywhere else.
 *
 * @param turn The pending turn the server opened.
 * @param type The action the player chose, as the server named it.
 * @param to The **total committed on this street** after a bet or a raise, not
 *   the amount added: the field is named `to` for that reason, and a delta is
 *   rejected as `AmountTooSmall`.
 */
export function actFrame(turn: PendingTurn, type: ActionType, to: number): Act {
  return {
    type: "Act",
    handNumber: turn.handNumber,
    actionSequence: turn.actionSequence,
    action: playerAction(type, turn.legalActions.seat, to),
  };
}

function playerAction(
  type: ActionType,
  seat: number,
  to: number,
): PlayerAction {
  switch (type) {
    case "FOLD":
      return { type: "Fold", seat };
    case "CHECK":
      return { type: "Check", seat };
    case "CALL":
      return { type: "Call", seat };
    case "BET":
      return { type: "Bet", seat, to };
    case "RAISE":
      return { type: "Raise", seat, to };
    case "ALL_IN":
      return { type: "AllIn", seat };
  }
}
