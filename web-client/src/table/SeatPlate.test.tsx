import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { SeatPresence } from "../protocol";
import type { ActEvent } from "../store/duel-state";
import { SeatPlate } from "./SeatPlate";
import { aSeat } from "./view-fixture";

// A non-breaking space, built at runtime rather than typed as a literal
// character, matching SeatPlate's own separator between a mark's verb and
// its figure.
const NBSP = String.fromCharCode(0xa0);

describe("a seat plate", () => {
  function plate(
    overrides: Parameters<typeof aSeat>[0] = {},
    props: {
      hasButton?: boolean;
      isToAct?: boolean;
      isViewer?: boolean;
      presence?: SeatPresence | null;
      lastAct?: ActEvent | null;
    } = {},
  ) {
    return render(
      <SeatPlate
        name="You"
        seat={aSeat(overrides)}
        hasButton={props.hasButton ?? false}
        isToAct={props.isToAct ?? false}
        isViewer={props.isViewer ?? true}
        presence={props.presence ?? null}
        lastAct={props.lastAct}
      />,
    );
  }

  it("writes the name and the stack the view carries", () => {
    const { getByText } = plate({ stack: 13400 });
    getByText("You");
    getByText("13,400");
  });

  it("shows the button only on the seat that has it", () => {
    const { getByLabelText, unmount } = plate({}, { hasButton: true });
    getByLabelText("the button");
    unmount();
    const { queryByLabelText } = plate({}, { hasButton: false });
    expect(queryByLabelText("the button")).toBeNull();
  });

  it("puts the status the seat is in on the plate", () => {
    const { getByText } = plate({ hasFolded: true });
    getByText("Folded");
  });

  it("puts the presence on the plate ahead of the turn", () => {
    const { getByText, queryByText, unmount } = plate(
      {},
      { presence: "AWAY", isToAct: true, isViewer: false },
    );
    getByText("Away");
    expect(queryByText("Their turn")).toBeNull();
    expect(queryByText("Your turn")).toBeNull();
    unmount();

    const {
      getByText: getByText2,
      queryByText: queryByText2,
      unmount: unmount2,
    } = plate({}, { presence: "AWAY", isToAct: true, isViewer: true });
    getByText2("Away");
    expect(queryByText2("Your turn")).toBeNull();
    unmount2();

    const { getByText: getByText3 } = plate(
      {},
      { presence: "ABSENT", isToAct: true },
    );
    getByText3("Timed out");
  });

  it("gives a present seat its ordinary status back", () => {
    const { getByText, queryByText } = plate(
      {},
      { presence: "PRESENT", isToAct: true, isViewer: false },
    );
    getByText("Their turn");
    expect(queryByText("Away")).toBeNull();
    expect(queryByText("Timed out")).toBeNull();

    const { getByText: getByText2, queryByText: queryByText2 } = plate(
      { hasFolded: true },
      { presence: "PRESENT" },
    );
    getByText2("Folded");
    expect(queryByText2("Away")).toBeNull();
    expect(queryByText2("Timed out")).toBeNull();
  });

  it("marks whichever seat is on turn, hero or rival", () => {
    const { container, unmount } = plate({}, { isToAct: true, isViewer: true });
    expect(container.querySelectorAll(".acting-mark")).toHaveLength(1);
    unmount();

    const { container: rivalContainer, unmount: unmountRival } = plate(
      {},
      { isToAct: true, isViewer: false },
    );
    expect(rivalContainer.querySelectorAll(".acting-mark")).toHaveLength(1);
    unmountRival();

    const { container: offTurnContainer } = plate({}, { isToAct: false });
    expect(offTurnContainer.querySelectorAll(".acting-mark")).toHaveLength(0);
  });

  it("keeps the still mark beside the moving one", () => {
    const { container, unmount } = plate({}, { isToAct: true });
    const onTurnPlate = container.firstElementChild as HTMLElement;
    expect(onTurnPlate.classList.contains("border-l-accent")).toBe(true);
    expect(onTurnPlate.classList.contains("acting-mark")).toBe(true);
    unmount();

    const { container: offTurnContainer } = plate({}, { isToAct: false });
    const offTurnPlate = offTurnContainer.firstElementChild as HTMLElement;
    expect(offTurnPlate.classList.contains("border-l-transparent")).toBe(true);
    expect(offTurnPlate.classList.contains("border-l-accent")).toBe(false);
    expect(offTurnPlate.classList.contains("acting-mark")).toBe(false);
  });

  it("leaves an away or timed-out seat unmarked, even on turn", () => {
    const { container, getByText, unmount } = plate(
      {},
      { presence: "AWAY", isToAct: true },
    );
    expect(container.querySelectorAll(".acting-mark")).toHaveLength(0);
    const awayPlate = container.firstElementChild as HTMLElement;
    expect(awayPlate.classList.contains("border-l-accent")).toBe(false);
    getByText("Away");
    unmount();

    const { container: absentContainer, getByText: getByTextAbsent } = plate(
      {},
      { presence: "ABSENT", isToAct: true },
    );
    expect(absentContainer.querySelectorAll(".acting-mark")).toHaveLength(0);
    const absentPlate = absentContainer.firstElementChild as HTMLElement;
    expect(absentPlate.classList.contains("border-l-accent")).toBe(false);
    getByTextAbsent("Timed out");
  });

  it("draws no mark when it is handed none", () => {
    const { container, unmount } = plate();
    expect(container.querySelectorAll(".last-act")).toHaveLength(0);
    unmount();

    const { container: nullContainer } = plate({}, { lastAct: null });
    expect(nullContainer.querySelectorAll(".last-act")).toHaveLength(0);
  });

  it("prints a fold and a check bare", () => {
    const folded: ActEvent = { type: "PlayerFolded", sequence: 1, seat: 0 };
    const { container, unmount } = plate({}, { lastAct: folded });
    const foldMarks = container.querySelectorAll(".last-act");
    expect(foldMarks).toHaveLength(1);
    expect(foldMarks[0].textContent).toBe("Fold");
    expect(foldMarks[0].textContent).not.toMatch(/\d/);
    unmount();

    const checked: ActEvent = { type: "PlayerChecked", sequence: 1, seat: 0 };
    const { container: checkContainer } = plate({}, { lastAct: checked });
    const checkMarks = checkContainer.querySelectorAll(".last-act");
    expect(checkMarks).toHaveLength(1);
    expect(checkMarks[0].textContent).toBe("Check");
    expect(checkMarks[0].textContent).not.toMatch(/\d/);
  });

  it("prints the act's own total on a call, a bet, a raise and an all-in", () => {
    const cases: Array<[ActEvent, string]> = [
      [
        { type: "PlayerCalled", sequence: 1, seat: 0, to: 400 },
        `Call${NBSP}400`,
      ],
      [{ type: "PlayerBet", sequence: 1, seat: 0, to: 950 }, `Bet${NBSP}950`],
      [
        { type: "PlayerRaised", sequence: 1, seat: 0, to: 2300 },
        `Raise to${NBSP}2,300`,
      ],
      [
        { type: "PlayerAllIn", sequence: 1, seat: 0, to: 13400 },
        `All in${NBSP}13,400`,
      ],
    ];

    for (const [event, text] of cases) {
      const { container, unmount } = plate({}, { lastAct: event });
      const marks = container.querySelectorAll(".last-act");
      expect(marks).toHaveLength(1);
      expect(marks[0].textContent).toBe(text);
      unmount();
    }
  });

  it("speaks the mark to nobody", () => {
    const { container } = plate(
      {},
      {
        hasButton: true,
        lastAct: { type: "PlayerBet", sequence: 1, seat: 0, to: 950 },
      },
    );
    expect(container.querySelectorAll(".last-act")).toHaveLength(1);
    expect(container.querySelectorAll("[aria-label]")).toHaveLength(1);
    expect(container.querySelectorAll("[title]")).toHaveLength(0);
  });
});
