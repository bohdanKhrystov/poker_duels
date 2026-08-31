import { act, fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SignInForm } from "./SignInForm";
import type { SignInOutcome } from "./sign-in";
import {
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGN_IN_LABEL,
  SIGN_IN_REFUSED,
  SIGN_UP_FAILED,
} from "./account-text";

/**
 * A `refused` outcome carrying an extra `reason` the real `SignInOutcome` type never has:
 * `ADR-0027` §6 makes a wrong password and an unknown handle indistinguishable on the wire, so
 * `{ kind: "refused" }` is the whole of what a real server ever sends. `reason` is assigned
 * through a variable rather than returned as a literal, so TypeScript's excess-property check
 * does not strip it — the field has to survive at runtime, invisible through the type, so a
 * fixture can prove the form never reaches for it (see the "leak the reason" mutation below).
 */
function refusedBecause(reason: string): SignInOutcome {
  const outcome = { kind: "refused" as const, reason };
  return outcome;
}

function submitButton(): HTMLElement {
  return screen.getByRole("button", { name: SIGN_IN_LABEL });
}

describe("signing in", () => {
  it("sends the handle and the password the player typed", async () => {
    const signIn = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    render(<SignInForm signIn={signIn} />);

    fireEvent.change(screen.getByLabelText(HANDLE_LABEL), {
      target: { value: "duel-otter" },
    });
    fireEvent.change(screen.getByLabelText(PASSWORD_LABEL), {
      target: { value: "river-card-flush-9" },
    });
    await act(async () => {
      fireEvent.click(submitButton());
    });

    // Two different strings, in order — a swap would still pass a test that
    // used the same string for both fields.
    expect(signIn).toHaveBeenCalledWith("duel-otter", "river-card-flush-9");
  });

  it("says the same sentence to a wrong password and to an unknown handle", async () => {
    // Two inputs, not one, and the two mocked refusals carry distinguishable
    // detail a real server never sends (see `refusedBecause`) — a fixture
    // that resolved both calls with the identical object could not tell a
    // component that renders a constant from one that renders a copy.
    const signIn = vi
      .fn(async (): Promise<SignInOutcome> => ({ kind: "signed-in" }))
      .mockResolvedValueOnce(refusedBecause("no account holds that handle"))
      .mockResolvedValueOnce(
        refusedBecause("that password does not match the account"),
      );
    render(<SignInForm signIn={signIn} />);

    const handleInput = screen.getByLabelText(HANDLE_LABEL);
    const passwordInput = screen.getByLabelText(PASSWORD_LABEL);

    fireEvent.change(handleInput, {
      target: { value: "totally-unregistered-handle" },
    });
    fireEvent.change(passwordInput, {
      target: { value: "any-password-at-all-1" },
    });
    await act(async () => {
      fireEvent.click(submitButton());
    });
    const first = screen.getByRole("status").textContent;

    fireEvent.change(handleInput, {
      target: { value: "a-real-registered-handle" },
    });
    fireEvent.change(passwordInput, {
      target: { value: "the-wrong-password-2" },
    });
    await act(async () => {
      fireEvent.click(submitButton());
    });
    const second = screen.getByRole("status").textContent;

    // The one sentence, gated on the screen — not merely equal to each
    // other, but equal to the named constant the copy module owns.
    expect(first).toBe(SIGN_IN_REFUSED);
    expect(second).toBe(SIGN_IN_REFUSED);
    // ...and gated in the copy module too: a verdict that named one field
    // would mention it without the other, which this asymmetry check
    // catches even though the honest sentence mentions both words at once.
    expect(/handle/i.test(first ?? "")).toBe(/password/i.test(first ?? ""));
  });

  it("marks neither field, because the server named neither", async () => {
    const signIn = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "refused",
    }));
    const { container } = render(<SignInForm signIn={signIn} />);

    await act(async () => {
      fireEvent.click(submitButton());
    });

    const handleInput = screen.getByLabelText(HANDLE_LABEL);
    const passwordInput = screen.getByLabelText(PASSWORD_LABEL);

    expect(handleInput.hasAttribute("aria-invalid")).toBe(false);
    expect(passwordInput.hasAttribute("aria-invalid")).toBe(false);

    // The whole screen, not one known string: a per-field message beside
    // either input — in any wording, not only via aria-invalid — would add
    // text this equality does not expect, which a substring or a count of
    // one known string would miss. Composed from the same copy-module
    // constants the component renders, so nothing here is retyped as a
    // literal. Whitespace is normalized to absorb an incidental text node at
    // an element boundary, not to hide an added word — today's raw,
    // unnormalized DOM text already matches with no separator at all.
    const normalize = (text: string) => text.replace(/\s+/g, " ").trim();
    const rendered = normalize(container.textContent ?? "");
    const onlyTheKnownComposition = normalize(
      [SIGN_IN_REFUSED, HANDLE_LABEL, PASSWORD_LABEL, SIGN_IN_LABEL].join(""),
    );
    expect(rendered).toBe(onlyTheKnownComposition);
  });

  it("tells a refusal from a broken server", async () => {
    const signIn = vi
      .fn(async (): Promise<SignInOutcome> => ({ kind: "refused" }))
      .mockResolvedValueOnce({ kind: "refused" })
      .mockResolvedValueOnce({ kind: "failed" });
    render(<SignInForm signIn={signIn} />);

    await act(async () => {
      fireEvent.click(submitButton());
    });
    const refusedText = screen.getByRole("status").textContent;

    await act(async () => {
      fireEvent.click(submitButton());
    });
    const failedText = screen.getByRole("status").textContent;

    expect(refusedText).not.toBe(failedText);
    expect(refusedText).toBe(SIGN_IN_REFUSED);
    expect(failedText).toBe(SIGN_UP_FAILED);
  });

  it("offers one credential and holds no space for another", () => {
    const signIn = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    const { container } = render(<SignInForm signIn={signIn} />);

    // Exactly two credential inputs and one submit — a third input or a
    // second button would be the space `ADR-0041` refuses to hold open for
    // a provider that does not exist yet.
    expect(container.querySelectorAll("input")).toHaveLength(2);
    expect(screen.getAllByRole("button")).toHaveLength(1);

    const text = container.textContent ?? "";
    expect(text).not.toMatch(/continue with/i);
    expect(text).not.toMatch(/google/i);
    expect(text).not.toMatch(/apple/i);
    expect(text).not.toMatch(/github/i);
    // STORY-0417's door, refused here on purpose.
    expect(text).not.toMatch(/forgot/i);
  });

  it("sends nothing on a second submit while one is in flight", async () => {
    let resolveSignIn: (outcome: SignInOutcome) => void = () => {};
    const signIn = vi.fn(
      () =>
        new Promise<SignInOutcome>((resolve) => {
          resolveSignIn = resolve;
        }),
    );
    render(<SignInForm signIn={signIn} />);

    // Both dispatches share one outer act(): a bare fireEvent is wrapped in
    // its own act() by testing-library, which would flush `isSubmitting`
    // between the two clicks and let `disabled` — not the in-flight ref
    // this test exists to prove — suppress the second click. Nesting both
    // here fires the second click before React has re-rendered from the
    // first, so the ref guard is what stands between them.
    act(() => {
      fireEvent.click(submitButton());
      fireEvent.click(submitButton());
    });

    expect(signIn).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveSignIn({ kind: "signed-in" });
    });
  });

  it("puts nothing in the DOM that tells the two refusals apart", async () => {
    // Two mocked refusals with different `reason` strings — a real server
    // sends neither field, but the fixture carries them so a test can prove
    // the form never reaches for them. React reflects a controlled input's
    // `value` into the DOM attribute, so the two attempts must type the
    // exact same credentials; otherwise the markup differs over the player's
    // keystrokes alone, never over any leak.
    const signIn = vi
      .fn(async (): Promise<SignInOutcome> => ({ kind: "signed-in" }))
      .mockResolvedValueOnce(refusedBecause("one reason string"))
      .mockResolvedValueOnce(refusedBecause("a different reason string"));
    const { container } = render(<SignInForm signIn={signIn} />);

    const handleInput = screen.getByLabelText(HANDLE_LABEL);
    const passwordInput = screen.getByLabelText(PASSWORD_LABEL);
    const same = "fixed-handle";
    const samePassword = "fixed-password";

    fireEvent.change(handleInput, { target: { value: same } });
    fireEvent.change(passwordInput, { target: { value: samePassword } });
    await act(async () => {
      fireEvent.click(submitButton());
    });
    const firstMarkup = container.innerHTML;

    fireEvent.change(handleInput, { target: { value: same } });
    fireEvent.change(passwordInput, { target: { value: samePassword } });
    await act(async () => {
      fireEvent.click(submitButton());
    });
    const secondMarkup = container.innerHTML;

    // Guarded against vacuity: if the refusal is never rendered, both would
    // be trivially equal.
    expect(firstMarkup).toContain(SIGN_IN_REFUSED);
    expect(secondMarkup).toContain(SIGN_IN_REFUSED);
    expect(firstMarkup).toBe(secondMarkup);
  });

  it("the sign-in submit is the card's fill button", () => {
    const signIn = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    render(<SignInForm signIn={signIn} />);

    const button = submitButton();

    // The submit's class list contains bg-accent-fill, contains text-on-accent,
    // and does not contain bg-surface — three membership checks against named
    // tokens, the third pinning that the quiet recipe went away rather than
    // gained a neighbour.
    expect(button.classList.contains("bg-accent-fill")).toBe(true);
    expect(button.classList.contains("text-on-accent")).toBe(true);
    expect(button.classList.contains("bg-surface")).toBe(false);
  });

  it("the sign-in fields are left-aligned and their labels are muted", () => {
    const signIn = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    const { container } = render(<SignInForm signIn={signIn} />);

    const handleLabel = screen.getByText(HANDLE_LABEL);
    const passwordLabel = screen.getByText(PASSWORD_LABEL);

    // Both labels sit in the one wrapper that also holds their inputs — the
    // fix left-aligns that field block, not the panel.
    const fieldWrapper = handleLabel.parentElement;
    expect(fieldWrapper).not.toBeNull();
    expect(passwordLabel.parentElement).toBe(fieldWrapper);
    expect(fieldWrapper?.classList.contains("text-left")).toBe(true);

    // Both labels read as muted, the way the card's `.field label` rule does.
    expect(handleLabel.classList.contains("text-text-muted")).toBe(true);
    expect(passwordLabel.classList.contains("text-text-muted")).toBe(true);

    // The panel itself still carries the centring the heading and the
    // refusal box depend on — proving the fix is the field block moving,
    // not a blanket un-centring of the whole card.
    const panel = container.querySelector(
      '[aria-label="sign in to an account"]',
    );
    expect(panel).not.toBeNull();
    expect(panel?.classList.contains("text-center")).toBe(true);
  });
});
