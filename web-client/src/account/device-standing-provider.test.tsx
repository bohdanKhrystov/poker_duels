import { act, fireEvent, render, screen } from "@testing-library/react";
import { useEffect, useState, type ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import {
  DeviceStandingProvider,
  useSignOutHandsANewProfile,
} from "./device-standing-provider";

function Consumer(): ReactElement {
  const handsANewProfile = useSignOutHandsANewProfile();
  return <span>{String(handsANewProfile)}</span>;
}

// Keeps the last value the consumer rendered in a DOM node outside the
// conditionally-unmounted subtree, so a test can read it after that subtree
// is gone rather than relying on console noise from a stray late update.
function Harness(props: { read: () => Promise<boolean> }): ReactElement {
  const [visible, setVisible] = useState(true);
  const [lastSeen, setLastSeen] = useState(false);
  return (
    <div>
      <div data-testid="last-seen">{String(lastSeen)}</div>
      <button onClick={() => setVisible(false)}>hide</button>
      {visible && (
        <DeviceStandingProvider read={props.read}>
          <Recorder onValue={setLastSeen} />
        </DeviceStandingProvider>
      )}
    </div>
  );
}

function Recorder(props: { onValue: (value: boolean) => void }): null {
  const value = useSignOutHandsANewProfile();
  const { onValue } = props;
  useEffect(() => {
    onValue(value);
  }, [value, onValue]);
  return null;
}

describe("DeviceStandingProvider", () => {
  it("answers false before the read has answered", () => {
    const read = (): Promise<boolean> => new Promise<boolean>(() => {});
    render(
      <DeviceStandingProvider read={read}>
        <Consumer />
      </DeviceStandingProvider>,
    );
    expect(screen.getByText("false")).toBeTruthy();
  });

  it("adopts the answer, either way", async () => {
    const readTrue = (): Promise<boolean> => Promise.resolve(true);
    const { unmount } = render(
      <DeviceStandingProvider read={readTrue}>
        <Consumer />
      </DeviceStandingProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText("true")).toBeTruthy();
    unmount();

    const readFalse = (): Promise<boolean> => Promise.resolve(false);
    render(
      <DeviceStandingProvider read={readFalse}>
        <Consumer />
      </DeviceStandingProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    expect(screen.getByText("false")).toBeTruthy();
  });

  it("reads once per mount", async () => {
    const read = vi.fn((): Promise<boolean> => Promise.resolve(false));
    const { rerender } = render(
      <DeviceStandingProvider read={read}>
        <Consumer />
      </DeviceStandingProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    rerender(
      <DeviceStandingProvider read={read}>
        <Consumer />
      </DeviceStandingProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    expect(read).toHaveBeenCalledTimes(1);
  });

  it("answers false where no provider is above", () => {
    render(<Consumer />);
    expect(screen.getByText("false")).toBeTruthy();
  });

  it("a read that lands after unmount changes nothing", async () => {
    let resolveRead!: (value: boolean) => void;
    const read = (): Promise<boolean> =>
      new Promise<boolean>((resolve) => {
        resolveRead = resolve;
      });
    render(<Harness read={read} />);
    expect(screen.getByTestId("last-seen").textContent).toBe("false");

    fireEvent.click(screen.getByRole("button", { name: "hide" }));

    await act(async () => {
      resolveRead(true);
      await Promise.resolve();
    });

    expect(screen.getByTestId("last-seen").textContent).toBe("false");
  });
});
