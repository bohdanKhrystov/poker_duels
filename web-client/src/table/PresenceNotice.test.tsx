import { render } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PresenceNotice } from "./PresenceNotice";

describe("the presence notice", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("counts nothing once the window has run out", () => {
    const { container, getByText } = render(
      <PresenceNotice presence="ABSENT" returned={false} />,
    );

    getByText(
      "Your rival did not come back. The duel continues, and the server acts for them.",
    );
    expect(container.textContent).not.toMatch(/\d/);
  });

  it("says nothing at all to a client whose rival never left", () => {
    const { container } = render(
      <PresenceNotice presence="PRESENT" returned={false} />,
    );

    expect(container.textContent).toBe("");
    expect(container.textContent).not.toMatch(/\d/);
  });

  it("says the rival is back to a client that saw them go", () => {
    const { container, getByText } = render(
      <PresenceNotice presence="PRESENT" returned={true} />,
    );

    getByText("Your rival is back.");
    expect(container.textContent).not.toMatch(/\d/);
  });

  it("the countdown is separated from the line it counts under", () => {
    vi.useFakeTimers();

    const { container } = render(
      <PresenceNotice presence="AWAY" returned={false} />,
    );

    // The rendered text should not contain "paused." immediately followed by a digit
    // (the bug was "paused.47" with no space)
    expect(container.textContent).not.toMatch(/paused\.\d/);
  });
});
