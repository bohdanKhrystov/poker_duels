import { fireEvent, render } from "@testing-library/react";
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

  it("clamps the amount control to the bounds the server sent", () => {
    const { getByRole } = bar();
    const slider = getByRole("slider", { name: "raise to" });
    expect(slider.getAttribute("min")).toBe("1200");
    expect(slider.getAttribute("max")).toBe("13400");

    const { getByRole: getByRole2 } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          minBetTo: 2750,
          allInTo: 9100,
          allowed: ["CHECK", "BET", "ALL_IN"],
        }),
      }),
    });
    const slider2 = getByRole2("slider", { name: "bet to" });
    expect(slider2.getAttribute("min")).toBe("2750");
    expect(slider2.getAttribute("max")).toBe("9100");
  });

  it("starts the amount at the server's minimum for the action it allowed", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const slider = getByRole("slider", { name: "raise to" });
    expect(slider.getAttribute("value")).toBe("1200");
    expect(slider.getAttribute("min")).toBe("1200");
    expect(slider.getAttribute("max")).toBe("13400");

    const { getByRole: getByRole2 } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          minBetTo: 350,
          allInTo: 8200,
          allowed: ["CHECK", "BET", "ALL_IN"],
        }),
      }),
    });

    const slider2 = getByRole2("slider", { name: "bet to" });
    expect(slider2.getAttribute("value")).toBe("350");
    expect(slider2.getAttribute("min")).toBe("350");
    expect(slider2.getAttribute("max")).toBe("8200");
  });

  it("offers no amount control when neither a bet nor a raise is allowed", () => {
    const { queryByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "ALL_IN"],
        }),
      }),
    });

    const slider = queryByRole("slider");
    expect(slider).toBeNull();
  });

  it("writes the raise button's total from the amount control", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const slider = getByRole("slider", { name: "raise to" });
    fireEvent.change(slider, { target: { value: "3250" } });

    const raiseButton = getByRole("button", { name: "Raise to 3,250" });
    expect(raiseButton).toBeDefined();
  });

  it("sends one Act carrying the turn's identity", () => {
    const { getByRole, send } = bar();

    fireEvent.click(getByRole("button", { name: "Fold" }));

    expect(send).toHaveBeenCalledOnce();
    expect(send).toHaveBeenCalledWith({
      type: "Act",
      handNumber: 14,
      actionSequence: 27,
      action: { type: "Fold", seat: 0 },
    });
  });

  it("sends the total the amount control holds", () => {
    const { getByRole, send } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const slider = getByRole("slider", { name: "raise to" });
    fireEvent.change(slider, { target: { value: "3250" } });
    fireEvent.click(getByRole("button", { name: "Raise to 3,250" }));

    expect(send).toHaveBeenCalledWith({
      type: "Act",
      handNumber: 14,
      actionSequence: 27,
      action: { type: "Raise", seat: 0, to: 3250 },
    });
  });

  it("disables every control once an action is sent", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    fireEvent.click(getByRole("button", { name: "Fold" }));

    const slider = getByRole("slider", {
      name: "raise to",
    }) as HTMLInputElement;
    expect(slider.disabled).toBe(true);

    const buttons = getByRole("button", {
      name: "Fold",
    }).parentElement?.querySelectorAll("button");
    buttons?.forEach((button) => {
      expect((button as HTMLButtonElement).disabled).toBe(true);
    });
  });

  it("sends nothing more once an action is sent", () => {
    const { getByRole, send } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    fireEvent.click(getByRole("button", { name: "Fold" }));
    fireEvent.click(getByRole("button", { name: "Call 400" }));

    expect(send).toHaveBeenCalledTimes(1);
  });

  it("comes back to life on the next turn, at the new minimum", () => {
    const send = vi.fn();
    const { rerender, getByRole } = render(
      <ActionBar turn={aTurn()} send={send} />,
    );
    fireEvent.click(getByRole("button", { name: "Fold" }));

    rerender(
      <ActionBar
        turn={aTurn({
          actionSequence: 28,
          legalActions: aLegalActions({ minRaiseTo: 2400 }),
        })}
        send={send}
      />,
    );

    expect(
      (getByRole("slider", { name: "raise to" }) as HTMLInputElement).value,
    ).toBe("2400");
    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenCalledTimes(2);
  });
});
