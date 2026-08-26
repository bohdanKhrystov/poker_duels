import { cleanup, fireEvent, render } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import { CANCEL, SIGN_OUT_LABEL, SIGN_OUT_WARNING } from "./account-text";
import { SignOutControl } from "./SignOutControl";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("signing out", () => {
  test("offers nothing to a browser that is not signed in", () => {
    const signOut = vi.fn();
    const { queryByRole } = render(
      <SignOutControl signedIn={false} signOut={signOut} />,
    );

    expect(queryByRole("button", { name: SIGN_OUT_LABEL })).toBeNull();
  });

  test("offers the control to a browser holding a session", () => {
    const signOut = vi.fn();
    const { getByRole, queryByText } = render(
      <SignOutControl signedIn={true} signOut={signOut} />,
    );

    expect(getByRole("button", { name: SIGN_OUT_LABEL })).toBeTruthy();
    expect(queryByText(SIGN_OUT_WARNING)).toBeNull();
  });

  test("warns before it acts, and acts on nothing until it is confirmed", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText } = render(
      <SignOutControl signedIn={true} signOut={signOut} />,
    );

    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    expect(getByText(SIGN_OUT_WARNING)).toBeTruthy();
    expect(signOut).toHaveBeenCalledTimes(0);
  });

  test("calls once, and only from the confirming control", () => {
    const confirmed = vi
      .fn()
      .mockResolvedValue({ kind: "signed-out" } as const);
    const confirmRender = render(
      <SignOutControl signedIn={true} signOut={confirmed} />,
    );
    fireEvent.click(
      confirmRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );
    fireEvent.click(
      confirmRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );

    expect(confirmed).toHaveBeenCalledTimes(1);
    confirmRender.unmount();

    const cancelled = vi
      .fn()
      .mockResolvedValue({ kind: "signed-out" } as const);
    const cancelRender = render(
      <SignOutControl signedIn={true} signOut={cancelled} />,
    );
    fireEvent.click(cancelRender.getByRole("button", { name: SIGN_OUT_LABEL }));
    fireEvent.click(cancelRender.getByRole("button", { name: CANCEL }));

    expect(cancelled).toHaveBeenCalledTimes(0);
    expect(
      cancelRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    ).toBeTruthy();
    expect(cancelRender.queryByText(SIGN_OUT_WARNING)).toBeNull();
  });

  test("puts no browser dialog between the press and the act", () => {
    const confirmDialog = vi.spyOn(window, "confirm").mockReturnValue(true);
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole } = render(
      <SignOutControl signedIn={true} signOut={signOut} />,
    );

    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));
    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    expect(confirmDialog).toHaveBeenCalledTimes(0);
  });
});
