import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { NAME_VOCABULARY } from "./name-vocabulary";
import { NameAsk } from "./NameAsk";
import { ProfileProvider, useProfileStrip } from "./profile-provider";
import type { PlayerProfile } from "./profile";
import type { SetNameOutcome } from "./set-name";

/** Turns a fixed sequence of `[0, 1)` draws into the `random` source `suggestName` takes. */
function scripted(values: readonly number[]): () => number {
  let call = 0;
  return () => values[call++ % values.length];
}

const NAMED_PROFILE: PlayerProfile = {
  playerId: "p1",
  coinBalance: 0,
  displayName: "Quiet Iron Raven",
  displayNameRemoved: false,
  deviceRouteLive: true,
  hasRecoveryEmail: false,
  hasPassword: false,
};

describe("the name ask", () => {
  it("puts a drawn suggestion in the field", () => {
    const firstDraws = [0, 0, 0];
    const first = NAME_VOCABULARY.map((list) => list[0]).join(" ");

    render(
      <NameAsk
        setName={vi.fn()}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={scripted(firstDraws)}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    expect(field.value).toBe(first);

    const secondDraws = [1, 1, 1];
    const second = NAME_VOCABULARY.map((list) => list[list.length - 1]).join(
      " ",
    );

    render(
      <NameAsk
        setName={vi.fn()}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={scripted(secondDraws)}
      />,
    );

    const fields = screen.getAllByLabelText("Your name") as HTMLInputElement[];
    expect(fields[1].value).toBe(second);
    expect(first).not.toBe(second);
  });

  it("keeps what the player types", () => {
    render(
      <NameAsk
        setName={vi.fn()}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={scripted([0, 0, 0])}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    const drawn = field.value;

    fireEvent.change(field, { target: { value: "Ironclad" } });
    expect(field.value).toBe("Ironclad");
    expect(field.value).not.toBe(drawn);

    // Nothing besides the player's own typing writes to the field again — a later render of the
    // same tree must not restore the draw.
    fireEvent.change(field, { target: { value: "Ironclad " } });
    expect(field.value).toBe("Ironclad ");
  });

  it("tells the caller only once the server has said yes", async () => {
    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);
    const onNamed = vi.fn();
    const onSkip = vi.fn();

    render(
      <NameAsk
        setName={setName}
        onNamed={onNamed}
        onSkip={onSkip}
        random={scripted([0, 0, 0])}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    fireEvent.change(field, { target: { value: "Ironclad" } });
    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));

    expect(setName).toHaveBeenCalledTimes(1);
    expect(setName).toHaveBeenCalledWith("Ironclad");
    expect(onNamed).not.toHaveBeenCalled();

    settle({ kind: "named", profile: NAMED_PROFILE });

    await waitFor(() => {
      expect(onNamed).toHaveBeenCalledTimes(1);
    });
    expect(onSkip).not.toHaveBeenCalled();
  });

  it("sends nothing at all when the player skips", () => {
    const setName = vi.fn();
    const onSkip = vi.fn();

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={onSkip}
        random={scripted([0, 0, 0])}
      />,
    );

    fireEvent.click(
      screen.getByRole("button", { name: "Duel without a name" }),
    );

    expect(onSkip).toHaveBeenCalledTimes(1);
    expect(setName).toHaveBeenCalledTimes(0);
  });

  it("reads a refusal in the words the product already uses", async () => {
    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);
    const onNamed = vi.fn();

    render(
      <NameAsk
        setName={setName}
        onNamed={onNamed}
        onSkip={vi.fn()}
        random={scripted([0, 0, 0])}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "conflict" });

    await waitFor(() => {
      const statusText = screen.getByRole("status").textContent;
      expect(statusText).toBe("That name is not available. Try another.");
    });
    expect(onNamed).not.toHaveBeenCalled();
  });

  it("withdraws the form a refusal has closed, and leaves the skip standing", async () => {
    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);
    const onSkip = vi.fn();

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={onSkip}
        random={scripted([0, 0, 0])}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "unavailable" });

    await waitFor(() => {
      const textboxes = screen.queryAllByRole("textbox");
      expect(textboxes).toHaveLength(0);
    });

    fireEvent.click(
      screen.getByRole("button", { name: "Duel without a name" }),
    );
    expect(onSkip).toHaveBeenCalledTimes(1);
  });

  it("sends one write for two presses", () => {
    const answer = new Promise<SetNameOutcome>(() => {
      // never settle
    });
    const setName = vi.fn(() => answer);

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={scripted([0, 0, 0])}
      />,
    );

    const submitButton = screen.getByRole("button", {
      name: "Take this name",
    }) as HTMLButtonElement;
    // Both clicks inside one `act`: it is re-entrant and flushes only when
    // the outermost call exits, so the state from the first click has not
    // committed while the second is dispatched — the button is still
    // enabled, `handleSubmit` runs a second time, and
    // `submitInFlight.current` is the only thing that stops it sending
    // twice.
    act(() => {
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
    });

    expect(setName).toHaveBeenCalledTimes(1);
  });

  it("carries the accepted name to the profile the client holds", async () => {
    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);
    const namedProfile: PlayerProfile = {
      playerId: "p1",
      coinBalance: 0,
      displayName: "Ravenpost",
      displayNameRemoved: false,
      deviceRouteLive: true,
      hasRecoveryEmail: false,
      hasPassword: false,
    };

    function ProfileConsumer(): React.ReactElement {
      const strip = useProfileStrip();
      return (
        <div>
          {strip?.kind === "profile" && <p>{strip.profile.displayName}</p>}
        </div>
      );
    }

    render(
      <ProfileProvider
        read={async () => ({
          kind: "profile",
          profile: {
            playerId: "p1",
            coinBalance: 0,
            displayName: null,
            displayNameRemoved: false,
            deviceRouteLive: true,
            hasRecoveryEmail: false,
            hasPassword: false,
          },
          duels: [],
        })}
      >
        <NameAsk
          setName={setName}
          onNamed={vi.fn()}
          onSkip={vi.fn()}
          random={scripted([0, 0, 0])}
        />
        <ProfileConsumer />
      </ProfileProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "named", profile: namedProfile });

    await waitFor(() => {
      expect(screen.getByText("Ravenpost")).toBeDefined();
    });
  });

  it("puts a fresh suggestion in a field the player never touched", async () => {
    // The first `NAME_VOCABULARY.length` draws give every list's first word (tuple A, the
    // initial suggestion); the next `NAME_VOCABULARY.length` give every list's last word
    // (tuple B, what the redraw must produce) — the same construction the first test in this
    // file uses to get two suggestions guaranteed to differ.
    const values = [
      ...Array(NAME_VOCABULARY.length).fill(0),
      ...Array(NAME_VOCABULARY.length).fill(1),
    ];
    const random = vi.fn(scripted(values));
    const a = NAME_VOCABULARY.map((list) => list[0]).join(" ");
    const b = NAME_VOCABULARY.map((list) => list[list.length - 1]).join(" ");
    expect(b).not.toBe(a);

    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={random}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    expect(field.value).toBe(a);

    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "conflict" });

    await waitFor(() => {
      expect(field.value).toBe(b);
    });
    expect(field.value).not.toBe(a);
  });

  it("never overwrites a name the player typed", async () => {
    const values = [
      ...Array(NAME_VOCABULARY.length).fill(0),
      ...Array(NAME_VOCABULARY.length).fill(1),
    ];
    const random = vi.fn(scripted(values));

    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={random}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    const callsAfterMount = random.mock.calls.length;

    fireEvent.change(field, { target: { value: "Ravenpost" } });
    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "conflict" });

    await waitFor(() => {
      const statusText = screen.getByRole("status").textContent;
      expect(statusText).toBe("That name is not available. Try another.");
    });

    expect(field.value).toBe("Ravenpost");
    expect(random.mock.calls.length).toBe(callsAfterMount);
  });

  it("redraws on a conflict and on nothing else", async () => {
    const values = [
      ...Array(NAME_VOCABULARY.length).fill(0),
      ...Array(NAME_VOCABULARY.length).fill(1),
    ];
    const random = vi.fn(scripted(values));
    const a = NAME_VOCABULARY.map((list) => list[0]).join(" ");

    let settle: (outcome: SetNameOutcome) => void = () => {};
    const answer = new Promise<SetNameOutcome>((resolve) => {
      settle = resolve;
    });
    const setName = vi.fn(() => answer);

    render(
      <NameAsk
        setName={setName}
        onNamed={vi.fn()}
        onSkip={vi.fn()}
        random={random}
      />,
    );

    const field = screen.getByLabelText("Your name") as HTMLInputElement;
    expect(field.value).toBe(a);
    const callsAfterMount = random.mock.calls.length;

    fireEvent.click(screen.getByRole("button", { name: "Take this name" }));
    settle({ kind: "unavailable" });

    await waitFor(() => {
      const textboxes = screen.queryAllByRole("textbox");
      expect(textboxes).toHaveLength(0);
    });

    expect(field.value).toBe(a);
    expect(random.mock.calls.length).toBe(callsAfterMount);
  });
});
