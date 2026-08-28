import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { AccountProvider, useAccount } from "./account-provider";
import type { AccountCalls } from "./account-provider";
import type { SignUpOutcome } from "./sign-up";
import type { SignInOutcome } from "./sign-in";
import type { SignOutOutcome } from "./sign-out";
import type { RevokeOutcome } from "./revoke-device";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";
import type { ForgotPasswordOutcome } from "./forgot-password";
import type { VerifyEmailOutcome } from "./verify-email";
import type { ResetPasswordOutcome } from "./reset-password";

/** Renders what `useAccount()` answers, and calls it when mounted. */
function Probe(props: {
  onCall?: (calls: ReturnType<typeof useAccount>) => void;
  onRerender?: () => void;
}): ReactElement {
  const calls = useAccount();
  if (props.onCall) {
    props.onCall(calls);
  }
  if (calls === null) {
    return <p>no account calls</p>;
  }
  return <p>has account calls</p>;
}

describe("the account calls", () => {
  it("hands down the four calls it was given, and the same references twice", async () => {
    const signUpSpy = vi.fn(async (): Promise<SignUpOutcome> => ({
      kind: "signed-up",
    }));
    const signInSpy = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    const signOutSpy = vi.fn(async (): Promise<SignOutOutcome> => ({
      kind: "signed-out",
    }));
    const revokeThisDeviceSpy = vi.fn(async (): Promise<RevokeOutcome> => ({
      kind: "revoked",
    }));

    const calls = {
      signUp: signUpSpy,
      signIn: signInSpy,
      signOut: signOutSpy,
      revokeThisDevice: revokeThisDeviceSpy,
      attachRecoveryEmail:
        vi.fn() as unknown as AccountCalls["attachRecoveryEmail"],
      forgotPassword: vi.fn() as unknown as AccountCalls["forgotPassword"],
      verifyEmail: vi.fn() as unknown as AccountCalls["verifyEmail"],
      resetPassword: vi.fn() as unknown as AccountCalls["resetPassword"],
    };

    let receivedCalls: AccountCalls | null = null;

    const { rerender } = render(
      <AccountProvider calls={calls}>
        <Probe
          onCall={(c) => {
            receivedCalls = c;
          }}
        />
      </AccountProvider>,
    );

    // First render: verify all four calls are passed by reference
    expect(receivedCalls).toBe(calls);
    expect(receivedCalls!.signUp).toBe(signUpSpy);
    expect(receivedCalls!.signIn).toBe(signInSpy);
    expect(receivedCalls!.signOut).toBe(signOutSpy);
    expect(receivedCalls!.revokeThisDevice).toBe(revokeThisDeviceSpy);

    const firstRenderCalls = receivedCalls;

    // Force re-render with same calls object
    receivedCalls = null;
    rerender(
      <AccountProvider calls={calls}>
        <Probe
          onCall={(c) => {
            receivedCalls = c;
          }}
        />
      </AccountProvider>,
    );

    // Second render: verify the object reference is identical
    expect(receivedCalls).toBe(firstRenderCalls);
    expect(receivedCalls!.signUp).toBe(signUpSpy);
    expect(receivedCalls!.signIn).toBe(signInSpy);
    expect(receivedCalls!.signOut).toBe(signOutSpy);
    expect(receivedCalls!.revokeThisDevice).toBe(revokeThisDeviceSpy);
  });

  it("answers with nothing where no provider is above", () => {
    const onCall = vi.fn();

    render(<Probe onCall={onCall} />);

    // The probe should get null and not throw
    expect(screen.getByText("no account calls")).toBeDefined();
    expect(onCall).toHaveBeenCalledWith(null);
  });

  it("calls the function that was passed, with the arguments it was given", async () => {
    const signUpSpy = vi.fn(async (): Promise<SignUpOutcome> => ({
      kind: "signed-up",
    }));
    const signInSpy = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    const signOutSpy = vi.fn(async (): Promise<SignOutOutcome> => ({
      kind: "signed-out",
    }));
    const revokeThisDeviceSpy = vi.fn(async (): Promise<RevokeOutcome> => ({
      kind: "revoked",
    }));

    const calls = {
      signUp: signUpSpy,
      signIn: signInSpy,
      signOut: signOutSpy,
      revokeThisDevice: revokeThisDeviceSpy,
      attachRecoveryEmail:
        vi.fn() as unknown as AccountCalls["attachRecoveryEmail"],
      forgotPassword: vi.fn() as unknown as AccountCalls["forgotPassword"],
      verifyEmail: vi.fn() as unknown as AccountCalls["verifyEmail"],
      resetPassword: vi.fn() as unknown as AccountCalls["resetPassword"],
    };

    let receivedCalls: AccountCalls | null = null;

    render(
      <AccountProvider calls={calls}>
        <Probe
          onCall={(c) => {
            receivedCalls = c;
          }}
        />
      </AccountProvider>,
    );

    // Call signUp with two different argument values
    const result = await receivedCalls!.signUp("h", "p");
    expect(result.kind).toBe("signed-up");
    expect(signUpSpy).toHaveBeenCalledWith("h", "p");
    // Verify the exact order and values
    expect(signUpSpy).toHaveBeenCalledWith("h", "p");
  });

  it("keeps the four apart", async () => {
    const signUpSpy = vi.fn(async (): Promise<SignUpOutcome> => ({
      kind: "signed-up",
    }));
    const signInSpy = vi.fn(async (): Promise<SignInOutcome> => ({
      kind: "signed-in",
    }));
    const signOutSpy = vi.fn(async (): Promise<SignOutOutcome> => ({
      kind: "signed-out",
    }));
    const revokeThisDeviceSpy = vi.fn(async (): Promise<RevokeOutcome> => ({
      kind: "revoked",
    }));

    const calls = {
      signUp: signUpSpy,
      signIn: signInSpy,
      signOut: signOutSpy,
      revokeThisDevice: revokeThisDeviceSpy,
      attachRecoveryEmail:
        vi.fn() as unknown as AccountCalls["attachRecoveryEmail"],
      forgotPassword: vi.fn() as unknown as AccountCalls["forgotPassword"],
      verifyEmail: vi.fn() as unknown as AccountCalls["verifyEmail"],
      resetPassword: vi.fn() as unknown as AccountCalls["resetPassword"],
    };

    let receivedCalls: AccountCalls | null = null;

    render(
      <AccountProvider calls={calls}>
        <Probe
          onCall={(c) => {
            receivedCalls = c;
          }}
        />
      </AccountProvider>,
    );

    // Verify that signOut and revokeThisDevice are different references
    expect(receivedCalls!.signOut).toBe(signOutSpy);
    expect(receivedCalls!.revokeThisDevice).toBe(revokeThisDeviceSpy);
    expect(receivedCalls!.signOut).not.toBe(receivedCalls!.revokeThisDevice);

    // Call signOut
    await receivedCalls!.signOut();

    // Verify signOut was called once
    expect(signOutSpy).toHaveBeenCalledTimes(1);
    // Verify revokeThisDevice was NOT called
    expect(revokeThisDeviceSpy).toHaveBeenCalledTimes(0);
  });

  it("hands every recovery call down unchanged, by reference", () => {
    const attachRecoveryEmailSpy = vi.fn(
      async (): Promise<AttachRecoveryOutcome> => ({ kind: "accepted" }),
    );
    const forgotPasswordSpy = vi.fn(
      async (): Promise<ForgotPasswordOutcome> => ({ kind: "accepted" }),
    );
    const verifyEmailSpy = vi.fn(async (): Promise<VerifyEmailOutcome> => ({
      kind: "verified",
    }));
    const resetPasswordSpy = vi.fn(async (): Promise<ResetPasswordOutcome> => ({
      kind: "reset",
    }));

    const calls: AccountCalls = {
      signUp: vi.fn() as unknown as AccountCalls["signUp"],
      signIn: vi.fn() as unknown as AccountCalls["signIn"],
      signOut: vi.fn() as unknown as AccountCalls["signOut"],
      revokeThisDevice: vi.fn() as unknown as AccountCalls["revokeThisDevice"],
      attachRecoveryEmail: attachRecoveryEmailSpy,
      forgotPassword: forgotPasswordSpy,
      verifyEmail: verifyEmailSpy,
      resetPassword: resetPasswordSpy,
    };

    let receivedCalls: AccountCalls | null = null;

    render(
      <AccountProvider calls={calls}>
        <Probe
          onCall={(c) => {
            receivedCalls = c;
          }}
        />
      </AccountProvider>,
    );

    // Each recovery member is the exact spy that went in.
    expect(receivedCalls!.attachRecoveryEmail).toBe(attachRecoveryEmailSpy);
    expect(receivedCalls!.forgotPassword).toBe(forgotPasswordSpy);
    expect(receivedCalls!.verifyEmail).toBe(verifyEmailSpy);
    expect(receivedCalls!.resetPassword).toBe(resetPasswordSpy);

    // Pairwise distinct: a provider that collapsed two members onto one
    // function, or a fixture that reused one spy for two fields, reddens
    // here even where the four toBe's above happen not to catch it.
    expect(receivedCalls!.attachRecoveryEmail).not.toBe(
      receivedCalls!.forgotPassword,
    );
    expect(receivedCalls!.attachRecoveryEmail).not.toBe(
      receivedCalls!.verifyEmail,
    );
    expect(receivedCalls!.attachRecoveryEmail).not.toBe(
      receivedCalls!.resetPassword,
    );
    expect(receivedCalls!.forgotPassword).not.toBe(receivedCalls!.verifyEmail);
    expect(receivedCalls!.forgotPassword).not.toBe(
      receivedCalls!.resetPassword,
    );
    expect(receivedCalls!.verifyEmail).not.toBe(receivedCalls!.resetPassword);
  });
});
