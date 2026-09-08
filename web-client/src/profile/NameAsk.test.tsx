import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { NAME_VOCABULARY } from "./name-vocabulary";
import { NameAsk } from "./NameAsk";
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
});
