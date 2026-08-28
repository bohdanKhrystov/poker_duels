import { describe, expect, it } from "vitest";
import * as recoveryText from "./recovery-text";

describe("the account screen's words about recovery", () => {
  it("states every sentence exactly, character for character", () => {
    // Exactly these exports, and no others — an extra or a missing one fails here even if
    // every literal below still matches, because `toEqual` cannot see either.
    expect(Object.keys(recoveryText).sort()).toEqual(
      [
        "ATTACH_ACKNOWLEDGED",
        "ATTACH_ADDRESS_REFUSED",
        "ATTACH_FAILED",
        "ATTACH_LABEL",
        "ATTACH_PASSWORD_WRONG",
        "ATTACH_WHY",
        "ADDRESS_LABEL",
        "CURRENT_PASSWORD_LABEL",
        "RECOVERY_OFF",
        "RECOVERY_ON",
        "recoveryLine",
      ].sort(),
    );

    expect(recoveryText.RECOVERY_ON).toBe(
      "Recovery is on. A verified address can set a new password for this account.",
    );
    expect(recoveryText.RECOVERY_OFF).toBe(
      "Recovery is off. With no verified address, a forgotten password cannot be replaced and this account is lost.",
    );
    expect(recoveryText.ATTACH_LABEL).toBe("Attach a recovery address");
    expect(recoveryText.ADDRESS_LABEL).toBe("Email address");
    expect(recoveryText.CURRENT_PASSWORD_LABEL).toBe("Current password");
    expect(recoveryText.ATTACH_WHY).toBe(
      "Your password is asked for here because a browser someone else reaches would otherwise become permanent ownership of this account.",
    );
    expect(recoveryText.ATTACH_ACKNOWLEDGED).toBe(
      "If that address can take mail, a link is on its way. Recovery stays off until you follow it.",
    );
    expect(recoveryText.ATTACH_ADDRESS_REFUSED).toBe(
      "That is not an address mail can be sent to.",
    );
    expect(recoveryText.ATTACH_PASSWORD_WRONG).toBe(
      "That password does not match this account.",
    );
    expect(recoveryText.ATTACH_FAILED).toBe(
      "That did not go through. Try again.",
    );
  });

  it("says recovery is on and recovery is off from one place", () => {
    // Both inputs, and the inequality, in one test: a `recoveryLine` that ignores its
    // argument would still pass a `true`-only test and a `false`-only test each written alone.
    // Compared against the constants and not against string literals, so the test guards the
    // branch, not a duplicate of the constant.
    expect(recoveryText.recoveryLine(true)).toBe(recoveryText.RECOVERY_ON);
    expect(recoveryText.recoveryLine(false)).toBe(recoveryText.RECOVERY_OFF);
    expect(recoveryText.recoveryLine(true)).not.toBe(
      recoveryText.recoveryLine(false),
    );
  });

  it("names no mailbox, no domain and no other account", () => {
    // Iterated over every string export rather than a hand-written list, so a new constant is
    // covered automatically. The loop asserts the collection is non-empty first, so it cannot
    // pass over nothing.
    const stringValues = Object.values(recoveryText).filter(
      (value) => typeof value === "string",
    ) as string[];
    expect(stringValues.length).toBeGreaterThan(0);

    for (const value of stringValues) {
      expect(value).not.toContain("@");
      expect(value.toLowerCase()).not.toContain(".test");
      expect(value.toLowerCase()).not.toContain(".com");
      expect(value.toLowerCase()).not.toContain("example");
      expect(value.toLowerCase()).not.toContain("taken");
      expect(value.toLowerCase()).not.toContain("registered");
    }
  });
});
