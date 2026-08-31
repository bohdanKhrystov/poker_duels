import { fireEvent, render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import {
  ProfileProvider,
  useProfileStrip,
  useReportNameWrite,
} from "./profile-provider";
import type { ProfileStripState } from "./profile-strip";
import type { SetNameOutcome } from "./set-name";
import { aDuelLine, aProfile } from "./profile-fixture";

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

/**
 * Renders the held display name and a button that reports a name-write
 * outcome through `useReportNameWrite()` — the only way a test reaches that
 * function from outside the tree. `onRender`, if given, is called with the
 * held state on every render, so a test can watch for one that never comes.
 */
function NameConsumer(props: {
  outcome: SetNameOutcome;
  onRender?: (state: ProfileStripState | null) => void;
}): ReactElement {
  const state = useProfileStrip();
  const report = useReportNameWrite();
  props.onRender?.(state);
  const name =
    state?.kind === "profile"
      ? (state.profile.displayName ?? "no name")
      : "no answer yet";
  return (
    <div>
      <p>{name}</p>
      <button onClick={() => report(props.outcome)}>report</button>
    </div>
  );
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

  it("a name that was just set reaches the profile read", async () => {
    const initial: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ displayName: null }),
      duels: [aDuelLine()],
    };
    // The name a player might have typed. The server canonicalises before
    // it answers (`ADR-0029` §5), and `SetNameOutcome`'s own contract means
    // this string never reaches the provider — only the server's answer
    // does — so it is never passed to anything below; it exists only to be
    // proven absent from the render.
    const submitted = "larry";
    const outcome: SetNameOutcome = {
      kind: "named",
      profile: aProfile({ displayName: "Canonical Larry" }),
    };
    expect(outcome.profile.displayName).not.toBe(submitted);

    render(
      <ProfileProvider read={() => Promise.resolve(initial)}>
        <NameConsumer outcome={outcome} />
      </ProfileProvider>,
    );
    await screen.findByText("no name");

    fireEvent.click(screen.getByRole("button", { name: "report" }));

    expect(await screen.findByText("Canonical Larry")).toBeDefined();
    expect(screen.queryByText(submitted)).toBeNull();
  });

  it("a name the server refused leaves the held profile alone", async () => {
    const initial: ProfileStripState = {
      kind: "profile",
      profile: aProfile({ displayName: "Held Name" }),
      duels: [aDuelLine()],
    };
    const outcome: SetNameOutcome = { kind: "rejected" };
    const seen: (ProfileStripState | null)[] = [];

    render(
      <ProfileProvider read={() => Promise.resolve(initial)}>
        <NameConsumer
          outcome={outcome}
          onRender={(state) => seen.push(state)}
        />
      </ProfileProvider>,
    );
    await screen.findByText("Held Name");
    const rendersBeforeReport = seen.length;

    fireEvent.click(screen.getByRole("button", { name: "report" }));

    // A refusal triggers no `setState` at all: no later render was ever
    // recorded, and the held answer is still the exact fixture object the
    // read resolved with — not merely an equal-looking one.
    expect(seen.length).toBe(rendersBeforeReport);
    expect(seen[seen.length - 1]).toBe(initial);
  });
});
