import type { Rejection } from "../protocol";
import { actionVerb } from "./action-text";
import { formatChips } from "./chips";

/**
 * A refused action, in the server's own numbers.
 *
 * Each variant is read off its own fields. The client states what it was told
 * and stops: it does not explain the rule behind the refusal, because it does
 * not know the rule and inventing one would be a game fact.
 */
export function rejectionText(rejection: Rejection): string {
  switch (rejection.type) {
    case "ActionNotAllowed":
      return `${actionVerb(rejection.attempted)} was refused. The server allows ${rejection.allowed
        .map(actionVerb)
        .join(", ")}.`;
    case "AmountTooSmall":
      return `${formatChips(rejection.attempted)} is under the minimum of ${formatChips(rejection.minimum)}.`;
    case "AmountTooLarge":
      return `${formatChips(rejection.attempted)} is over the maximum of ${formatChips(rejection.maximum)}.`;
    case "NotYourTurn":
      return rejection.seatToAct === null
        ? "The server says it is nobody's turn."
        : `The server says it is seat ${rejection.seatToAct}'s turn.`;
    case "HandComplete":
      return "The server says that hand is already over.";
  }
}
