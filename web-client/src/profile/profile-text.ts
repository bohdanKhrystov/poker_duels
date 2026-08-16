import type { DuelOutcomeWord } from "./recent-duels";

/** The outcome in the reader's words: Won, Lost, Drew. */
export function outcomeWord(outcome: DuelOutcomeWord): string {
  switch (outcome) {
    case "WON":
      return "Won";
    case "LOST":
      return "Lost";
    case "DREW":
      return "Drew";
  }
}

/** A coin delta, signed as the server signed it: `+1`, `−1`, `0`. */
export function coinDeltaText(delta: number): string {
  const minus = "−"; // U+2212 MINUS SIGN
  if (delta > 0) {
    return `+${delta}`;
  } else if (delta < 0) {
    return `${minus}${Math.abs(delta)}`;
  } else {
    return "0";
  }
}

/** A balance, stated: `7`, `0`, `−1`. No plus, and never clamped (`ADR-0014`). */
export function coinBalanceText(balance: number): string {
  const minus = "−"; // U+2212 MINUS SIGN
  if (balance < 0) {
    return `${minus}${Math.abs(balance)}`;
  } else {
    return balance.toString();
  }
}

/** When the duel finished, in the reader's locale. `""` if it cannot be read. */
export function finishedAtText(
  instant: string,
  options?: { readonly locales?: string; readonly timeZone?: string },
): string {
  try {
    const date = new Date(instant);
    if (Number.isNaN(date.getTime())) {
      return "";
    }
    return new Intl.DateTimeFormat(options?.locales, {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: options?.timeZone,
    }).format(date);
  } catch {
    return "";
  }
}
