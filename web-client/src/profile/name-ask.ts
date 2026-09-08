import type { ProfileStripState } from "./profile-strip";

export interface AskInput {
  readonly profile: ProfileStripState | null;
  readonly skipped: boolean;
}

/**
 * Whether the name ask should be shown to this player.
 *
 * The ask is shown exactly when all four conditions hold: the profile read has landed
 * and returned a profile (not null, not no-profile, not unavailable), the player holds
 * no display name, their name has not been removed, and they have not already skipped
 * the ask. Every condition is essential: a player with a name needs no ask; one whose
 * name was removed needs the reason shown on the name surface, not buried in an ask;
 * and one who skipped needs no nag, because skipping plays (`ADR-0119` §5).
 */
export function askForName(input: AskInput): boolean {
  // Must have a profile loaded
  if (input.profile === null || input.profile.kind !== "profile") {
    return false;
  }

  // Must hold no display name
  if (input.profile.profile.displayName !== null) {
    return false;
  }

  // Their name must not have been removed
  if (input.profile.profile.displayNameRemoved) {
    return false;
  }

  // Must not have already skipped
  if (input.skipped) {
    return false;
  }

  return true;
}
