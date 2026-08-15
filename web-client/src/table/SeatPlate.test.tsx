import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SeatPlate } from "./SeatPlate";
import { aSeat } from "./view-fixture";

describe("a seat plate", () => {
  function plate(
    overrides: Parameters<typeof aSeat>[0] = {},
    props: { hasButton?: boolean; isToAct?: boolean; isViewer?: boolean } = {},
  ) {
    return render(
      <SeatPlate
        name="You"
        seat={aSeat(overrides)}
        hasButton={props.hasButton ?? false}
        isToAct={props.isToAct ?? false}
        isViewer={props.isViewer ?? true}
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
});
