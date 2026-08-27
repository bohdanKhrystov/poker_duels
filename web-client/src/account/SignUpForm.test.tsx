import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { SignUpForm } from "./SignUpForm";
import type { SignUpOutcome } from "./sign-up";
import {
  HANDLE_LABEL,
  PASSWORD_LABEL,
  SIGN_UP_LABEL,
  SIGNED_UP,
  HANDLE_REFUSED,
  HANDLE_UNAVAILABLE,
  PASSWORD_REFUSED,
  NO_PROFILE_YET,
  SIGN_UP_THROTTLED,
  SIGN_UP_FAILED,
} from "./account-text";
import { ProfileStrip } from "../profile/ProfileStrip";
import { aProfile } from "../profile/profile-fixture";

/**
 * An `unavailable-handle` outcome carrying an extra `reason` the real `SignUpOutcome` type never has:
 * `ADR-0031` §2's indistinguishability note makes `{ kind: "unavailable-handle" }` the whole of what
 * a real server sends — one sentence maps to two world-states, so the outcome carries no disambiguating
 * field. `reason` is assigned through a variable rather than returned as a literal, so TypeScript's
 * excess-property check does not strip it — the field has to survive at runtime, invisible through the
 * type, so a fixture can prove the form never reaches for it.
 */
function unavailableHandleBecause(reason: string): SignUpOutcome {
  const outcome = { kind: "unavailable-handle" as const, reason };
  return outcome;
}

