import { render } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ActionBar } from "./ActionBar";
import { aTurn, aLegalActions } from "./turn-fixture";

describe("the action bar", () => {
  function bar(props: Partial<Parameters<typeof ActionBar>[0]> = {}) {
    const send = vi.fn();
    const rendered = render(
      <ActionBar
        turn={props.turn === undefined ? aTurn() : props.turn}
        send={props.send ?? send}
      />,
    );
    return { ...rendered, send };
  }

  it("names itself and waits when there is no turn", () => {
    const { getByRole, getByText } = bar({ turn: null });

    const region = getByRole("region", { name: "your move" });
    expect(region).toBeDefined();

    const waitingText = getByText("Waiting for your rival…");
    expect(waitingText).toBeDefined();
  });

  it("offers no control when there is no turn", () => {
    const { queryAllByRole, queryByRole } = bar({ turn: null });

    const buttons = queryAllByRole("button");
    expect(buttons).toEqual([]);

    const slider = queryByRole("slider");
    expect(slider).toBeNull();
  });

  it("renders one button per action the server allowed, in the order it sent them", () => {
    const { getAllByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const buttons = getAllByRole("button");
    const texts = buttons.map((b) => b.textContent);
    expect(texts).toEqual([
      "Fold",
      "Call 400",
      "Raise to 1,200",
      "All in 13,400",
    ]);
  });

  it("renders no button for an action the server withheld", () => {
    const { queryAllByRole, queryByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE"],
        }),
      }),
    });

    const buttons = queryAllByRole("button");
    expect(buttons).toHaveLength(3);

    const checkButton = queryByRole("button", { name: "Check" });
    expect(checkButton).toBeNull();
  });

  it("fills the raise and leaves the other buttons ghosts", () => {
    const { getAllByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const buttons = getAllByRole("button");
    const raiseButton = buttons[2]; // "Raise to 1,200"
    const foldButton = buttons[0]; // "Fold"

    expect(raiseButton.className).toContain("bg-accent-fill");
    expect(foldButton.className).not.toContain("bg-accent-fill");
  });

  it("fills the last button when the server offers no bet and no raise", () => {
    const { getAllByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "ALL_IN"],
        }),
      }),
    });

    const buttons = getAllByRole("button");
    const allInButton = buttons[2]; // "All in 13,400" - the last button

    expect(allInButton.textContent).toBe("All in 13,400");
    expect(allInButton.className).toContain("bg-accent-fill");
  });
});
