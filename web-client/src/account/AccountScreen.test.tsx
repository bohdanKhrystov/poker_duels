import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AccountScreen } from "./AccountScreen";
import {
  ACCOUNT_HEADING,
  CANCEL,
  DEVICE_ROUTE_LIVE,
  DEVICE_ROUTE_REVOKED,
  PASSWORD_ROUTE_LIVE,
  SIGN_IN_HEADING,
  SIGN_OUT_LABEL,
} from "./account-text";
import { RECOVERY_ON, RECOVERY_OFF, ATTACH_LABEL } from "./recovery-text";
import { aProfile } from "../profile/profile-fixture";
import type { ProfileStripState } from "../profile/profile-strip";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";

describe("the account screen", () => {
  it("says the device signs in, and says it stopped, from the server fact alone", () => {
    const live: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ deviceRouteLive: true }),
      duels: [],
    };
    const { rerender } = render(
      <AccountScreen profile={live} signedIn={false} />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).not.toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();

    const revoked: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ deviceRouteLive: false }),
      duels: [],
    };
    rerender(<AccountScreen profile={revoked} signedIn={false} />);
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).not.toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
  });

  it("says the password signs in to a browser holding a session", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(<AccountScreen profile={profile} signedIn={true} />);

    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).not.toBeNull();
  });

  it("says nothing about a password route to a browser holding no session", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile(),
      duels: [],
    };
    render(<AccountScreen profile={profile} signedIn={false} />);

    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).not.toBeNull();
  });

  it("asserts no route of its own while the profile has not landed", () => {
    render(<AccountScreen profile={null} signedIn={false} />);

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
      <AccountScreen profile={unavailable} signedIn={false} />,
    );
    expect(screen.queryByText(DEVICE_ROUTE_LIVE)).toBeNull();
    expect(screen.queryByText(DEVICE_ROUTE_REVOKED)).toBeNull();
    expect(screen.queryByText(PASSWORD_ROUTE_LIVE)).toBeNull();

    const noProfile: ProfileStripState = { kind: "no-profile" };
    rerender(<AccountScreen profile={noProfile} signedIn={false} />);
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
    render(<AccountScreen profile={profile} signedIn={true} />);

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
      <AccountScreen profile={recoveryOn} signedIn={false} />,
    );
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const recoveryOff: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: false }),
      duels: [],
    };
    rerender(<AccountScreen profile={recoveryOff} signedIn={false} />);
    expect(screen.queryByText(RECOVERY_OFF)).not.toBeNull();
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
  });

  it("says nothing about recovery when no profile is in hand", () => {
    const noProfile: ProfileStripState = { kind: "no-profile" };
    const { rerender } = render(
      <AccountScreen profile={noProfile} signedIn={false} />,
    );
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const unavailable: ProfileStripState = { kind: "unavailable" };
    rerender(<AccountScreen profile={unavailable} signedIn={false} />);
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const nullProfile = null;
    rerender(<AccountScreen profile={nullProfile} signedIn={false} />);
    expect(screen.queryByText(RECOVERY_ON)).toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).toBeNull();

    const withProfile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    rerender(<AccountScreen profile={withProfile} signedIn={false} />);
    expect(screen.queryByText(RECOVERY_ON)).not.toBeNull();
  });

  it("renders no address, because it is given none and asks for none", () => {
    const profile: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ hasRecoveryEmail: true }),
      duels: [],
    };
    const { container } = render(
      <AccountScreen profile={profile} signedIn={false} />,
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
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.getByRole("button", { name: ATTACH_LABEL })).not.toBeNull();

    // With profile and no prop: form is absent.
    rerender(<AccountScreen profile={profile} signedIn={false} />);
    expect(screen.queryByRole("button", { name: ATTACH_LABEL })).toBeNull();

    // With prop and profile=null: form is absent.
    rerender(
      <AccountScreen
        profile={null}
        signedIn={false}
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
        attachRecoveryEmail={attach}
      />,
    );
    expect(screen.getByRole("button", { name: ATTACH_LABEL })).not.toBeNull();
    expect(screen.queryByText(RECOVERY_OFF)).not.toBeNull();
  });

  it("the sign-in door is a drawn button, not a sentence", () => {
    render(<AccountScreen profile={null} signedIn={false} />);

    const signIn = screen.getByRole("button", { name: SIGN_IN_HEADING });
    expect(signIn.classList.contains("border-hairline")).toBe(true);
    expect(signIn.classList.contains("px-5")).toBe(true);
  });

  it("sign out and its confirmation are drawn buttons, not sentences", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    render(<AccountScreen profile={null} signedIn={true} signOut={signOut} />);

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
});