describe("signing up for an account", () => {
  it("sends the handle and the password the player typed", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });

    render(<SignUpForm signUp={signUp} />);

    fireEvent.change(screen.getByLabelText(HANDLE_LABEL), {
      target: { value: "ada-lovelace" },
    });
    fireEvent.change(screen.getByLabelText(PASSWORD_LABEL), {
      target: { value: "correct-horse-battery" },
    });
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));

    // Exactly the two typed strings, in call order (handle, then password) —
    // a form that swapped them or dropped one would fail this line.
    expect(signUp).toHaveBeenCalledWith(
      "ada-lovelace",
      "correct-horse-battery",
    );

    // Let the resolved promise settle before the test tears the tree down.
    await screen.findByText(SIGNED_UP);
  });

  it("leaves the coin balance and the name exactly as they were", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });

    render(
      <>
        <ProfileStrip
          state={{
            kind: "profile",
            profile: aProfile({ coinBalance: 41, displayName: "Ada" }),
            duels: [],
          }}
        />
        <SignUpForm signUp={signUp} />
      </>,
    );

    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(SIGNED_UP);

    expect(screen.getByText("41 Duel coins")).toBeDefined();
    expect(screen.getByText("Ada")).toBeDefined();
  });

  it("offers one credential and holds no space for another", () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });

    const { container } = render(<SignUpForm signUp={signUp} />);

    const credentialInputs = container.querySelectorAll(
      'input[type="text"], input[type="password"]',
    );
    expect(credentialInputs).toHaveLength(2);

    const submitControls = container.querySelectorAll('button[type="submit"]');
    expect(submitControls).toHaveLength(1);

    const text = container.textContent ?? "";
    expect(text).not.toMatch(/continue with/i);
    expect(text).not.toMatch(/or sign (in|up) with/i);
    expect(text).not.toMatch(/google/i);
    expect(text).not.toMatch(/apple/i);
    expect(text).not.toMatch(/github/i);
  });

  it("never fills one field from the other", () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "signed-up" });

    render(
      <>
        <ProfileStrip
          state={{
            kind: "profile",
            profile: aProfile({ displayName: "Ada" }),
            duels: [],
          }}
        />
        <SignUpForm signUp={signUp} />
      </>,
    );

    const handleInput = screen.getByLabelText(HANDLE_LABEL) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(
      PASSWORD_LABEL,
    ) as HTMLInputElement;

    expect(handleInput.value).toBe("");
    expect(passwordInput.value).toBe("");

    fireEvent.change(handleInput, { target: { value: "someone" } });
    expect(passwordInput.value).toBe("");

    fireEvent.change(passwordInput, { target: { value: "a-password" } });
    expect(handleInput.value).toBe("someone");
  });

  it("says one sentence per refusal, and replaces it on the next attempt", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp
      .mockResolvedValueOnce({ kind: "handle-refused" })
      .mockResolvedValueOnce({ kind: "unavailable-handle" });

    render(<SignUpForm signUp={signUp} />);

    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(HANDLE_REFUSED);

    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(HANDLE_UNAVAILABLE);

    expect(screen.queryByText(HANDLE_REFUSED)).toBeNull();
  });

  it("maps each refusal to its own sentence", async () => {
    const handleRefused = vi.fn<[string, string], Promise<SignUpOutcome>>();
    handleRefused.mockResolvedValue({ kind: "handle-refused" });
    render(<SignUpForm signUp={handleRefused} />);
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(HANDLE_REFUSED);
    cleanup();

    const unavailableHandle = vi.fn<[string, string], Promise<SignUpOutcome>>();
    unavailableHandle.mockResolvedValue({ kind: "unavailable-handle" });
    render(<SignUpForm signUp={unavailableHandle} />);
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(HANDLE_UNAVAILABLE);
    cleanup();

    const passwordRefused = vi.fn<[string, string], Promise<SignUpOutcome>>();
    passwordRefused.mockResolvedValue({ kind: "password-refused" });
    render(<SignUpForm signUp={passwordRefused} />);
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(PASSWORD_REFUSED);
    cleanup();

    const noProfile = vi.fn<[string, string], Promise<SignUpOutcome>>();
    noProfile.mockResolvedValue({ kind: "no-profile" });
    render(<SignUpForm signUp={noProfile} />);
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(NO_PROFILE_YET);
    cleanup();

    const failed = vi.fn<[string, string], Promise<SignUpOutcome>>();
    failed.mockResolvedValue({ kind: "failed" });
    render(<SignUpForm signUp={failed} />);
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(SIGN_UP_FAILED);
  });

  it("sends nothing on a second submit while one is in flight", async () => {
    let resolveSignUp: (outcome: SignUpOutcome) => void = () => {};
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>(
      () =>
        new Promise<SignUpOutcome>((resolve) => {
          resolveSignUp = resolve;
        }),
    );

    render(<SignUpForm signUp={signUp} />);

    const submitButton = screen.getByRole("button", { name: SIGN_UP_LABEL });
    // Both dispatches inside one outer `act()`: fireEvent's own act() wrapping
    // nests inside it and defers its flush to the outer call, so the second
    // click's handler runs against the same pre-render closure as the first —
    // the actual race a synchronous ref (and not state) is guarding against.
    act(() => {
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
    });

    expect(signUp).toHaveBeenCalledTimes(1);

    resolveSignUp({ kind: "signed-up" });
    await screen.findByText(SIGNED_UP);
  });

  it("tells a deliberate refusal from a broken product, on the screen", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp
      .mockResolvedValueOnce({ kind: "throttled" })
      .mockResolvedValueOnce({ kind: "failed" })
      .mockResolvedValueOnce({ kind: "failed" })
      .mockResolvedValueOnce({ kind: "failed" });

    render(<SignUpForm signUp={signUp} />);
    const submitButton = screen.getByRole("button", { name: SIGN_UP_LABEL });

    // A 429, reached through the double as `throttled`.
    fireEvent.click(submitButton);
    await screen.findByText(SIGN_UP_THROTTLED);
    expect(screen.queryByText(SIGN_UP_FAILED)).toBeNull();

    // A 500, reached through the double as `failed`.
    fireEvent.click(submitButton);
    await screen.findByText(SIGN_UP_FAILED);
    expect(screen.queryByText(SIGN_UP_THROTTLED)).toBeNull();

    // A 503, reached through the double as `failed`.
    fireEvent.click(submitButton);
    await screen.findByText(SIGN_UP_FAILED);
    expect(screen.queryByText(SIGN_UP_THROTTLED)).toBeNull();

    // A rejected fetch, reached through the double as `failed`.
    fireEvent.click(submitButton);
    await screen.findByText(SIGN_UP_FAILED);
    expect(screen.queryByText(SIGN_UP_THROTTLED)).toBeNull();
  });

  it("keeps both fields exactly as they were typed", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "throttled" });

    render(<SignUpForm signUp={signUp} />);

    const handleInput = screen.getByLabelText(HANDLE_LABEL) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(
      PASSWORD_LABEL,
    ) as HTMLInputElement;

    fireEvent.change(handleInput, { target: { value: "grace-hopper" } });
    // Not the fixture default (the empty string every field starts from), and
    // holding characters a re-mask would eat.
    fireEvent.change(passwordInput, {
      target: { value: "Tr0ub4dor&3-zz!" },
    });
    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));

    await screen.findByText(SIGN_UP_THROTTLED);

    expect(handleInput.value).toBe("grace-hopper");
    expect(passwordInput.value).toBe("Tr0ub4dor&3-zz!");
  });

  it("marks neither field and says nothing beside either one", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "throttled" });

    render(<SignUpForm signUp={signUp} />);

    fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    await screen.findByText(SIGN_UP_THROTTLED);

    const handleInput = screen.getByLabelText(HANDLE_LABEL) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(
      PASSWORD_LABEL,
    ) as HTMLInputElement;

    expect(handleInput.getAttribute("aria-invalid")).toBeNull();
    expect(passwordInput.getAttribute("aria-invalid")).toBeNull();
    expect(screen.getAllByRole("status")).toHaveLength(1);
  });

  it("sends nothing more until the player submits again", async () => {
    const signUp = vi.fn<[string, string], Promise<SignUpOutcome>>();
    signUp.mockResolvedValue({ kind: "throttled" });

    render(<SignUpForm signUp={signUp} />);
    const submitButton = screen.getByRole("button", {
      name: SIGN_UP_LABEL,
    }) as HTMLButtonElement;

    // Fake timers from before the throttled outcome settles, so a `setTimeout`
    // scheduled the moment it settles is one this test's own advance would
    // reach — not a real one racing the assertions below regardless.
    vi.useFakeTimers();
    try {
      await act(async () => {
        fireEvent.click(submitButton);
        // The double's promise is already resolved; one microtask turn is
        // enough for its `.then` to run and settle the outcome.
        await Promise.resolve();
      });

      expect(screen.getByText(SIGN_UP_THROTTLED)).toBeDefined();

      act(() => {
        vi.advanceTimersByTime(120_000);
      });

      expect(signUp).toHaveBeenCalledTimes(1);
      expect(submitButton.disabled).toBe(false);
      expect(screen.queryByRole("button", { name: /retry/i })).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it("puts nothing in the DOM that tells two unavailable-handle refusals apart", async () => {
    // Two mocked unavailable-handle outcomes with different `reason` strings — a real server
    // sends neither field, but the fixture carries them so a test can prove the form never
    // reaches for them. React reflects a controlled input's `value` into the DOM attribute,
    // so the two attempts must type the exact same credentials; otherwise the markup differs
    // over the player's keystrokes alone, never over any leak.
    const signUp = vi
      .fn<[string, string], Promise<SignUpOutcome>>()
      .mockResolvedValueOnce(unavailableHandleBecause("handle taken branch"))
      .mockResolvedValueOnce(
        unavailableHandleBecause("profile has password branch"),
      );
    const { container } = render(<SignUpForm signUp={signUp} />);

    const handleInput = screen.getByLabelText(HANDLE_LABEL) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(
      PASSWORD_LABEL,
    ) as HTMLInputElement;
    const same = "fixed-handle";
    const samePassword = "fixed-password";

    fireEvent.change(handleInput, { target: { value: same } });
    fireEvent.change(passwordInput, { target: { value: samePassword } });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    });
    const firstMarkup = container.innerHTML;

    fireEvent.change(handleInput, { target: { value: same } });
    fireEvent.change(passwordInput, { target: { value: samePassword } });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: SIGN_UP_LABEL }));
    });
    const secondMarkup = container.innerHTML;

    // Guarded against vacuity: if the refusal is never rendered, both would
    // be trivially equal.
    expect(firstMarkup).toContain(HANDLE_UNAVAILABLE);
    expect(secondMarkup).toContain(HANDLE_UNAVAILABLE);
    expect(firstMarkup).toBe(secondMarkup);
  });
});
