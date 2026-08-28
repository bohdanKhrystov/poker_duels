import { act, fireEvent, render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PASSWORD_REFUSED } from "./account-text";
import {
  NEW_PASSWORD_LABEL,
  RESET_ENDS_EVERY_SESSION,
  RESET_HEADING,
  RESET_LINK_DEAD,
} from "./recovery-text";
import { ResetScreen } from "./ResetScreen";
import type { ResetPasswordOutcome } from "./reset-password";

const TOKEN = "zqx-reset-token-zqx";
const TYPED = "zqx-new-password-zqx";

describe("ResetScreen", () => {
  it("warns that every session ends before it is asked to do anything", () => {
    const reset = vi.fn();
    const { getByText } = render(
      <ResetScreen token={TOKEN} reset={reset} onDone={vi.fn()} />,
    );

    getByText(RESET_HEADING);
    getByText(RESET_ENDS_EVERY_SESSION);
    expect(reset).toHaveBeenCalledTimes(0);
  });

  it("sends the token it was handed with the password that was typed", async () => {
    const reset = vi.fn().mockResolvedValue({ kind: "reset" });
    const { getByLabelText, getByRole } = render(
      <ResetScreen token={TOKEN} reset={reset} onDone={vi.fn()} />,
    );

    fireEvent.change(getByLabelText(NEW_PASSWORD_LABEL), {
      target: { value: TYPED },
    });
    await act(async () => {
      fireEvent.click(getByRole("button"));
    });

    expect(reset).toHaveBeenCalledTimes(1);
    expect(reset).toHaveBeenCalledWith(TOKEN, TYPED);
  });

  it("reads a refused password and a dead link as two different things", async () => {
    const refused = render(
      <ResetScreen
        token={TOKEN}
        reset={vi.fn().mockResolvedValue({ kind: "password-refused" })}
        onDone={vi.fn()}
      />,
    );
    await act(async () => {
      fireEvent.click(refused.getByRole("button"));
    });
    await refused.findByText(PASSWORD_REFUSED);
    expect(refused.queryByText(RESET_LINK_DEAD)).toBeNull();
    refused.unmount();

    const dead = render(
      <ResetScreen
        token={TOKEN}
        reset={vi.fn().mockResolvedValue({ kind: "link-dead" })}
        onDone={vi.fn()}
      />,
    );
    await act(async () => {
      fireEvent.click(dead.getByRole("button"));
    });
    await dead.findByText(RESET_LINK_DEAD);
    expect(dead.queryByText(PASSWORD_REFUSED)).toBeNull();
    dead.unmount();
  });

  it("lets a refused password be corrected on the same link", async () => {
    const reset = vi
      .fn()
      .mockResolvedValueOnce({ kind: "password-refused" })
      .mockResolvedValueOnce({ kind: "reset" });
    const onDone = vi.fn();
    const { getByLabelText, getByRole, findByText } = render(
      <ResetScreen token={TOKEN} reset={reset} onDone={onDone} />,
    );

    const input = getByLabelText(NEW_PASSWORD_LABEL) as HTMLInputElement;
    fireEvent.change(input, { target: { value: TYPED } });
    await act(async () => {
      fireEvent.click(getByRole("button"));
    });
    await findByText(PASSWORD_REFUSED);

    expect(input.value).toBe(TYPED);
    expect((getByRole("button") as HTMLButtonElement).disabled).toBe(false);

    const secondTyped = `${TYPED}-second`;
    fireEvent.change(input, { target: { value: secondTyped } });
    await act(async () => {
      fireEvent.click(getByRole("button"));
    });

    expect(reset).toHaveBeenCalledTimes(2);
    expect(reset).toHaveBeenNthCalledWith(2, TOKEN, secondTyped);
    expect(onDone).toHaveBeenCalledTimes(1);
  });

  it("sends the player onward on success, and never before", async () => {
    async function callsToOnDone(
      outcome: ResetPasswordOutcome,
    ): Promise<number> {
      const onDone = vi.fn();
      const rendered = render(
        <ResetScreen
          token={TOKEN}
          reset={vi.fn().mockResolvedValue(outcome)}
          onDone={onDone}
        />,
      );
      await act(async () => {
        fireEvent.click(rendered.getByRole("button"));
      });
      rendered.unmount();
      return onDone.mock.calls.length;
    }

    expect(await callsToOnDone({ kind: "reset" })).toBe(1);
    expect(await callsToOnDone({ kind: "link-dead" })).toBe(0);
    expect(await callsToOnDone({ kind: "password-refused" })).toBe(0);
    expect(await callsToOnDone({ kind: "failed" })).toBe(0);
  });

  it("refuses to send with no link, and still says what a reset costs", async () => {
    const reset = vi.fn();
    const { container, getByText, getByRole } = render(
      <ResetScreen token={null} reset={reset} onDone={vi.fn()} />,
    );

    getByText(RESET_ENDS_EVERY_SESSION);
    const button = getByRole("button") as HTMLButtonElement;
    expect(button.disabled).toBe(true);

    // A click on a genuinely disabled button never reaches a submit handler in this
    // environment, which would make the call count below a proof of jsdom's platform
    // behaviour rather than of this component's own guard. Submitting the form directly
    // exercises the guard itself, regardless of what stopped a real click from arriving.
    const form = container.querySelector("form");
    if (form === null) {
      throw new Error("ResetScreen did not render a form");
    }
    await act(async () => {
      fireEvent.submit(form);
    });

    expect(reset).toHaveBeenCalledTimes(0);
  });

  it("puts the token and the password on no part of the screen", async () => {
    const { container, getByLabelText, getByRole, findByText } = render(
      <ResetScreen
        token={TOKEN}
        reset={vi.fn().mockResolvedValue({ kind: "link-dead" })}
        onDone={vi.fn()}
      />,
    );

    fireEvent.change(getByLabelText(NEW_PASSWORD_LABEL), {
      target: { value: TYPED },
    });
    await act(async () => {
      fireEvent.click(getByRole("button"));
    });
    await findByText(RESET_LINK_DEAD);

    expect(container.textContent).not.toContain(TOKEN);
    expect(container.textContent).not.toContain(TYPED);
    expect(container.innerHTML).not.toContain(TOKEN);
  });

  it("sends nothing twice, however fast the control is pressed", async () => {
    const reset = vi
      .fn()
      .mockReturnValue(new Promise<ResetPasswordOutcome>(() => {}));
    const { getByLabelText, getByRole } = render(
      <ResetScreen token={TOKEN} reset={reset} onDone={vi.fn()} />,
    );

    fireEvent.change(getByLabelText(NEW_PASSWORD_LABEL), {
      target: { value: TYPED },
    });
    const button = getByRole("button");
    await act(async () => {
      fireEvent.click(button);
      fireEvent.click(button);
      fireEvent.click(button);
    });

    expect(reset).toHaveBeenCalledTimes(1);
  });
});
