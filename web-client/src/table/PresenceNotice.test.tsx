import { act, render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PresenceNotice } from "./PresenceNotice";

describe("the presence notice", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("says the duel is paused and starts from the number the frame carried", () => {
    vi.useFakeTimers();

    // 47 000 on purpose: the server's default window is 60 000, so a countdown seeded from a
    // constant reads 60 and fails here.
    const { getByText } = render(
      <PresenceNotice
        presence="AWAY"
        returned={false}
        graceRemainingMillis={47_000}
      />,
    );

    getByText("Your rival is away. The duel is paused.");
    getByText("47");
  });

  it("counts down as time passes", () => {
    vi.useFakeTimers();

    const { getByText } = render(
      <PresenceNotice
        presence="AWAY"
        returned={false}
        graceRemainingMillis={47_000}
      />,
    );

    // Two advances, because one is satisfied by a component that subtracts a constant.
    act(() => {
      vi.advanceTimersByTime(3_000);
    });
    getByText("44");

    act(() => {
      vi.advanceTimersByTime(20_000);
    });
    getByText("24");
  });

  it("holds at zero, and says nothing new there", () => {
    vi.useFakeTimers();

    const { container, getByText } = render(
      <PresenceNotice
        presence="AWAY"
        returned={false}
        graceRemainingMillis={47_000}
      />,
    );

    act(() => {
      vi.advanceTimersByTime(120_000);
    });
    getByText("0");

    act(() => {
      vi.advanceTimersByTime(120_000);
    });
    getByText("0");
    getByText("Your rival is away. The duel is paused.");
    expect(container.textContent).not.toMatch(
      /expired|time.s up|too late|gone/i,
    );
  });

  it("counts nothing once the window has run out", () => {
    const { container, getByText } = render(
      <PresenceNotice
        presence="ABSENT"
        returned={false}
        graceRemainingMillis={null}
      />,
    );

    getByText(
      "Your rival did not come back. The duel continues, and the server acts for them.",
    );
    expect(container.textContent).not.toMatch(/\d/);
  });

  it("says nothing at all to a client whose rival never left", () => {
    const { container } = render(
      <PresenceNotice
        presence="PRESENT"
        returned={false}
        graceRemainingMillis={null}
      />,
    );

    expect(container.textContent).toBe("");
    expect(container.textContent).not.toMatch(/\d/);
  });

  it("says the rival is back to a client that saw them go", () => {
    const { container, getByText } = render(
      <PresenceNotice
        presence="PRESENT"
        returned={true}
        graceRemainingMillis={null}
      />,
    );

    getByText("Your rival is back.");
    expect(container.textContent).not.toMatch(/\d/);
  });
});
