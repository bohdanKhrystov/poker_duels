import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AccountScreen } from "./AccountScreen";
import {
  ACCOUNT_HEADING,
  CANCEL,
  DEVICE_ROUTE_LIVE,
  DEVICE_ROUTE_REVOKED,
  HANDLE_LABEL,
  PASSWORD_LABEL,
  PASSWORD_ROUTE_LIVE,
  SIGN_IN_HEADING,
  SIGN_OUT_LABEL,
  SIGN_OUT_WARNING,
  SIGN_OUT_HANDS_A_NEW_PROFILE,
  SIGN_UP_LABEL,
  ANONYMOUS_STATE,
  ANONYMOUS_COST,
  ANONYMOUS_WAY_OUT,
} from "./account-text";
import {
  RECOVERY_ON,
  RECOVERY_OFF,
  ATTACH_LABEL,
  ADDRESS_LABEL,
  CURRENT_PASSWORD_LABEL,
} from "./recovery-text";
import { aProfile } from "../profile/profile-fixture";
import type { ProfileStripState } from "../profile/profile-strip";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";
import type { SignUpOutcome } from "./sign-up";
import type { SetNameOutcome } from "../profile/set-name";

describe("the account screen", () => {
  it("says the device signs in, and says it stopped, from the server fact alone", () => {
    const live: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ deviceRouteLive: true }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={live}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).not.toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();

    const revoked: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ deviceRouteLive: false }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={revoked}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).not.toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
  });

  it("says the password signs in to a browser holding a session", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={true}
        signOutHandsANewProfile={false}
      />,
    );

    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).not.toBeNull();
  });

  it("says nothing about a password route to a browser holding no session", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).not.toBeNull();
  });

  it("asserts no route of its own while the profile has not landed", () => {
    render(
      <AccountScreen
        profile={null}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();
    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();
    expect(
      screen.getByRole("heading", { level: 2, name: ACCOUNT_HEADING }),
    ).not.toBeNull();
  });

  it("says nothing about routes when the profile read failed", () => {
    const unavailable: ProfileStripState = { kind: "unavailable" };
    const { rerender } = render(
      <AccountScreen
        profile={unavailable}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();
    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();

    const noProfile: ProfileStripState = { kind: "no-profile" };
    rerender(
      <AccountScreen
        profile={noProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();
    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();
  });

  it("carries exactly one heading", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={true}
        signOutHandsANewProfile={false}
      />,
    );

    const headings = screen.getAllByRole("heading");
    expect(headings).toHaveLength(1);
    expect(headings[0].tagName).toBe("H2");
    expect(headings[0].textContent).toBe(ACCOUNT_HEADING);
  });

  it("says recovery is on for one profile and off for another, in one render each", () => {
    const recoveryOn: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={recoveryOn}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const recoveryOff: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: false }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={recoveryOff}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_OFF)).not.toBeNull();
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
  });

  it("says nothing about recovery when no profile is in hand", () => {
    const noProfile: ProfileStripState = { kind: "no-profile" };
    const { rerender } = render(
      <AccountScreen
        profile={noProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const unavailable: ProfileStripState = { kind: "unavailable" };
    rerender(
      <AccountScreen
        profile={unavailable}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const nullProfile = null;
    rerender(
      <AccountScreen
        profile={nullProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const withProfile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={withProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();
  });

  it("renders no address, because it is given none and asks for none", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    const { container } = render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();
    expect(container.textContent).not.toMatch(/@/);
  });

  it("offers the attach form only with a profile in hand and a call to make", () => {
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });

    // With profile and prop: form is present.
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.getByRole("button", { name: ATTACH_LABEL })).not.toBeNull();

    // With profile and no prop: form is absent.
    rerender(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByRole("button", { name: ATTACH_LABEL })).toBeNull();

    // With prop and profile=null: form is absent.
    rerender(
      <AccountScreen
        profile={null}
        signedIn={false}
        signOutHandsANewProfile={false}
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.queryByRole("button", { name: ATTACH_LABEL })).toBeNull();

    // With prop and kind="no-profile": form is absent.
    const noProfile: ProfileStripState = { kind: "no-profile" };
    rerender(
      <AccountScreen
        profile={noProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.queryByRole("button", { name: ATTACH_LABEL })).toBeNull();
  });

  it("offers it whether recovery is already on or not", () => {
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });

    // Recovery on: form is present and recovery sentence is RECOVERY_ON.
    const recoveryOn: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={recoveryOn}
        signedIn={false}
        signOutHandsANewProfile={false}
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.getByRole("button", { name: ATTACH_LABEL })).not.toBeNull();
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();

    // Recovery off: form is present and recovery sentence is RECOVERY_OFF.
    const recoveryOff: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: false }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={recoveryOff}
        signedIn={false}
        signOutHandsANewProfile={false}
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.getByRole("button", { name: ATTACH_LABEL })).not.toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).not.toBeNull();
  });

  it("the sign-in door is a drawn button, not a sentence", () => {
    render(
      <AccountScreen
        profile={null}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    const signIn = screen.getByRole("button", { name: SIGN_IN_HEADING });
    expect(signIn.classList.contains("border-hairline")).toBe(true);
    expect(signIn.classList.contains("px-5")).toBe(true);
  });

  it("sign out and its confirmation are drawn buttons, not sentences", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    render(
      <AccountScreen
        profile={null}
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    const offered = screen.getByRole("button", { name: SIGN_OUT_LABEL });
    expect(offered.classList.contains("border-hairline")).toBe(true);
    expect(offered.classList.contains("px-5")).toBe(true);

    fireEvent.click(offered);

    const confirming = screen.getByRole("button", { name: SIGN_OUT_LABEL });
    expect(confirming.classList.contains("border-hairline")).toBe(true);
    expect(confirming.classList.contains("px-5")).toBe(true);

    const cancel = screen.getByRole("button", { name: CANCEL });
    expect(cancel.classList.contains("border-hairline")).toBe(true);
    expect(cancel.classList.contains("px-5")).toBe(true);
  });

  it("both account forms submit with the card's fill button", () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });

    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
        signUp={signUp}
        attachRecoveryEmail={attach}
      />,
    );

    const signUpSubmit = screen.getByRole("button", { name: SIGN_UP_LABEL });
    expect(signUpSubmit.classList.contains("bg-accent-fill")).toBe(true);
    expect(signUpSubmit.classList.contains("text-on-accent")).toBe(true);

    const attachSubmit = screen.getByRole("button", { name: ATTACH_LABEL });
    expect(attachSubmit.classList.contains("bg-accent-fill")).toBe(true);
    expect(attachSubmit.classList.contains("text-on-accent")).toBe(true);
  });

  it("both account forms left-align their fields and mute their labels", () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });

    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
        signUp={signUp}
        attachRecoveryEmail={attach}
      />,
    );

    // Every field on both forms, enumerated — a fix applied to one label or
    // one form and not the rest must fail this, not just the first field.
    const fieldLabels = [
      HANDLE_LABEL,
      PASSWORD_LABEL,
      ADDRESS_LABEL,
      CURRENT_PASSWORD_LABEL,
    ];

    for (const labelText of fieldLabels) {
      const label = screen.getByText(labelText);
      expect(label.tagName).toBe("LABEL");
      expect(label.classList.contains("text-small")).toBe(true);
      expect(label.classList.contains("text-text-muted")).toBe(true);

      const wrapper = label.parentElement;
      expect(wrapper).not.toBeNull();
      expect(wrapper?.classList.contains("text-left")).toBe(true);
    }
  });

  it("names the profile anonymous when the profile it holds has no password", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: false }),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    expect(screen.queryByText(ANONYMOUS_STATE)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).not.toBeNull();
  });

  it("says nothing about the state to a profile that holds a password", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: true }),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    expect(screen.queryByText(ANONYMOUS_STATE)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).toBeNull();
    // Ensure the screen still renders
    expect(
      screen.getByRole("heading", { level: 2, name: ACCOUNT_HEADING }),
    ).not.toBeNull();
  });

  it("says nothing about the state where it was told nothing", () => {
    // Test with profile=null (still loading)
    const { rerender } = render(
      <AccountScreen
        profile={null}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).toBeNull();

    // Test with kind="no-profile"
    const noProfile: ProfileStripState = { kind: "no-profile" };
    rerender(
      <AccountScreen
        profile={noProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).toBeNull();

    // Test with kind="unavailable"
    const unavailable: ProfileStripState = { kind: "unavailable" };
    rerender(
      <AccountScreen
        profile={unavailable}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).toBeNull();
  });

  it("says the same words to a named player and a nameless one", () => {
    // First render with a named player
    const namedProfile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: false, displayName: "Bob" }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={namedProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).not.toBeNull();
    const namedCount = screen.getAllByText(ANONYMOUS_STATE).length;

    // Second render with an anonymous player (no displayName)
    const anonProfile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: false, displayName: null }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={anonProfile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).not.toBeNull();
    const anonCount = screen.getAllByText(ANONYMOUS_STATE).length;

    // The three sentences should appear exactly once in each render
    expect(namedCount).toBe(1);
    expect(anonCount).toBe(1);
  });

  it("never reads the session token for the state", () => {
    // First case: signedIn=true with hasPassword=false should SHOW the block
    const profile1: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: false }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen
        profile={profile1}
        signedIn={true}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).not.toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).not.toBeNull();

    // Second case: signedIn=false with hasPassword=true should HIDE the block
    const profile2: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasPassword: true }),
      duels: [],
    };
    rerender(
      <AccountScreen
        profile={profile2}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByText(ANONYMOUS_STATE)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_COST)).toBeNull();
    expect(screen.queryByText(ANONYMOUS_WAY_OUT)).toBeNull();
  });

  it("carries the name form when it is given one", () => {
    const setName = vi.fn<[string], Promise<SetNameOutcome>>();
    setName.mockResolvedValue({ kind: "named", profile: aProfile() });

    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
        setName={setName}
      />,
    );

    const nameSurface = screen.getByLabelText("your display name");
    expect(nameSurface).not.toBeNull();

    // Verify it is the immediate next sibling of the Account heading
    const heading = screen.getByRole("heading", {
      level: 2,
      name: ACCOUNT_HEADING,
    });
    const headingIndex = Array.from(
      screen.getByLabelText("account").children,
    ).indexOf(heading);
    const surfaceIndex = Array.from(
      screen.getByLabelText("account").children,
    ).indexOf(nameSurface);
    expect(surfaceIndex).toBe(headingIndex + 1);
  });

  it("carries no name form when it is given no setName", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      <AccountScreen
        profile={profile}
        signedIn={false}
        signOutHandsANewProfile={false}
      />,
    );

    const nameSurface = screen.queryByLabelText("your display name");
    expect(nameSurface).toBeNull();
  });

  it("carries no name form without a profile in hand", () => {
    const setName = vi.fn<[string], Promise<SetNameOutcome>>();
    setName.mockResolvedValue({ kind: "named", profile: aProfile() });

    // Test with profile=null (still loading)
    const { rerender } = render(
      <AccountScreen
        profile={null}
        signedIn={false}
        setName={setName}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByLabelText("your display name")).toBeNull();

    // Test with kind="loading" - but loading is not a kind in ProfileStripState,
    // so we only test the non-profile states
    const noProfile: ProfileStripState = { kind: "no-profile" };
    rerender(
      <AccountScreen
        profile={noProfile}
        signedIn={false}
        setName={setName}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByLabelText("your display name")).toBeNull();

    // Test with kind="unavailable"
    const unavailable: ProfileStripState = { kind: "unavailable" };
    rerender(
      <AccountScreen
        profile={unavailable}
        signedIn={false}
        setName={setName}
        signOutHandsANewProfile={false}
      />,
    );
    expect(screen.queryByLabelText("your display name")).toBeNull();
  });

  it("hands the sign-out control the answer it was given", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);

    // With signOutHandsANewProfile={false}, should show the warning
    const { rerender } = render(
      <AccountScreen
        profile={null}
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: SIGN_OUT_LABEL }));
    expect(screen.queryByText(SIGN_OUT_WARNING)).not.toBeNull();
    expect(screen.queryByText(SIGN_OUT_HANDS_A_NEW_PROFILE)).toBeNull();

    // With signOutHandsANewProfile={true}, should show the new profile message
    rerender(
      <AccountScreen
        profile={null}
        signedIn={true}
        signOutHandsANewProfile={true}
        signOut={signOut}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: SIGN_OUT_LABEL }));
    expect(screen.queryByText(SIGN_OUT_HANDS_A_NEW_PROFILE)).not.toBeNull();
    expect(screen.queryByText(SIGN_OUT_WARNING)).toBeNull();
  });

  // ADR-0135 §7: a component that can be mounted without this prop is a component whose
  // absence is invisible from the outside. This is a typechecker gate, not a runtime one — if
  // `signOutHandsANewProfile` is ever made optional, the omission below stops being an error and
  // the directive below stops being needed, so `tsc` fails on it going unused.
  it("cannot be mounted without being told which sign-out this is", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(
      // @ts-expect-error ADR-0135 §7: signOutHandsANewProfile is required, not optional.
      <AccountScreen profile={profile} signedIn={false} />,
    );

    expect(
      screen.getByRole("heading", { level: 2, name: ACCOUNT_HEADING }),
    ).not.toBeNull();
  });
});
