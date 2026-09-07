import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { InvitePanel } from "./InvitePanel";

function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
});

describe("the invite panel", () => {
  it("shows the bare code, the labelled box and the copy control", () => {
    render(<InvitePanel code="7Q4M9K2T" />);

    expect(screen.getByText("7Q4M9K2T")).toBeDefined();
    expect(screen.getByLabelText("Invite link")).toBeDefined();
    expect(screen.getByRole("button", { name: "Copy the link" })).toBeDefined();
  });

  it("builds the link from this window's origin and the code", () => {
    render(<InvitePanel code="7Q4M9K2T" />);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=7Q4M9K2T");
  });

  it("leaves the box read-only and focused for a copy by hand", () => {
    render(<InvitePanel code="7Q4M9K2T" />);

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.readOnly).toBe(true);
    expect(document.activeElement).toBe(inviteLink);
  });

  it("says so when the copy succeeds", async () => {
    const writeText = vi.fn(() => Promise.resolve());
    withClipboard(writeText);

    render(<InvitePanel code="7Q4M9K2T" />);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));

    expect(writeText).toHaveBeenCalledWith(
      "http://localhost:3000/?room=7Q4M9K2T",
    );
    await screen.findByText("Link copied.");
  });

  it("hands over the selection when the write is refused", async () => {
    const rejectingWriteText = vi.fn(() => Promise.reject(new Error("denied")));
    withClipboard(rejectingWriteText);

    render(<InvitePanel code="7Q4M9K2T" />);

    const box = screen.getByLabelText<HTMLInputElement>("Invite link");
    const button = screen.getByRole("button", { name: "Copy the link" });

    // Break the box's own precondition before the press.
    box.setSelectionRange(0, 0);
    // Focus the button first to verify the hand-over actually moved focus.
    button.focus();
    expect(document.activeElement).toBe(button);

    fireEvent.click(button);

    await screen.findByText("Copy it from the box above.");
    expect(screen.queryByText("Link copied.")).toBeNull();
    expect(document.activeElement).toBe(box);
    expect(box.selectionStart).toBe(0);
    expect(box.selectionEnd).toBe(box.value.length);
  });

  it("offers the control and hands over the selection with no Clipboard API", async () => {
    expect(navigator.clipboard).toBeUndefined();

    render(<InvitePanel code="7Q4M9K2T" />);

    const box = screen.getByLabelText<HTMLInputElement>("Invite link");
    const button = screen.getByRole("button", { name: "Copy the link" });

    expect(button).toBeDefined();

    // Break the box's own precondition before the press.
    box.setSelectionRange(0, 0);
    // Focus the button first to verify the hand-over actually moved focus.
    button.focus();
    expect(document.activeElement).toBe(button);

    fireEvent.click(button);

    expect(screen.getByText("Copy it from the box above.")).toBeDefined();
    expect(screen.queryByText("Link copied.")).toBeNull();
    expect(document.activeElement).toBe(box);
    expect(box.selectionStart).toBe(0);
    expect(box.selectionEnd).toBe(box.value.length);
  });
});
