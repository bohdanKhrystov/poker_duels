import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { RecoveryEmailForm } from "./RecoveryEmailForm";
import type { AttachRecoveryOutcome } from "./attach-recovery-email";
import {
  ADDRESS_LABEL,
  CURRENT_PASSWORD_LABEL,
  ATTACH_LABEL,
  ATTACH_WHY,
  ATTACH_ACKNOWLEDGED,
  ATTACH_ADDRESS_REFUSED,
  ATTACH_PASSWORD_WRONG,
  ATTACH_FAILED,
  RECOVERY_ON,
} from "./recovery-text";

const ADDRESS = "zqx-address-zqx";
const CURRENT = "zqx-current-zqx";

describe("attaching a recovery address", () => {
  it("sends what was typed, once, and says why the password is asked for", async () => {
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });

    render(<RecoveryEmailForm attach={attach} />);

    // Asserted at mount, before any submit: the reason for asking is not
    // conditional on anything happening first.
    expect(screen.getByText(ATTACH_WHY)).toBeDefined();

    fireEvent.change(screen.getByLabelText(ADDRESS_LABEL), {
      target: { value: ADDRESS },
    });
    fireEvent.change(screen.getByLabelText(CURRENT_PASSWORD_LABEL), {
      target: { value: CURRENT },
    });
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));

    // Exactly the two typed strings, in call order (address, then current
    // password) — a form that swapped them or dropped one would fail this
    // line. The two fixture literals differ for exactly this reason.
    expect(attach).toHaveBeenCalledTimes(1);
    expect(attach).toHaveBeenCalledWith(ADDRESS, CURRENT);

    await screen.findByText(ATTACH_ACKNOWLEDGED);
  });

  it("says the same thing for every reason the server accepted it", async () => {
    // Three distinct lengths, so a mutation that branches on address length
    // cannot hide behind a coincidence in the fixture.
    const addresses = [
      "a@zqx",
      "ab-cd@zqx-two",
      "ab-cd-ef-gh-ij@zqx-three-longer",
    ];
    const outcomeTexts: string[] = [];

    for (const oneAddress of addresses) {
      const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
      attach.mockResolvedValue({ kind: "accepted" });

      render(<RecoveryEmailForm attach={attach} />);

      fireEvent.change(screen.getByLabelText(ADDRESS_LABEL), {
        target: { value: oneAddress },
      });
      fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));

      const status = await screen.findByRole("status");
      outcomeTexts.push(status.textContent ?? "");

      cleanup();
    }

    // Guarded against vacuity: the first is checked against the real
    // constant, not only against the other two.
    expect(outcomeTexts[0]).toBe(ATTACH_ACKNOWLEDGED);
    // The story's "renders the same acknowledgement whatever the server's
    // reason for 202", asserted as an equality rather than as three separate
    // presence checks.
    expect(outcomeTexts[1]).toBe(outcomeTexts[0]);
    expect(outcomeTexts[2]).toBe(outcomeTexts[0]);
  });

  it("renders one sentence per refusal, and each is its own", async () => {
    const addressRefused = vi.fn<
      [string, string],
      Promise<AttachRecoveryOutcome>
    >();
    addressRefused.mockResolvedValue({ kind: "address-refused" });
    render(<RecoveryEmailForm attach={addressRefused} />);
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_ADDRESS_REFUSED);
    expect(screen.queryByText(ATTACH_PASSWORD_WRONG)).toBeNull();
    expect(screen.queryByText(ATTACH_FAILED)).toBeNull();
    cleanup();

    const passwordRefused = vi.fn<
      [string, string],
      Promise<AttachRecoveryOutcome>
    >();
    passwordRefused.mockResolvedValue({ kind: "password-refused" });
    render(<RecoveryEmailForm attach={passwordRefused} />);
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_PASSWORD_WRONG);
    expect(screen.queryByText(ATTACH_ADDRESS_REFUSED)).toBeNull();
    expect(screen.queryByText(ATTACH_FAILED)).toBeNull();
    cleanup();

    const failed = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    failed.mockResolvedValue({ kind: "failed" });
    render(<RecoveryEmailForm attach={failed} />);
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_FAILED);
    expect(screen.queryByText(ATTACH_ADDRESS_REFUSED)).toBeNull();
    expect(screen.queryByText(ATTACH_PASSWORD_WRONG)).toBeNull();
  });

  it("treats a browser the server does not know as a failure it does not blame the player for", async () => {
    const noProfile = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    noProfile.mockResolvedValue({ kind: "no-profile" });
    render(<RecoveryEmailForm attach={noProfile} />);
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    const noProfileStatus = await screen.findByRole("status");
    const noProfileText = noProfileStatus.textContent;
    cleanup();

    const failed = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    failed.mockResolvedValue({ kind: "failed" });
    render(<RecoveryEmailForm attach={failed} />);
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    const failedStatus = await screen.findByRole("status");

    // Guarded against vacuity: each is checked against the real constant, not
    // only against each other.
    expect(noProfileText).toBe(ATTACH_FAILED);
    expect(failedStatus.textContent).toBe(ATTACH_FAILED);
    expect(noProfileText).toBe(failedStatus.textContent);
  });

  it("keeps what was typed when the server refuses, and clears the password on success", async () => {
    const passwordRefused = vi.fn<
      [string, string],
      Promise<AttachRecoveryOutcome>
    >();
    passwordRefused.mockResolvedValue({ kind: "password-refused" });
    render(<RecoveryEmailForm attach={passwordRefused} />);

    const refusedAddressInput = screen.getByLabelText(
      ADDRESS_LABEL,
    ) as HTMLInputElement;
    const refusedPasswordInput = screen.getByLabelText(
      CURRENT_PASSWORD_LABEL,
    ) as HTMLInputElement;
    fireEvent.change(refusedAddressInput, { target: { value: ADDRESS } });
    fireEvent.change(refusedPasswordInput, { target: { value: CURRENT } });
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_PASSWORD_WRONG);

    expect(refusedAddressInput.value).toBe(ADDRESS);
    expect(refusedPasswordInput.value).toBe(CURRENT);
    cleanup();

    const accepted = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    accepted.mockResolvedValue({ kind: "accepted" });
    render(<RecoveryEmailForm attach={accepted} />);

    const acceptedAddressInput = screen.getByLabelText(
      ADDRESS_LABEL,
    ) as HTMLInputElement;
    const acceptedPasswordInput = screen.getByLabelText(
      CURRENT_PASSWORD_LABEL,
    ) as HTMLInputElement;
    fireEvent.change(acceptedAddressInput, { target: { value: ADDRESS } });
    fireEvent.change(acceptedPasswordInput, { target: { value: CURRENT } });
    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_ACKNOWLEDGED);

    expect(acceptedAddressInput.value).toBe(ADDRESS);
    expect(acceptedPasswordInput.value).toBe("");
  });

  it("never says recovery is on, because a link has only been sent", async () => {
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>();
    attach.mockResolvedValue({ kind: "accepted" });
    const { container } = render(<RecoveryEmailForm attach={attach} />);

    fireEvent.click(screen.getByRole("button", { name: ATTACH_LABEL }));
    await screen.findByText(ATTACH_ACKNOWLEDGED);

    const text = container.textContent ?? "";
    expect(text).not.toContain(RECOVERY_ON);
    expect(text).toContain(ATTACH_ACKNOWLEDGED);
  });

  it("sends nothing twice, however fast the control is pressed", async () => {
    let resolveAttach: (outcome: AttachRecoveryOutcome) => void = () => {};
    const attach = vi.fn<[string, string], Promise<AttachRecoveryOutcome>>(
      () =>
        new Promise<AttachRecoveryOutcome>((resolve) => {
          resolveAttach = resolve;
        }),
    );

    render(<RecoveryEmailForm attach={attach} />);

    const submitButton = screen.getByRole("button", {
      name: ATTACH_LABEL,
    }) as HTMLButtonElement;
    // All three dispatches inside one outer act(): fireEvent's own act()
    // wrapping nests inside it and defers its flush to the outer call, so
    // the second and third clicks' handlers run against the same pre-render
    // closure as the first — the actual race a synchronous ref (and not
    // state) is guarding against.
    act(() => {
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
      fireEvent.click(submitButton);
    });

    // The count is the assertion; a disabled attribute alone is a claim
    // about the DOM, not about the request.
    expect(attach).toHaveBeenCalledTimes(1);
    expect(submitButton.disabled).toBe(true);

    resolveAttach({ kind: "accepted" });
    await screen.findByText(ATTACH_ACKNOWLEDGED);
  });
});
