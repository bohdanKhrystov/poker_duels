import { describe, it, expect, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
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
    // Both clicks inside one `act`: it is re-entrant and flushes only when
    // the outermost call exits, so the state from the first click has not
    // committed while the second is dispatched — the button is still
    // enabled, `handleSubmit` runs a second time, and
    // `submitInFlight.current` is the only thing that stops it sending
    // twice. Two bare `fireEvent.click` calls would each flush in their own
    // `act` and hide the ref entirely.
    act(() => {
      fireEvent.click(firstButton);
      fireEvent.click(firstButton);
    });

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
    act(() => {
      fireEvent.click(secondButton);
      fireEvent.click(secondButton);
    });

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

  it("gives every refusal its own sentence, from one render each", async () => {
    // Literals, not `refusalSentence(kind)`: an assertion against the
    // function under test would compare its output to itself and pass no
    // matter what the sentence said. The golden copies live in
    // name-text.test.ts; these are retyped here on purpose.
    const cases: ReadonlyArray<{
      kind: Exclude<SetNameOutcome["kind"], "named">;
      typed: string | null;
      sentence: string;
    }> = [
      {
        kind: "rejected",
        // TASK-041110 forbids a client-side empty-name guard: this field is
        // left untouched — not typed into, not cleared, just submitted as
        // it starts — so a silently reintroduced guard, which would swallow
        // the click and never call `setName`, fails this test instead of
        // failing nothing. `null` means "do not fire a change event at all".
        typed: null,
        sentence: "That name cannot be used. Try another.",
      },
      {
        kind: "conflict",
        typed: "Ada",
        sentence: "That name is not available. Try another.",
      },
      {
        kind: "permanent",
        typed: "Ada",
        sentence:
          "You already have a display name. That choice is permanent and cannot be changed.",
      },
      {
        kind: "no-profile",
        typed: "Ada",
        sentence: "This browser has no profile. Reload the page and try again.",
      },
      {
        kind: "unavailable",
        typed: "Ada",
        sentence:
          "That did not reach the server. Reload the page to see whether the name was set.",
      },
    ];

    render(
      <>
        {cases.map(({ kind }) => (
          <NameSurface
            key={kind}
            profile={aProfile({ displayName: null })}
            setName={() => Promise.resolve({ kind })}
          />
        ))}
      </>,
    );

    const textboxes = screen.getAllByRole("textbox");
    const buttons = screen.getAllByRole("button");
    cases.forEach(({ typed }, i) => {
      // `null` leaves this particular field exactly as it was rendered —
      // no `fireEvent.change` at all — so the empty string that reaches
      // `setName` is the field's own starting value, not one this test typed.
      if (typed !== null) {
        fireEvent.change(textboxes[i], { target: { value: typed } });
      }
      fireEvent.click(buttons[i]);
    });

    for (const { sentence } of cases) {
      expect(await screen.findByText(sentence)).toBeDefined();
    }

    // Scoped to the refusal sentences themselves, not the whole screen:
    // `PERMANENCE_LINE` legitimately says a name "can be taken away", a
    // different sense that a page-wide check would wrongly indict.
    const statusTexts = screen
      .getAllByRole("status")
      .map((element) => element.textContent ?? "");
    expect(statusTexts).toHaveLength(cases.length);
    for (const text of statusTexts) {
      expect(text.toLowerCase()).not.toContain("taken");
    }
  });

  it("keeps the form for the two refusals a player can act on, and takes it away for the rest", async () => {
    const staysWithWhatWasTyped: ReadonlyArray<
      Exclude<SetNameOutcome["kind"], "named">
    > = ["rejected", "conflict"];
    const goesAway: ReadonlyArray<Exclude<SetNameOutcome["kind"], "named">> = [
      "permanent",
      "no-profile",
      "unavailable",
    ];

    for (const kind of staysWithWhatWasTyped) {
      const { unmount } = render(
        <NameSurface
          profile={aProfile({ displayName: null })}
          setName={() => Promise.resolve({ kind })}
        />,
      );

      fireEvent.change(screen.getByRole("textbox"), {
        target: { value: "Ada" },
      });
      fireEvent.click(screen.getByRole("button"));

      // The settled request re-enables the button; the form that does that
      // is the same form that must have survived the refusal.
      await waitFor(() => {
        expect((screen.getByRole("button") as HTMLButtonElement).disabled).toBe(
          false,
        );
      });

      const textbox = screen.getByRole("textbox") as HTMLInputElement;
      expect(textbox.value).toBe("Ada");
      expect(screen.getAllByRole("button")).toHaveLength(1);

      unmount();
    }

    for (const kind of goesAway) {
      const { unmount } = render(
        <NameSurface
          profile={aProfile({ displayName: null })}
          setName={() => Promise.resolve({ kind })}
        />,
      );

      fireEvent.change(screen.getByRole("textbox"), {
        target: { value: "Ada" },
      });
      fireEvent.click(screen.getByRole("button"));

      await waitFor(() => {
        expect(screen.queryAllByRole("textbox")).toHaveLength(0);
      });
      expect(screen.queryAllByRole("button")).toHaveLength(0);

      unmount();
    }
  });
});
