import { describe, expect, it } from "vitest";
import * as accountText from "./account-text";

describe("the account screen's words", () => {
  it("states every sentence exactly, character for character", () => {
    // Exactly these exports, and no others — an extra or a missing one fails here even if
    // every literal below still matches, because `toContain` cannot see either.
    expect(Object.keys(accountText).sort()).toEqual(
      [
        "ACCOUNT_HEADING",
        "CANCEL",
        "DEVICE_ROUTE_LIVE",
        "DEVICE_ROUTE_REVOKED",
        "HANDLE_LABEL",
        "HANDLE_REFUSED",
        "HANDLE_UNAVAILABLE",
        "NO_PROFILE_YET",
        "PASSWORD_LABEL",
        "PASSWORD_REFUSED",
        "PASSWORD_ROUTE_LIVE",
        "REVOKE_LABEL",
        "REVOKE_ONLY_WAY_BACK",
        "REVOKE_OTHER_SESSIONS",
        "REVOKE_PERMANENT",
        "SIGNED_UP",
        "SIGN_IN_LABEL",
        "SIGN_IN_REFUSED",
        "SIGN_OUT_LABEL",
        "SIGN_OUT_WARNING",
        "SIGN_UP_FAILED",
        "SIGN_UP_LABEL",
        "SIGN_UP_THROTTLED",
        "deviceRouteLine",
      ].sort(),
    );

    expect(accountText.ACCOUNT_HEADING).toBe("Account");
    expect(accountText.DEVICE_ROUTE_LIVE).toBe(
      "This device signs in to this account.",
    );
    expect(accountText.DEVICE_ROUTE_REVOKED).toBe(
      "This device no longer signs in to this account.",
    );
    expect(accountText.PASSWORD_ROUTE_LIVE).toBe(
      "Your password signs in to this account.",
    );
    expect(accountText.REVOKE_LABEL).toBe("Stop this device signing in");
    expect(accountText.REVOKE_PERMANENT).toBe(
      "This device will never sign in to this account again. This cannot be undone.",
    );
    expect(accountText.REVOKE_OTHER_SESSIONS).toBe(
      "You will be signed out on every other device. You stay signed in here.",
    );
    expect(accountText.REVOKE_ONLY_WAY_BACK).toBe(
      "Your password becomes the only way back to this account.",
    );
    expect(accountText.SIGN_OUT_LABEL).toBe("Sign out");
    expect(accountText.SIGN_OUT_WARNING).toBe(
      "Signing out leaves any duel room this browser is in, and a duel left this way can be lost. " +
        "This browser goes back to the profile it had before.",
    );
    expect(accountText.SIGN_UP_LABEL).toBe("Give this profile a password");
    expect(accountText.HANDLE_LABEL).toBe("Handle");
    expect(accountText.PASSWORD_LABEL).toBe("Password");
    expect(accountText.SIGNED_UP).toBe(
      "This profile now has a password. Sign in with it on any other browser.",
    );
    expect(accountText.HANDLE_REFUSED).toBe(
      "A handle is 3 to 32 of a–z, 0–9, dot, dash or underscore, and starts with a letter or a number.",
    );
    expect(accountText.HANDLE_UNAVAILABLE).toBe(
      "That handle is taken, or this profile already has a password.",
    );
    expect(accountText.PASSWORD_REFUSED).toBe(
      "A password is 8 to 128 characters.",
    );
    expect(accountText.NO_PROFILE_YET).toBe(
      "This browser has no profile yet. Reload the page and try again.",
    );
    expect(accountText.SIGN_UP_FAILED).toBe(
      "That did not go through. Try again.",
    );
    expect(accountText.SIGN_UP_THROTTLED).toBe(
      "Sign-up did not go through this time, and that is about the connection, not the player. " +
        "Nothing typed was refused, and no account was created. The profile is unchanged, with the " +
        "same duel coins and the same duels, and it can keep playing now and sign up again later.",
    );
    expect(accountText.SIGN_IN_LABEL).toBe("Sign in");
    expect(accountText.SIGN_IN_REFUSED).toBe(
      "That handle and password do not match an account.",
    );
    expect(accountText.CANCEL).toBe("Cancel");
  });

  it("says one thing about the device route in each of its two states", () => {
    // Both inputs, and the inequality, in one test: a `deviceRouteLine` that ignores its
    // argument would still pass a `true`-only test and a `false`-only test each written alone.
    expect(accountText.deviceRouteLine(true)).toBe(
      accountText.DEVICE_ROUTE_LIVE,
    );
    expect(accountText.deviceRouteLine(false)).toBe(
      accountText.DEVICE_ROUTE_REVOKED,
    );
    expect(accountText.deviceRouteLine(true)).not.toBe(
      accountText.deviceRouteLine(false),
    );
  });

  it("says the same thing about a wrong password and an unknown handle", () => {
    expect(accountText.SIGN_IN_REFUSED).not.toMatch(
      /handle (is|was) (unknown|not)/i,
    );
    expect(accountText.SIGN_IN_REFUSED).not.toMatch(
      /password (is|was) (wrong|incorrect)/i,
    );

    // Not a hand-written list: every string export is a candidate. The unified sentence is
    // itself a candidate under any wording, and so is anything shaped like the oracle it
    // refuses — a second, field-specific sentence would be the enumeration oracle in words
    // this test exists to catch, however that second constant happened to be worded.
    const stringExports = Object.values(accountText).filter(
      (value): value is string => typeof value === "string",
    );
    const mismatchShaped = stringExports.filter(
      (value) =>
        value === accountText.SIGN_IN_REFUSED ||
        /handle (is|was) (unknown|not)/i.test(value) ||
        /password (is|was) (wrong|incorrect)/i.test(value),
    );
    expect(mismatchShaped).toEqual([accountText.SIGN_IN_REFUSED]);
  });

  it("tells a deliberate refusal from a broken product", () => {
    expect(accountText.SIGN_UP_THROTTLED).not.toBe(accountText.SIGN_UP_FAILED);
    expect(accountText.SIGN_UP_THROTTLED).not.toBe("");
    expect(accountText.SIGN_UP_FAILED).not.toBe("");

    // The six outcomes the sign-up form can render: success, the four field-level refusals
    // (a merged sentence for the two 409s), the throttled state, and the generic failure.
    const signUpOutcomes = [
      accountText.SIGNED_UP,
      accountText.HANDLE_REFUSED,
      accountText.HANDLE_UNAVAILABLE,
      accountText.PASSWORD_REFUSED,
      accountText.SIGN_UP_FAILED,
      accountText.SIGN_UP_THROTTLED,
    ];
    expect(new Set(signUpOutcomes).size).toBe(6);
  });

  it("refuses a throttled sign-up without a digit, a mechanism or an accusation", () => {
    expect(accountText.SIGN_UP_THROTTLED).not.toMatch(/\d/);

    // Each word its own assertion: a failure names the exact word `toContain` found, rather
    // than a loop reporting only that some unnamed word matched.
    const lower = accountText.SIGN_UP_THROTTLED.toLowerCase();
    expect(lower).not.toContain("rate");
    expect(lower).not.toContain("limit");
    expect(lower).not.toContain("throttl");
    expect(lower).not.toContain("budget");
    expect(lower).not.toContain("security");
    expect(lower).not.toContain("error");
    expect(lower).not.toContain("blocked");
    expect(lower).not.toContain("banned");
    expect(lower).not.toContain("suspicious");
    expect(lower).not.toContain("too many");
    expect(lower).not.toContain("try again in");
    expect(lower).not.toContain("minute");
    expect(lower).not.toContain("second");
    expect(lower).not.toContain("hour");
  });

  it("never says revoke to a player", () => {
    // Iterated over every export rather than a hand-written list, so a new constant is covered
    // automatically.
    const sayingRevoke = Object.entries(accountText).filter(
      ([, value]) =>
        typeof value === "string" && value.toLowerCase().includes("revoke"),
    );
    expect(sayingRevoke).toEqual([]);
  });
});
