import { cleanup, fireEvent, render } from "@testing-library/react";
import { afterEach, describe, expect, test, vi } from "vitest";
import {
  CANCEL,
  SIGN_OUT_HANDS_A_NEW_PROFILE,
  SIGN_OUT_LABEL,
  SIGN_OUT_WARNING,
} from "./account-text";
import { SignOutControl } from "./SignOutControl";

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("signing out", () => {
  test("offers nothing to a browser that is not signed in", () => {
    const signOut = vi.fn();
    const { queryByRole } = render(
      <SignOutControl
        signedIn={false}
        signOutHandsANewProfile={true}
        signOut={signOut}
      />,
    );

    expect(queryByRole("button", { name: SIGN_OUT_LABEL })).toBeNull();
  });

  test("offers the control to a browser holding a session", () => {
    const signOut = vi.fn();
    const { getByRole, queryByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    expect(getByRole("button", { name: SIGN_OUT_LABEL })).toBeTruthy();
    expect(queryByText(SIGN_OUT_WARNING)).toBeNull();
  });

  test("warns before it acts, and acts on nothing until it is confirmed", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
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
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={confirmed}
      />,
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
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={cancelled}
      />,
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
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));
    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    expect(confirmDialog).toHaveBeenCalledTimes(0);
  });

  test("states the new-profile sentence when that is the sign-out on offer", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText, queryByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={true}
        signOut={signOut}
      />,
    );

    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    expect(getByText(SIGN_OUT_HANDS_A_NEW_PROFILE)).toBeTruthy();
    expect(queryByText(SIGN_OUT_WARNING)).toBeNull();
  });

  test("states the returning sentence when the profile stays", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText, queryByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    expect(getByText(SIGN_OUT_WARNING)).toBeTruthy();
    expect(queryByText(SIGN_OUT_HANDS_A_NEW_PROFILE)).toBeNull();
  });

  test("the press carries the answer the sentence stated", () => {
    const handsANewProfile = vi
      .fn()
      .mockResolvedValue({ kind: "signed-out" } as const);
    const newProfileRender = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={true}
        signOut={handsANewProfile}
      />,
    );
    fireEvent.click(
      newProfileRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );
    fireEvent.click(
      newProfileRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );

    expect(handsANewProfile).toHaveBeenCalledWith(true);
    newProfileRender.unmount();

    const keepsProfile = vi
      .fn()
      .mockResolvedValue({ kind: "signed-out" } as const);
    const keepsProfileRender = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={keepsProfile}
      />,
    );
    fireEvent.click(
      keepsProfileRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );
    fireEvent.click(
      keepsProfileRender.getByRole("button", { name: SIGN_OUT_LABEL }),
    );

    expect(keepsProfile).toHaveBeenCalledWith(false);
  });

  // ADR-0143 §2 obligation 2: the claim is the last thing read before the control that performs
  // the act, with no other sentence between them.
  test("the keeping sentence is the last thing before the confirming press", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={false}
        signOut={signOut}
      />,
    );

    // Enter the confirming step
    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    // Find the sentence element (the <p> containing the warning text)
    let sentenceElement = getByText(SIGN_OUT_WARNING);
    while (sentenceElement && sentenceElement.tagName !== "P") {
      sentenceElement = sentenceElement.parentElement as HTMLElement;
    }
    expect(sentenceElement).toBeTruthy();
    expect(sentenceElement?.tagName).toBe("P");

    // The next element sibling should be the confirming button
    const nextElementSibling =
      sentenceElement?.nextElementSibling as HTMLElement | null;
    expect(nextElementSibling).toBeTruthy();
    expect(nextElementSibling?.textContent?.trim()).toBe(SIGN_OUT_LABEL);

    // Assert: no text node sits between them
    let nextNode: Node | null = sentenceElement?.nextSibling || null;
    while (nextNode && nextNode.nodeType === 3) {
      // Text node type is 3
      const textContent = (nextNode as Text).data;
      if (textContent.trim() !== "") {
        throw new Error(
          `Non-whitespace text node found between sentence and button: "${textContent}"`,
        );
      }
      nextNode = nextNode.nextSibling;
    }
  });

  // ADR-0143 §2 obligation 2: the claim is the last thing read before the control that performs
  // the act, with no other sentence between them.
  test("the abandoning sentence is the last thing before the confirming press", () => {
    const signOut = vi.fn().mockResolvedValue({ kind: "signed-out" } as const);
    const { getByRole, getByText } = render(
      <SignOutControl
        signedIn={true}
        signOutHandsANewProfile={true}
        signOut={signOut}
      />,
    );

    // Enter the confirming step
    fireEvent.click(getByRole("button", { name: SIGN_OUT_LABEL }));

    // Find the sentence element (the <p> containing the warning text)
    let sentenceElement = getByText(SIGN_OUT_HANDS_A_NEW_PROFILE);
    while (sentenceElement && sentenceElement.tagName !== "P") {
      sentenceElement = sentenceElement.parentElement as HTMLElement;
    }
    expect(sentenceElement).toBeTruthy();
    expect(sentenceElement?.tagName).toBe("P");

    // The next element sibling should be the confirming button
    const nextElementSibling =
      sentenceElement?.nextElementSibling as HTMLElement | null;
    expect(nextElementSibling).toBeTruthy();
    expect(nextElementSibling?.textContent?.trim()).toBe(SIGN_OUT_LABEL);

    // Assert: no text node sits between them
    let nextNode: Node | null = sentenceElement?.nextSibling || null;
    while (nextNode && nextNode.nodeType === 3) {
      // Text node type is 3
      const textContent = (nextNode as Text).data;
      if (textContent.trim() !== "") {
        throw new Error(
          `Non-whitespace text node found between sentence and button: "${textContent}"`,
        );
      }
      nextNode = nextNode.nextSibling;
    }
  });
});
