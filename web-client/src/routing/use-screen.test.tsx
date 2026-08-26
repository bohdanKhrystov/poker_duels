import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { useScreen } from "./use-screen";

describe("the screen the address names", () => {
  beforeEach(() => {
    // A replace, not an assignment: it resets the fragment without adding a
    // history entry, so history.length deltas captured inside a test stay
    // meaningful instead of drifting upward test after test.
    window.history.replaceState(null, "", "/");
  });

  afterEach(() => {
    cleanup();
  });

  it("reads the screen the address already names", () => {
    window.location.hash = "#/duels";
    const first = renderHook(() => useScreen());
    expect(first.result.current.screen).toBe("duels");
    first.unmount();

    // A second address, because one would pass against a hook that always
    // returns the same constant.
    window.location.hash = "#/leaderboard";
    const second = renderHook(() => useScreen());
    expect(second.result.current.screen).toBe("leaderboard");
    second.unmount();
  });

  it("re-renders when the address changes under it", async () => {
    const { result } = renderHook(() => useScreen());
    expect(result.current.screen).toBe("first");

    window.location.hash = "#/leaderboard";

    await waitFor(() => {
      expect(result.current.screen).toBe("leaderboard");
    });
  });

  it("opening a screen changes the address and adds an entry to go back to", async () => {
    const { result } = renderHook(() => useScreen());
    const lengthBefore = window.history.length;

    act(() => {
      result.current.open("duels");
    });

    expect(window.location.hash).toBe("#/duels");
    expect(window.history.length).toBe(lengthBefore + 1);

    await waitFor(() => {
      expect(result.current.screen).toBe("duels");
    });
  });

  it("leaving a screen replaces the entry rather than stacking another", () => {
    window.location.hash = "#/duels";
    const { result } = renderHook(() => useScreen());
    expect(result.current.screen).toBe("duels");

    const lengthBefore = window.history.length;

    act(() => {
      result.current.leave();
    });

    expect(window.location.hash).toBe("");
    expect(result.current.screen).toBe("first");
    expect(window.history.length).toBe(lengthBefore);
  });

  it("leaving a screen re-renders with no hashchange anywhere", () => {
    window.location.hash = "#/duels";
    const { result } = renderHook(() => useScreen());
    expect(result.current.screen).toBe("duels");

    let hashChangeCount = 0;
    const countHashChange = () => {
      hashChangeCount += 1;
    };
    window.addEventListener("hashchange", countHashChange);

    act(() => {
      result.current.leave();
    });

    window.removeEventListener("hashchange", countHashChange);

    expect(hashChangeCount).toBe(0);
    expect(result.current.screen).toBe("first");
  });
});
