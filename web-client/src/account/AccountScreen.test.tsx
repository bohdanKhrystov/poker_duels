import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { AccountScreen } from "./AccountScreen";
import {
  ACCOUNT_HEADING,
  DEVICE_ROUTE_LIVE,
  DEVICE_ROUTE_REVOKED,
  PASSWORD_ROUTE_LIVE,
} from "./account-text";
import { RECOVERY_ON, RECOVERY_OFF } from "./recovery-text";
import { aProfile } from "../profile/profile-fixture";
import type { ProfileStripState } from "../profile/profile-strip";

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
});
