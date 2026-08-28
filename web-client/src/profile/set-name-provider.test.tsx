import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { SetNameProvider, useSetName } from "./set-name-provider";
import type { SetNameOutcome } from "./set-name";

/** Renders what `useSetName()` answers, and calls it when mounted. */
function Probe(props: {
  onCall?: (
    setName: ((name: string) => Promise<SetNameOutcome>) | null,
  ) => void;
}): ReactElement {
  const setName = useSetName();
  if (props.onCall) {
    props.onCall(setName);
  }
  if (setName === null) {
    return <p>no setName</p>;
  }
  return <p>has setName</p>;
}

describe("the set-name provider", () => {
  it("hands the write down to the tree below it", async () => {
    const spy1 = vi.fn(async (): Promise<SetNameOutcome> => ({
      kind: "named",
      profile: {
        playerId: "test-id",
        displayName: "Ada",
        coinBalance: 100,
        displayNameRemoved: false,
        deviceRouteLive: false,
        hasRecoveryEmail: false,
      },
    }));

    let receivedSetName: ((name: string) => Promise<SetNameOutcome>) | null =
      null;

    const { rerender } = render(
      <SetNameProvider setName={spy1}>
        <Probe
          onCall={(fn) => {
            receivedSetName = fn;
          }}
        />
      </SetNameProvider>,
    );

    // The probe should receive the exact function reference
    expect(receivedSetName).toBe(spy1);
    expect(screen.getByText("has setName")).toBeDefined();

    // Calling the function should reach the spy
    const result = await receivedSetName!("Ada");
    expect(result.kind).toBe("named");
    expect(spy1).toHaveBeenCalledWith("Ada");

    // A second render with a different function should hand down that one instead
    const spy2 = vi.fn(async (): Promise<SetNameOutcome> => ({
      kind: "conflict",
    }));

    receivedSetName = null;
    rerender(
      <SetNameProvider setName={spy2}>
        <Probe
          onCall={(fn) => {
            receivedSetName = fn;
          }}
        />
      </SetNameProvider>,
    );

    // The probe should now receive the new function reference
    expect(receivedSetName).toBe(spy2);
    expect(receivedSetName).not.toBe(spy1);
  });

  it("answers null where no provider is above, and asks for nothing", () => {
    const onCall = vi.fn();

    render(<Probe onCall={onCall} />);

    // The probe should get null and not throw
    expect(screen.getByText("no setName")).toBeDefined();
    expect(onCall).toHaveBeenCalledWith(null);
  });
});
