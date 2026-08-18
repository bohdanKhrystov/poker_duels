import { describe, it, expect, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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

  it("tells the player their name was removed, and tells the player beside them nothing", () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>();

    render(
      <>
        <NameSurface
          profile={aProfile({ displayName: null, displayNameRemoved: true })}
          setName={setNameSpy}
        />
        <NameSurface
          profile={aProfile({ displayName: null, displayNameRemoved: false })}
          setName={setNameSpy}
        />
      </>,
    );

    const headings = screen.getAllByText("Your display name was removed.");
    expect(headings).toHaveLength(1);

    const bodyTexts = screen.getAllByText(
      /A person running Poker Duels removed it/,
    );
    expect(bodyTexts).toHaveLength(1);

    const textboxes = screen.getAllByRole("textbox");
    expect(textboxes).toHaveLength(2);
  });

  it("keeps the notice out of the way of choosing again", () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>();

    const { rerender } = render(
      <NameSurface
        profile={aProfile({ displayName: null, displayNameRemoved: true })}
        setName={setNameSpy}
      />,
    );

    const noticeElement = screen.getByText("Your display name was removed.");
    const textbox = screen.getByRole("textbox");
    expect(noticeElement.compareDocumentPosition(textbox)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );

    const alerts = screen.queryAllByRole("alert");
    expect(alerts).toHaveLength(0);

    const renderedText =
      screen.getByText("Your display name was removed.").parentElement
        ?.parentElement?.textContent || "";
    expect(renderedText).not.toContain("banned");
    expect(renderedText).not.toContain("suspended");
    expect(renderedText).not.toContain("violation");
    expect(renderedText).not.toContain("report");
    expect(renderedText).not.toContain("appeal");

    // Test the impossible pair: displayName is set but displayNameRemoved is true
    rerender(
      <NameSurface
        profile={aProfile({ displayName: "Ada", displayNameRemoved: true })}
        setName={setNameSpy}
      />,
    );

    expect(screen.getByText("Ada")).toBeDefined();
    expect(
      screen.queryAllByText("Your display name was removed."),
    ).toHaveLength(0);
  });

  it("sends what the player typed, once, however many times the button is pressed", async () => {
    // Resolved after the in-flight assertions below: this test also checks
    // that the button re-enables once the request settles, not only that it
    // sends once while it doesn't.
    const pending: Array<(outcome: SetNameOutcome) => void> = [];
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>(
      () =>
        new Promise<SetNameOutcome>((resolve) => {
          pending.push(resolve);
        }),
    );

    const { unmount } = render(
      <NameSurface
        profile={aProfile({ displayName: null })}
        setName={setNameSpy}
      />,
    );

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "  Ada  " },
    });
    const firstButton = screen.getByRole("button") as HTMLButtonElement;
    fireEvent.click(firstButton);
    fireEvent.click(firstButton);

    expect(setNameSpy).toHaveBeenCalledTimes(1);
    expect(setNameSpy).toHaveBeenCalledWith("  Ada  ");
    // Disabled the instant a request is in flight — a button that still
    // looked pressable would invite the second press this guard exists for.
    expect(firstButton.disabled).toBe(true);

    pending[0]({ kind: "unavailable" });
    await waitFor(() => {
      expect(firstButton.disabled).toBe(false);
    });

    unmount();

    render(
      <NameSurface
        profile={aProfile({ displayName: null })}
        setName={setNameSpy}
      />,
    );

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "Grace" },
    });
    const secondButton = screen.getByRole("button");
    fireEvent.click(secondButton);
    fireEvent.click(secondButton);

    expect(setNameSpy).toHaveBeenCalledTimes(2);
    expect(setNameSpy).toHaveBeenNthCalledWith(2, "Grace");
    expect(pending).toHaveLength(2);
  });

  it("shows the name the server canonicalised, never the one that was typed", async () => {
    const setNameSpy = vi.fn<[string], Promise<SetNameOutcome>>(() =>
      Promise.resolve({
        kind: "named",
        profile: aProfile({ displayName: "Ada" }),
      }),
    );

    render(
      <NameSurface
        profile={aProfile({ displayName: null })}
        setName={setNameSpy}
      />,
    );

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "  ada  " },
    });
    fireEvent.click(screen.getByRole("button"));

    expect(await screen.findByText("Ada")).toBeDefined();
    expect(screen.queryAllByRole("textbox")).toHaveLength(0);

    const renderedText =
      screen.getByRole("region", { name: "your display name" }).textContent ??
      "";
    expect(renderedText).not.toContain("  ada  ");
    expect(screen.queryByText(/^ada$/)).toBeNull();
  });
});
