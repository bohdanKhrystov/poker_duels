import type { ActedForAbsent } from "../protocol";

/**
 * What the server did for an absent seat, in `ADR-0046` §4's words.
 *
 * The subject is always the server. `The server folded…` is not a claim about the rival;
 * `Your rival folded…` is, and a reader who stops after the verb has then been told something
 * false. Nothing here says why the seat is absent — the presence line already says as much as
 * is known, and repeating it on every action turns a fact into an accusation.
 */
export function absentActionText(
  mark: ActedForAbsent,
  mySeat: number | null,
): string {
  // Determine the subject clause based on the seat comparison.
  // Test mySeat === null first: mark.seat === null is never true,
  // and a null seat compared against 0 would silently answer "your rival".
  let subject: string;
  if (mySeat === null) {
    subject = "an absent seat";
  } else if (mark.seat === mySeat) {
    subject = "you";
  } else {
    subject = "your rival";
  }

  // Convert the action to its past tense verb.
  let verb: string;
  switch (mark.action) {
    case "FOLD":
      verb = "folded";
      break;
    case "CHECK":
      verb = "checked";
      break;
    default:
      // The type can only carry FOLD and CHECK per the Kotlin init,
      // but be defensive.
      return "";
  }

  return `The server ${verb} for ${subject}.`;
}
