import { act, fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CANCEL, PASSWORD_LABEL } from "./account-text";
import { ForgotPasswordForm } from "./ForgotPasswordForm";
import {
  ADDRESS_LABEL,
  CURRENT_PASSWORD_LABEL,
  FORGOT_PASSWORD_ACKNOWLEDGED,
  FORGOT_PASSWORD_FAILED,
  FORGOT_PASSWORD_LABEL,
  FORGOT_PASSWORD_SUBMIT,
} from "./recovery-text";

// Leading space, trailing space, a trailing dot and capital letters: a fixed
// point of no trim/lowercase/normalisation this component might apply, so a
// mutation that adds one is visible on this fixture and would not be on an
// already-trimmed, already-lowercase one.
const ADDRESS = " Zqx-Address-Zqx. ";

describe("ForgotPasswordForm", () => {
  it("sends what was typed, once, exactly as it was typed", async () => {
    const forgotPassword = vi.fn().mockResolvedValue({ kind: "accepted" });
    const { getByLabelText, getByRole } = render(
      <ForgotPasswordForm forgotPassword={forgotPassword} onCancel={vi.fn()} />,
    );

    getByRole("heading", { name: FORGOT_PASSWORD_LABEL });
    fireEvent.change(getByLabelText(ADDRESS_LABEL), {
      target: { value: ADDRESS },
    });
    await act(async () => {
      fireEvent.click(getByRole("button", { name: FORGOT_PASSWORD_SUBMIT }));
    });

    expect(forgotPassword).toHaveBeenCalledTimes(1);
    expect(forgotPassword).toHaveBeenCalledWith(ADDRESS);
  });

  it("says the same thing for every address it is given", async () => {
    async function acknowledgementFor(address: string): Promise<string | null> {
      const forgotPassword = vi.fn().mockResolvedValue({ kind: "accepted" });
      const { getByLabelText, getByRole, unmount } = render(
        <ForgotPasswordForm
          forgotPassword={forgotPassword}
          onCancel={vi.fn()}
        />,
      );
      fireEvent.change(getByLabelText(ADDRESS_LABEL), {
        target: { value: address },
      });
      await act(async () => {
        fireEvent.click(getByRole("button", { name: FORGOT_PASSWORD_SUBMIT }));
      });
      const text = getByRole("status").textContent;
      unmount();
      return text;
    }

    // Lengths 18, 11 and 15: two odd, one even, so a branch on the address's
    // length (odd or even) is exercised by this fixture and not hidden by it.
    const first = await acknowledgementFor(ADDRESS);
    const second = await acknowledgementFor("a-third-one");
    const third = await acknowledgementFor("yet-another-one");

    expect(first).toBe(FORGOT_PASSWORD_ACKNOWLEDGED);
    expect(second).toBe(FORGOT_PASSWORD_ACKNOWLEDGED);
    expect(third).toBe(FORGOT_PASSWORD_ACKNOWLEDGED);
    expect(first).toBe(second);
    expect(second).toBe(third);
  });

  it("keeps the form and what was typed after it is acknowledged", async () => {
    const forgotPassword = vi.fn().mockResolvedValue({ kind: "accepted" });
    const { getByLabelText, getByRole, findByText } = render(
      <ForgotPasswordForm forgotPassword={forgotPassword} onCancel={vi.fn()} />,
    );

    const input = getByLabelText(ADDRESS_LABEL) as HTMLInputElement;
    fireEvent.change(input, { target: { value: ADDRESS } });
    await act(async () => {
      fireEvent.click(getByRole("button", { name: FORGOT_PASSWORD_SUBMIT }));
    });
    await findByText(FORGOT_PASSWORD_ACKNOWLEDGED);

    expect(input.value).toBe(ADDRESS);
    const submit = getByRole("button", {
      name: FORGOT_PASSWORD_SUBMIT,
    }) as HTMLButtonElement;
    expect(submit.disabled).toBe(false);
    getByLabelText(ADDRESS_LABEL);
    getByRole("button", { name: CANCEL });
  });

  it("says the request did not go through, and keeps the form and what was typed", async () => {
    const forgotPassword = vi.fn().mockResolvedValue({ kind: "failed" });
    const { getByLabelText, getByRole, findByText, queryByText } = render(
      <ForgotPasswordForm forgotPassword={forgotPassword} onCancel={vi.fn()} />,
    );

    const input = getByLabelText(ADDRESS_LABEL) as HTMLInputElement;
    fireEvent.change(input, { target: { value: ADDRESS } });
    await act(async () => {
      fireEvent.click(getByRole("button", { name: FORGOT_PASSWORD_SUBMIT }));
    });
    await findByText(FORGOT_PASSWORD_FAILED);

    expect(queryByText(FORGOT_PASSWORD_ACKNOWLEDGED)).toBeNull();
    expect(input.value).toBe(ADDRESS);
    const submit = getByRole("button", {
      name: FORGOT_PASSWORD_SUBMIT,
    }) as HTMLButtonElement;
    expect(submit.disabled).toBe(false);
  });

  it("asks for an address and never for a password", async () => {
    const forgotPassword = vi.fn().mockResolvedValue({ kind: "accepted" });
    const {
      container,
      getByLabelText,
      queryByLabelText,
      getByRole,
      findByText,
    } = render(
      <ForgotPasswordForm forgotPassword={forgotPassword} onCancel={vi.fn()} />,
    );

    function assertOneAddressFieldAndNoPassword(): void {
      getByLabelText(ADDRESS_LABEL);
      expect(container.querySelectorAll("input").length).toBe(1);
      expect(container.querySelector('input[type="password"]')).toBeNull();
      expect(queryByLabelText(PASSWORD_LABEL)).toBeNull();
      expect(queryByLabelText(CURRENT_PASSWORD_LABEL)).toBeNull();
    }

    assertOneAddressFieldAndNoPassword();

    fireEvent.change(getByLabelText(ADDRESS_LABEL), {
      target: { value: ADDRESS },
    });
    await act(async () => {
      fireEvent.click(getByRole("button", { name: FORGOT_PASSWORD_SUBMIT }));
    });
    await findByText(FORGOT_PASSWORD_ACKNOWLEDGED);

    assertOneAddressFieldAndNoPassword();
  });

  it("offers the way back, and sends nothing when it is taken", () => {
    const forgotPassword = vi.fn();
    const onCancel = vi.fn();
    const { getByRole } = render(
      <ForgotPasswordForm
        forgotPassword={forgotPassword}
        onCancel={onCancel}
      />,
    );

    fireEvent.click(getByRole("button", { name: CANCEL }));

    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(forgotPassword).toHaveBeenCalledTimes(0);
  });

  it("sends nothing twice, however fast the form is submitted", async () => {
    const forgotPassword = vi.fn().mockReturnValue(new Promise(() => {}));
    const { container, getByLabelText } = render(
      <ForgotPasswordForm forgotPassword={forgotPassword} onCancel={vi.fn()} />,
    );

    fireEvent.change(getByLabelText(ADDRESS_LABEL), {
      target: { value: ADDRESS },
    });

    const form = container.querySelector("form");
    if (form === null) {
      throw new Error("ForgotPasswordForm did not render a form");
    }

    await act(async () => {
      fireEvent.submit(form);
      fireEvent.submit(form);
      fireEvent.submit(form);
    });

    expect(forgotPassword).toHaveBeenCalledTimes(1);
  });
});
