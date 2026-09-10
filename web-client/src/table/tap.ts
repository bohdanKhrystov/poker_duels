import type { TouchEvent, MouseEvent } from "react";

/** How far a finger may travel between touching and lifting and still be a tap, in CSS pixels. */
const TAP_SLOP_PX = 12;

// Where each control's current touch began, keyed by the element so the start
// survives a re-render between touchstart and touchend (the turn clock
// re-renders the bar every second).
const touchStarts = new WeakMap<Element, { x: number; y: number }>();

/**
 * Props that make a control act on the tap itself, not on the click iOS
 * Safari synthesizes afterwards.
 *
 * With the amount box focused, Safari dismisses the keyboard on the first tap
 * elsewhere and shifts the page as it goes; the synthesized click can then
 * miss the control the finger was on, and a player reads "the first tap did
 * nothing". Acting at `touchend` — while the finger is still on the control —
 * sidesteps that, and `preventDefault` there is what stops the click from
 * following, so a tap acts exactly once. A finger that travelled more than
 * `TAP_SLOP_PX` was scrolling, not tapping, and acts on nothing. A mouse, a
 * pen, and a keyboard reach `onClick` as they always did.
 *
 * The focused field is blurred on the tap, since a press on Fold, Call or
 * Raise is the end of the typing.
 */
export function tapProps<E extends HTMLElement>(
  act: () => void,
): {
  readonly onTouchStart: (event: TouchEvent<E>) => void;
  readonly onTouchEnd: (event: TouchEvent<E>) => void;
  readonly onClick: (event: MouseEvent<E>) => void;
} {
  return {
    onTouchStart: (event) => {
      const touch = event.touches[0];
      if (touch === undefined) return;
      touchStarts.set(event.currentTarget, {
        x: touch.clientX,
        y: touch.clientY,
      });
    },
    onTouchEnd: (event) => {
      const start = touchStarts.get(event.currentTarget);
      touchStarts.delete(event.currentTarget);
      const touch = event.changedTouches[0];
      if (start === undefined || touch === undefined) return;
      if (
        Math.hypot(touch.clientX - start.x, touch.clientY - start.y) >
        TAP_SLOP_PX
      ) {
        return;
      }
      if (
        (event.currentTarget as HTMLElement & { disabled?: boolean }).disabled
      ) {
        return;
      }
      if (event.cancelable) event.preventDefault();
      const focused = document.activeElement;
      if (focused instanceof HTMLElement && focused !== event.currentTarget) {
        focused.blur();
      }
      act();
    },
    onClick: () => act(),
  };
}
