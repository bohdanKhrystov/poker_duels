import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { ProfileProvider, useProfileStrip } from "./profile-provider";
import type { ProfileStripState } from "./profile-strip";
import { aProfile } from "./profile-fixture";

/** Renders whatever `useProfileStrip()` answers, in text a test can find. */
function Consumer(): ReactElement {
  const state = useProfileStrip();
  if (state === null) {
    return <p>no answer yet</p>;
  }
  if (state.kind === "profile") {
    return <p>{`balance ${state.profile.coinBalance}`}</p>;
  }
  return <p>{state.kind}</p>;
}

describe("the profile provider", () => {
  it("hands down whatever the read answered", async () => {
    const profileAnswer: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ coinBalance: 7 }),
      duels: [],
    };
    const { unmount } = render(
      <ProfileProvider read={() => Promise.resolve(profileAnswer)}>
        <Consumer />
      </ProfileProvider>,
    );
    await screen.findByText("balance 7");
    unmount();

    render(
      <ProfileProvider read={() => Promise.resolve({ kind: "no-profile" })}>
        <Consumer />
      </ProfileProvider>,
    );
    await screen.findByText("no-profile");
  });

  it("hands down nothing until the read lands", () => {
    // A promise that never settles within the test: the read has started
    // but has not landed, so this assertion is safe without any await.
    const read = (): Promise<ProfileStripState> => new Promise(() => {});

    render(
      <ProfileProvider read={read}>
        <Consumer />
      </ProfileProvider>,
    );

    expect(screen.getByText("no answer yet")).toBeDefined();
  });

  it("answers null where no provider is above it", () => {
    render(<Consumer />);

    expect(screen.getByText("no answer yet")).toBeDefined();
  });

  it("reads once for one mount, and not again on a re-render", async () => {
    const answer: ProfileStripState = { kind: "no-profile" };
    const read = vi.fn(() => Promise.resolve(answer));

    const { rerender } = render(
      <ProfileProvider read={read}>
        <Consumer />
      </ProfileProvider>,
    );
    await screen.findByText("no-profile");

    rerender(
      <ProfileProvider read={read}>
        <Consumer />
      </ProfileProvider>,
    );

    expect(read).toHaveBeenCalledTimes(1);
  });
});
