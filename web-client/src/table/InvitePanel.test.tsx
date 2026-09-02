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
    const writeText = vi.fn(() => Promise.resolve());
    withClipboard(writeText);

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

  it("keeps the box as the whole invite when the clipboard refuses, and when there is none", async () => {
    // First test with a refusing clipboard
    const rejectingWriteText = vi.fn(() => Promise.reject(new Error("denied")));
    withClipboard(rejectingWriteText);

    const { unmount } = render(<InvitePanel code="7Q4M9K2T" />);

    fireEvent.click(screen.getByRole("button", { name: "Copy the link" }));
    await screen.findByText("Copy it from the box above.");

    const inviteLink = screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLink.value).toBe("http://localhost:3000/?room=7Q4M9K2T");

    unmount();

    // Now test with no clipboard
    Reflect.deleteProperty(navigator, "clipboard");
    render(<InvitePanel code="7Q4M9K2T" />);

    expect(screen.queryByRole("button", { name: "Copy the link" })).toBeNull();
    const inviteLinkNoClipboard =
      screen.getByLabelText<HTMLInputElement>("Invite link");
    expect(inviteLinkNoClipboard.value).toBe(
      "http://localhost:3000/?room=7Q4M9K2T",
    );
  });
});
