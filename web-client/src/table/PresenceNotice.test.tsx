import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PresenceNotice } from "./PresenceNotice";

describe("the presence notice", () => {
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

  it("says the away line, and counts nothing", () => {
    const { container, getByText } = render(
      <PresenceNotice presence="AWAY" returned={false} />,
    );

    getByText("Your rival is away.");
    expect(container.textContent).not.toMatch(/\d/);
  });
});
