import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NameSurface } from "./NameSurface";
import { aProfile } from "./profile-fixture";
import type { SetNameOutcome } from "./set-name";
import { PERMANENCE_LINE } from "./name-text";

describe("the name surface", () => {
  it("shows the name the server sent, and offers no way to change it", () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>();

    const { unmount } = render(
      <NameSurface
        profile={aProfile({ displayName: "Ada" })}
        setName={setNameSpy}
      />,
    );

    expect(screen.getByText("Ada")).toBeDefined();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryAllByRole("button")).toHaveLength(0);

    unmount();

    render(
      <NameSurface
        profile={aProfile({ displayName: "Grace" })}
        setName={setNameSpy}
      />,
    );

    expect(screen.getByText("Grace")).toBeDefined();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);
    expect(screen.queryAllByRole("button")).toHaveLength(0);
  });

  it("offers the form to a player who has no name", () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>();

    render(
      <NameSurface
        profile={aProfile({ displayName: null })}
        setName={setNameSpy}
      />,
    );

    const textboxes = screen.queryAllByRole("textbox");
    expect(textboxes).toHaveLength(1);
    expect((textboxes[0] as HTMLInputElement).value).toBe("");

    const buttons = screen.queryAllByRole("button");
    expect(buttons).toHaveLength(1);
    expect(buttons[0].textContent).toBe("Set my name");
  });

  it("says the choice is permanent before anything is sent", () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>();

    render(
      <NameSurface
        profile={aProfile({ displayName: null })}
        setName={setNameSpy}
      />,
    );

    const permanenceLineElement = screen.getByText(PERMANENCE_LINE);
    expect(permanenceLineElement).toBeDefined();

    const textbox = screen.getByRole("textbox");
    expect(permanenceLineElement.compareDocumentPosition(textbox)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );

    expect(setNameSpy).not.toHaveBeenCalled();
  });
});
