import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { tapProps } from "./tap";

function touch(x: number, y: number) {
  return { clientX: x, clientY: y };
}

describe("a tap on a control", () => {
  it("acts once at touchend, and the click that would follow is cancelled", () => {
    const act = vi.fn();
    render(
      <button type="button" {...tapProps(act)}>
        Raise
      </button>,
    );
    const button = screen.getByRole("button");

    fireEvent.touchStart(button, { touches: [touch(10, 10)] });
    const cancelled = !fireEvent.touchEnd(button, {
      changedTouches: [touch(12, 11)],
      cancelable: true,
    });
    expect(act).toHaveBeenCalledTimes(1);
    expect(cancelled).toBe(true);
  });

  it("acts on nothing when the finger travelled — that was a scroll", () => {
    const act = vi.fn();
    render(
      <button type="button" {...tapProps(act)}>
        Raise
      </button>,
    );
    const button = screen.getByRole("button");

    fireEvent.touchStart(button, { touches: [touch(10, 10)] });
    const cancelled = !fireEvent.touchEnd(button, {
      changedTouches: [touch(10, 60)],
      cancelable: true,
    });
    expect(act).not.toHaveBeenCalled();
    // Not cancelled: the browser's own scroll handling is left alone.
    expect(cancelled).toBe(false);
  });

  it("acts on nothing for a disabled control, and still acts on a mouse click", () => {
    const act = vi.fn();
    const { rerender } = render(
      <button type="button" disabled {...tapProps(act)}>
        Raise
      </button>,
    );
    const button = screen.getByRole("button");
    fireEvent.touchStart(button, { touches: [touch(10, 10)] });
    fireEvent.touchEnd(button, { changedTouches: [touch(10, 10)] });
    expect(act).not.toHaveBeenCalled();

    rerender(
      <button type="button" {...tapProps(act)}>
        Raise
      </button>,
    );
    fireEvent.click(button);
    expect(act).toHaveBeenCalledTimes(1);
  });

  it("takes the keyboard down with it: the focused field is blurred by the tap", () => {
    const act = vi.fn();
    render(
      <>
        <input aria-label="the total" />
        <button type="button" {...tapProps(act)}>
          Raise
        </button>
      </>,
    );
    const field = screen.getByLabelText("the total");
    field.focus();
    expect(document.activeElement).toBe(field);

    const button = screen.getByRole("button");
    fireEvent.touchStart(button, { touches: [touch(10, 10)] });
    fireEvent.touchEnd(button, { changedTouches: [touch(10, 10)] });
    expect(act).toHaveBeenCalledTimes(1);
    expect(document.activeElement).not.toBe(field);
  });
});
