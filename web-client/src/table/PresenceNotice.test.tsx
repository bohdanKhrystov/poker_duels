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
    getByText("47s");
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
    getByText("44s");

    act(() => {
      vi.advanceTimersByTime(20_000);
    });
    getByText("24s");
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
    getByText("0s");

    act(() => {
      vi.advanceTimersByTime(120_000);
    });
    getByText("0s");
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

  it("the countdown is separated from the line it counts under", () => {
    vi.useFakeTimers();

    const { container } = render(
      <PresenceNotice
        presence="AWAY"
        returned={false}
        graceRemainingMillis={47_000}
      />,
    );

    // The rendered text should not contain "paused." immediately followed by a digit
    // (the bug was "paused.47" with no space)
    expect(container.textContent).not.toMatch(/paused\.\d/);
  });

  it("the countdown carries the numeral shape ADR-0046 fixes", () => {
    vi.useFakeTimers();

    const { getByText } = render(
      <PresenceNotice
        presence="AWAY"
        returned={false}
        graceRemainingMillis={47_000}
      />,
    );

    // First value: 47s
    getByText("47s");

    // Advance time to get a different value
    act(() => {
      vi.advanceTimersByTime(3_000);
    });

    // Second value: 44s - confirms the format is consistent across different values
    getByText("44s");
  });
});
