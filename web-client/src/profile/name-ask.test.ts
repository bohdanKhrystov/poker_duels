import { describe, it, expect } from "vitest";
import { askForName, type AskInput } from "./name-ask";

describe("who is asked for a name", () => {
  // Asking input with all conditions true
  const baseProfile = {
    playerId: "test-player",
    coinBalance: 0,
    displayName: null,
    displayNameRemoved: false,
    deviceRouteLive: false,
    hasRecoveryEmail: false,
  };

  const baseProfileState = {
    kind: "profile" as const,
    profile: baseProfile,
    duels: [],
  };

  const baseInput: AskInput = {
    profile: baseProfileState,
    skipped: false,
  };

  it("asks a player who holds no name and has not skipped", () => {
    expect(askForName(baseInput)).toBe(true);
  });

  it("never asks a player who already holds a name", () => {
    const modified: AskInput = {
      profile: {
        kind: "profile",
        profile: { ...baseProfile, displayName: "Ravenpost" },
        duels: baseProfileState.duels,
      },
      skipped: false,
    };
    expect(askForName(modified)).toBe(false);
  });

  it("never asks a player whose name was removed", () => {
    const modified: AskInput = {
      profile: {
        kind: "profile",
        profile: { ...baseProfile, displayNameRemoved: true },
        duels: baseProfileState.duels,
      },
      skipped: false,
    };
    expect(askForName(modified)).toBe(false);
  });

  it("never asks a browser that has already skipped", () => {
    const modified: AskInput = {
      profile: baseProfileState,
      skipped: true,
    };
    expect(askForName(modified)).toBe(false);
  });

  it("never asks before the profile read has landed", () => {
    const modified: AskInput = {
      profile: null,
      skipped: false,
    };
    expect(askForName(modified)).toBe(false);
  });

  it("never asks when the read answered no-profile or unavailable", () => {
    const noprofile: AskInput = {
      profile: { kind: "no-profile" },
      skipped: false,
    };
    expect(askForName(noprofile)).toBe(false);

    const unavailable: AskInput = {
      profile: { kind: "unavailable" },
      skipped: false,
    };
    expect(askForName(unavailable)).toBe(false);
  });
});
