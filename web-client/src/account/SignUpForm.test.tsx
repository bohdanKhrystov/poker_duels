import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
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
  SIGN_UP_FAILED,
} from "./account-text";
import { ProfileStrip } from "../profile/ProfileStrip";
import { aProfile } from "../profile/profile-fixture";

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
    fireEvent.click(submitButton);
    fireEvent.click(submitButton);

    expect(signUp).toHaveBeenCalledTimes(1);

    resolveSignUp({ kind: "signed-up" });
    await screen.findByText(SIGNED_UP);
  });
});
