import { describe, expect, it } from "vitest";
import * as recoveryText from "./recovery-text";
import { PASSWORD_REFUSED } from "./account-text";

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
        "NEW_PASSWORD_LABEL",
        "RECOVERY_OFF",
        "RECOVERY_ON",
        "RESET_ENDS_EVERY_SESSION",
        "RESET_HEADING",
        "RESET_LINK_DEAD",
        "VERIFY_ADDRESS_TAKEN",
        "VERIFY_DONE",
        "VERIFY_HEADING",
        "VERIFY_LINK_DEAD",
        "VERIFY_NO_LINK",
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
    expect(recoveryText.VERIFY_HEADING).toBe("Finish verifying an address");
    expect(recoveryText.VERIFY_DONE).toBe(
      "That address is attached. It can now set a new password for this account.",
    );
    expect(recoveryText.VERIFY_LINK_DEAD).toBe(
      "That link has expired or has already been used. Ask for a new one from the account screen.",
    );
    expect(recoveryText.VERIFY_ADDRESS_TAKEN).toBe(
      "That address is already attached to another account, so it cannot be attached to this one.",
    );
    expect(recoveryText.VERIFY_NO_LINK).toBe(
      "Open the link from your mail to finish this. There is nothing on this screen to fill in.",
    );
    expect(recoveryText.RESET_HEADING).toBe("Set a new password");
    expect(recoveryText.NEW_PASSWORD_LABEL).toBe("New password");
    expect(recoveryText.RESET_ENDS_EVERY_SESSION).toBe(
      "Setting a new password ends every session on every browser, including this one. You will sign in again with the new password.",
    );
    expect(recoveryText.RESET_LINK_DEAD).toBe(
      "That link has expired or has already been used. Ask for a new one and try again.",
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

  it("tells a dead link and a refused password apart, in words a player can act on", () => {
    // RESET_LINK_DEAD and PASSWORD_REFUSED are different strings, so a player told the password
    // is wrong can then be told the link is dead without contradiction.
    expect(recoveryText.RESET_LINK_DEAD).not.toBe(PASSWORD_REFUSED);
    expect(recoveryText.RESET_LINK_DEAD).not.toContain(PASSWORD_REFUSED);
    expect(PASSWORD_REFUSED).not.toContain(recoveryText.RESET_LINK_DEAD);

    // RESET_LINK_DEAD and VERIFY_LINK_DEAD are different strings, because the two screens send
    // a player to different places.
    expect(recoveryText.RESET_LINK_DEAD).not.toBe(
      recoveryText.VERIFY_LINK_DEAD,
    );

    // VERIFY_NO_LINK is not an error — no sentences here say invalid, error, expired or used,
    // because an absent token is an empty input rather than a failure.
    expect(recoveryText.VERIFY_NO_LINK).not.toContain("invalid");
    expect(recoveryText.VERIFY_NO_LINK).not.toContain("error");
    expect(recoveryText.VERIFY_NO_LINK).not.toContain("expired");
    expect(recoveryText.VERIFY_NO_LINK).not.toContain("used");
  });
});
