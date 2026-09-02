import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { SeatPresence } from "../protocol";
import { SeatPlate } from "./SeatPlate";
import { aSeat } from "./view-fixture";

describe("a seat plate", () => {
  function plate(
    overrides: Parameters<typeof aSeat>[0] = {},
    props: {
      hasButton?: boolean;
      isToAct?: boolean;
      isViewer?: boolean;
      presence?: SeatPresence | null;
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
});
