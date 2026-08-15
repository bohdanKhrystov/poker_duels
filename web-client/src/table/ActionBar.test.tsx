import { render } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ActionBar } from "./ActionBar";
import { aTurn } from "./turn-fixture";

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
});
