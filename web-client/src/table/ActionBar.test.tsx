import { fireEvent, render, within } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ActionBar } from "./ActionBar";
import { aTurn, aLegalActions } from "./turn-fixture";

describe("the action bar", () => {
  function bar(props: Partial<Parameters<typeof ActionBar>[0]> = {}) {
    const send = vi.fn();
    const rendered = render(
      <ActionBar
        turn={props.turn === undefined ? aTurn() : props.turn}
        // Frame A (`ADR-0101` §4) — a real hero-turn frame, not a bare zero,
        // so a test that renders through this helper without naming its own
        // pot still exercises a full sizing row rather than an empty one.
        potIncludingStreet={props.potIncludingStreet ?? 2850}
        committedThisStreet={props.committedThisStreet ?? 0}
        rejection={props.rejection ?? null}
        refusal={props.refusal ?? null}
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
    const { queryAllByRole } = bar({ turn: null });

    const buttons = queryAllByRole("button");
    expect(buttons).toEqual([]);
  });

  it("renders one button per action the server allowed, in the order it sent them", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const buttons = within(
      getByRole("group", { name: "actions" }),
    ).getAllByRole("button");
    const texts = buttons.map((b) => b.textContent);
    expect(texts).toEqual([
      "Fold",
      "Call 400",
      "Raise to 1,200",
      "All in 13,400",
    ]);
  });

  it("renders no button for an action the server withheld", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE"],
        }),
      }),
    });

    const actions = within(getByRole("group", { name: "actions" }));
    expect(actions.getAllByRole("button")).toHaveLength(3);
    expect(actions.queryByRole("button", { name: "Check" })).toBeNull();
  });

  it("fills the raise and leaves the other buttons ghosts", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    const buttons = within(
      getByRole("group", { name: "actions" }),
    ).getAllByRole("button");
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

  it("the sizing row offers the card's five presets", () => {
    const { getByRole, container } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          callTo: 400,
          minRaiseTo: 800,
          allInTo: 13400,
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
      potIncludingStreet: 2850,
      committedThisStreet: 0,
    });

    const chips = within(getByRole("group", { name: "amount" })).getAllByRole(
      "button",
    );
    expect(chips.map((chip) => chip.textContent)).toEqual([
      "min",
      "⅓",
      "½",
      "pot",
      "all-in",
    ]);
    chips.forEach((chip) => {
      expect(chip.textContent ?? "").not.toMatch(/\d/);
      expect(chip.getAttribute("aria-label") ?? "").not.toMatch(/\d/);
    });

    expect(container.querySelector("input")).toBeNull();
  });

  it("each preset sets the amount its own name states", () => {
    const frameA = aLegalActions({
      callTo: 400,
      minRaiseTo: 800,
      allInTo: 13400,
      allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
    });
    const frameB = aLegalActions({
      callTo: 600,
      minRaiseTo: 1000,
      allInTo: 13400,
      allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
    });

    function totalsFor(
      legalActions: ReturnType<typeof aLegalActions>,
      potIncludingStreet: number,
      committedThisStreet: number,
    ): (string | null)[] {
      const { getByRole, unmount } = bar({
        turn: aTurn({ legalActions }),
        potIncludingStreet,
        committedThisStreet,
      });
      const chips = within(getByRole("group", { name: "amount" })).getAllByRole(
        "button",
      );
      const totals = chips.map((chip) => {
        fireEvent.click(chip);
        return getByRole("button", { name: /^Raise to /i }).textContent;
      });
      unmount();
      return totals;
    }

    // Frame A: the hero frame, committedThisStreet 0.
    expect(totalsFor(frameA, 2850, 0)).toEqual([
      "Raise to 800",
      "Raise to 1,483",
      "Raise to 2,025",
      "Raise to 3,650",
      "Raise to 13,400",
    ]);

    // Frame B: the re-raise frame, committedThisStreet 200 — the one frame
    // that separates this answer from every near-miss formula (`ADR-0101`
    // §7).
    expect(totalsFor(frameB, 1400, 200)).toEqual([
      "Raise to 1,000",
      "Raise to 1,200",
      "Raise to 1,500",
      "Raise to 2,400",
      "Raise to 13,400",
    ]);
  });

  it("a preset the stack cannot afford is not offered", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          callTo: 400,
          minRaiseTo: 800,
          allInTo: 3000,
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
      potIncludingStreet: 2850,
      committedThisStreet: 0,
    });

    const sizing = within(getByRole("group", { name: "amount" }));
    const chips = sizing.getAllByRole("button");
    expect(chips.map((chip) => chip.textContent)).toEqual([
      "min",
      "⅓",
      "½",
      "all-in",
    ]);
    expect(sizing.queryByRole("button", { name: "pot" })).toBeNull();

    const totals = chips.map((chip) => {
      fireEvent.click(chip);
      return getByRole("button", { name: /^Raise to /i }).textContent;
    });
    expect(totals).toEqual([
      "Raise to 800",
      "Raise to 1,483",
      "Raise to 2,025",
      "Raise to 3,000",
    ]);
  });

  it("a preset under the server's minimum is not offered", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          callTo: 150,
          minRaiseTo: 300,
          allInTo: 10000,
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
      potIncludingStreet: 225,
      committedThisStreet: 75,
    });

    const sizing = within(getByRole("group", { name: "amount" }));
    const chips = sizing.getAllByRole("button");
    expect(chips.map((chip) => chip.textContent)).toEqual([
      "min",
      "½",
      "pot",
      "all-in",
    ]);
    expect(sizing.queryByRole("button", { name: "⅓" })).toBeNull();

    fireEvent.click(sizing.getByRole("button", { name: "min" }));
    expect(getByRole("button", { name: "Raise to 300" })).toBeDefined();

    fireEvent.click(sizing.getByRole("button", { name: "½" }));
    expect(getByRole("button", { name: "Raise to 300" })).toBeDefined();

    fireEvent.click(sizing.getByRole("button", { name: "pot" }));
    expect(getByRole("button", { name: "Raise to 450" })).toBeDefined();
  });

  it("offers only totals inside the bounds the server sent, betting and raising", () => {
    function totalsFor(
      turn: ReturnType<typeof aTurn>,
      potIncludingStreet: number,
      committedThisStreet: number,
      floor: number,
      allInTo: number,
    ): number[] {
      const { getByRole, unmount } = bar({
        turn,
        potIncludingStreet,
        committedThisStreet,
      });
      const chips = within(getByRole("group", { name: "amount" })).getAllByRole(
        "button",
      );
      expect(chips.length).toBeGreaterThan(0);

      const totals = chips.map((chip) => {
        fireEvent.click(chip);
        const printed =
          getByRole("button", {
            name: (name) =>
              name.startsWith("Bet") || name.startsWith("Raise to"),
          }).textContent ?? "";
        return Number(printed.replace(/\D+/g, ""));
      });
      totals.forEach((total) => {
        expect(total).toBeGreaterThanOrEqual(floor);
        expect(total).toBeLessThanOrEqual(allInTo);
      });

      unmount();
      return totals;
    }

    totalsFor(
      aTurn({
        legalActions: aLegalActions({
          callTo: 400,
          minRaiseTo: 800,
          allInTo: 13400,
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
      2850,
      0,
      800,
      13400,
    );

    // Frame E, the BET branch: half the pot with no call to add on top.
    const frameE = totalsFor(
      aTurn({
        legalActions: aLegalActions({
          callTo: 0,
          minBetTo: 2750,
          allInTo: 9100,
          allowed: ["CHECK", "BET", "ALL_IN"],
        }),
      }),
      9000,
      0,
      2750,
      9100,
    );
    expect(frameE[2]).toBe(4500); // the "½" chip, in the row's own order
  });

  it("starts the amount at the server's minimum for the action it allowed", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
    });

    expect(getByRole("button", { name: "Raise to 1,200" })).toBeDefined();

    const { getByRole: getByRole2 } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          minBetTo: 350,
          allInTo: 8200,
          allowed: ["CHECK", "BET", "ALL_IN"],
        }),
      }),
    });

    expect(getByRole2("button", { name: "Bet 350" })).toBeDefined();
  });

  it("offers no amount control when neither a bet nor a raise is allowed", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          allowed: ["FOLD", "CALL", "ALL_IN"],
        }),
      }),
    });

    expect(
      within(getByRole("group", { name: "amount" })).queryAllByRole("button"),
    ).toEqual([]);
    expect(
      within(getByRole("group", { name: "actions" }))
        .getAllByRole("button")
        .map((button) => button.textContent),
    ).toEqual(["Fold", "Call 400", "All in 13,400"]);
  });

  it("writes the raise button's total from the preset the player pressed", () => {
    const { getByRole } = bar({
      turn: aTurn({
        legalActions: aLegalActions({
          callTo: 400,
          minRaiseTo: 800,
          allInTo: 13400,
          allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
        }),
      }),
      potIncludingStreet: 2850,
      committedThisStreet: 0,
    });

    fireEvent.click(
      within(getByRole("group", { name: "amount" })).getByRole("button", {
        name: "pot",
      }),
    );

    const raiseButton = getByRole("button", { name: "Raise to 3,650" });
    expect(raiseButton).toBeDefined();
  });

  it("sends one Act carrying the turn's identity", () => {
    const send = vi.fn();
    const { rerender, getByRole } = render(
      <ActionBar
        turn={aTurn({ handNumber: 61, actionSequence: 103 })}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenNthCalledWith(1, {
      type: "Act",
      handNumber: 61,
      actionSequence: 103,
      action: { type: "Fold", seat: 0 },
    });

    rerender(
      <ActionBar
        turn={aTurn({
          handNumber: 42,
          actionSequence: 88,
          legalActions: aLegalActions({ seat: 1 }),
        })}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenNthCalledWith(2, {
      type: "Act",
      handNumber: 42,
      actionSequence: 88,
      action: { type: "Fold", seat: 1 },
    });

    expect(send).toHaveBeenCalledTimes(2);
  });

  it("sends the total the amount control holds", () => {
    const send = vi.fn();
    const { rerender, getByRole } = render(
      <ActionBar
        turn={aTurn({
          legalActions: aLegalActions({
            callTo: 400,
            minRaiseTo: 800,
            allInTo: 13400,
            allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
          }),
        })}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );

    fireEvent.click(
      within(getByRole("group", { name: "amount" })).getByRole("button", {
        name: "pot",
      }),
    );
    fireEvent.click(getByRole("button", { name: "Raise to 3,650" }));

    expect(send).toHaveBeenNthCalledWith(1, {
      type: "Act",
      handNumber: 14,
      actionSequence: 27,
      action: { type: "Raise", seat: 0, to: 3650 },
    });

    rerender(
      <ActionBar
        turn={aTurn({
          actionSequence: 28,
          legalActions: aLegalActions({
            callTo: 600,
            minRaiseTo: 1000,
            allInTo: 13400,
            allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
          }),
        })}
        potIncludingStreet={1400}
        committedThisStreet={200}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );

    fireEvent.click(
      within(getByRole("group", { name: "amount" })).getByRole("button", {
        name: "½",
      }),
    );
    fireEvent.click(getByRole("button", { name: "Raise to 1,500" }));

    expect(send).toHaveBeenNthCalledWith(2, {
      type: "Act",
      handNumber: 14,
      actionSequence: 28,
      action: { type: "Raise", seat: 0, to: 1500 },
    });

    expect(send).toHaveBeenCalledTimes(2);
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

    const region = getByRole("region", { name: "your move" });
    const everyButton = within(region).getAllByRole("button");
    const actionButtons = within(
      getByRole("group", { name: "actions" }),
    ).getAllByRole("button");

    everyButton.forEach((button) => {
      expect((button as HTMLButtonElement).disabled).toBe(true);
    });
    // Strictly more buttons than the actions row alone holds — the sizing
    // chips are in this set too.
    expect(everyButton.length).toBeGreaterThan(actionButtons.length);
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
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );
    fireEvent.click(getByRole("button", { name: "Fold" }));

    rerender(
      <ActionBar
        turn={aTurn({
          actionSequence: 28,
          legalActions: aLegalActions({ minRaiseTo: 2400 }),
        })}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        send={send}
      />,
    );

    expect(getByRole("button", { name: "Raise to 2,400" })).toBeDefined();
    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenCalledTimes(2);
  });

  it("comes back to life after a rejection at the same decision point", () => {
    const send = vi.fn();
    const turn = aTurn({ handNumber: 61, actionSequence: 103 });
    const { rerender, getByRole } = render(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        rejectionCount={0}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenCalledTimes(1);

    // The very same turn object: the identity below survives on the object,
    // not because a new one arrived.
    rerender(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={{ type: "AmountTooSmall", attempted: 900, minimum: 1200 }}
        refusal={null}
        rejectionCount={1}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));

    expect(send).toHaveBeenNthCalledWith(2, {
      type: "Act",
      handNumber: 61,
      actionSequence: 103,
      action: { type: "Fold", seat: 0 },
    });
    expect(send).toHaveBeenCalledTimes(2);
  });

  it("stays locked when nothing was rejected", () => {
    const send = vi.fn();
    const turn = aTurn({ handNumber: 61, actionSequence: 103 });
    const { rerender, getByRole } = render(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        rejectionCount={0}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));
    expect(send).toHaveBeenCalledTimes(1);

    // A plain rerender, count unmoved: a bar that remounted on every rerender
    // would send a second time here too.
    rerender(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        rejectionCount={0}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));

    expect(send).toHaveBeenCalledTimes(1);
  });

  it("comes back a second time when the second attempt is refused too", () => {
    const send = vi.fn();
    const turn = aTurn({ handNumber: 61, actionSequence: 103 });
    // Deep-equal on both rejected rerenders: only the count moves, the way a
    // second identical refusal actually arrives.
    const rejection = {
      type: "AmountTooSmall",
      attempted: 900,
      minimum: 1200,
    } as const;
    const expected = {
      type: "Act",
      handNumber: 61,
      actionSequence: 103,
      action: { type: "Fold", seat: 0 },
    };
    const { rerender, getByRole } = render(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        rejectionCount={0}
        send={send}
      />,
    );

    fireEvent.click(getByRole("button", { name: "Fold" }));

    rerender(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={rejection}
        refusal={null}
        rejectionCount={1}
        send={send}
      />,
    );
    fireEvent.click(getByRole("button", { name: "Fold" }));

    rerender(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={rejection}
        refusal={null}
        rejectionCount={2}
        send={send}
      />,
    );
    fireEvent.click(getByRole("button", { name: "Fold" }));

    expect(send).toHaveBeenNthCalledWith(1, expected);
    expect(send).toHaveBeenNthCalledWith(2, expected);
    expect(send).toHaveBeenNthCalledWith(3, expected);
    expect(send).toHaveBeenCalledTimes(3);
  });

  it("returns the amount control to the minimum the server sent after a rejection", () => {
    const send = vi.fn();
    const turn = aTurn({
      legalActions: aLegalActions({
        callTo: 400,
        minRaiseTo: 1200,
        allInTo: 13400,
        allowed: ["FOLD", "CALL", "RAISE", "ALL_IN"],
      }),
    });
    const { rerender, getByRole } = render(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal={null}
        rejectionCount={0}
        send={send}
      />,
    );

    fireEvent.click(
      within(getByRole("group", { name: "amount" })).getByRole("button", {
        name: "pot",
      }),
    );
    expect(getByRole("button", { name: "Raise to 3,650" })).toBeDefined();

    rerender(
      <ActionBar
        turn={turn}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={{ type: "AmountTooSmall", attempted: 900, minimum: 1200 }}
        refusal={null}
        rejectionCount={1}
        send={send}
      />,
    );

    // Reached by a remount at the server's minimum, not by a clamp.
    expect(getByRole("button", { name: "Raise to 1,200" })).toBeDefined();
  });

  it("states a rejection in the server's own numbers", () => {
    const send = vi.fn();
    const { rerender, getByText, queryByText } = render(
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={{ type: "AmountTooSmall", attempted: 900, minimum: 1200 }}
        refusal={null}
        send={send}
      />,
    );

    let text = getByText("900 is under the minimum of 1,200.");
    expect(text).toBeDefined();

    // Different amounts show different numbers
    rerender(
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={{ type: "AmountTooSmall", attempted: 500, minimum: 2500 }}
        refusal={null}
        send={send}
      />,
    );

    text = getByText("500 is under the minimum of 2,500.");
    expect(text).toBeDefined();
    expect(queryByText("900 is under the minimum of 1,200.")).toBeNull();

    // Different rejection type shows different message
    rerender(
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={{
          type: "NotYourTurn",
          seatToAct: 1,
        }}
        refusal={null}
        send={send}
      />,
    );

    text = getByText("The server says it is seat 1's turn.");
    expect(text).toBeDefined();
  });

  it("says a paused duel did not apply the action", () => {
    const send = vi.fn();
    const { rerender, getByText, queryByText } = render(
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal="DUEL_PAUSED"
        send={send}
      />,
    );

    let text = getByText("The duel is paused. That action was not applied.");
    expect(text).toBeDefined();

    // Different refusal shows different message
    rerender(
      <ActionBar
        turn={aTurn()}
        potIncludingStreet={2850}
        committedThisStreet={0}
        rejection={null}
        refusal="NOT_IN_DUEL"
        send={send}
      />,
    );

    text = getByText("The server did not apply that action.");
    expect(text).toBeDefined();
    expect(
      queryByText("The duel is paused. That action was not applied."),
    ).toBeNull();
  });

  it("has nothing to say when nothing was refused", () => {
    const { queryByText, send } = bar();

    const refusedText = queryByText(/refused|minimum|paused/);
    expect(refusedText).toBeNull();
    expect(send).not.toHaveBeenCalled();
  });
});
