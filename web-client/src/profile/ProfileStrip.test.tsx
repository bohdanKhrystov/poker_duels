import { describe, it, expect } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { ProfileStrip } from "./ProfileStrip";

describe("the profile strip", () => {
  it("states the balance the server sent", () => {
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p1", coinBalance: 7 },
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("7 Duel coins")).toBeDefined();

    // Test negative balance: −1 (U+2212 MINUS SIGN)
    cleanup();
    render(
      <ProfileStrip
        state={{
          kind: "profile",
          profile: { playerId: "p2", coinBalance: -1 },
          duels: [],
        }}
      />,
    );

    expect(screen.getByText("−1 Duel coins")).toBeDefined();
  });

  it("says there is no profile yet, and raises no alarm", () => {
    render(<ProfileStrip state={{ kind: "no-profile" }} />);

    expect(screen.getByText("No profile yet.")).toBeDefined();
    expect(screen.queryAllByRole("alert")).toHaveLength(0);
    expect(
      screen.getByText("No profile yet.").textContent?.toLowerCase(),
    ).not.toContain("error");
  });

  it("renders nothing at all when the read did not land", () => {
    const { container } = render(
      <ProfileStrip state={{ kind: "unavailable" }} />,
    );

    expect(container.innerHTML).toBe("");
  });
});
