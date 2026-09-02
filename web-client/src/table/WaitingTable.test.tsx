import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { WaitingTable } from "./WaitingTable";

function withClipboard(writeText: () => Promise<void>): void {
  Object.defineProperty(navigator, "clipboard", {
    value: { writeText },
    configurable: true,
  });
}

afterEach(() => {
  Reflect.deleteProperty(navigator, "clipboard");
});

describe("WaitingTable", () => {
  it("names the empty seat and the host's seat exactly once each", () => {
    render(<WaitingTable code="7Q4M9K2T" onLeave={vi.fn()} />);

    const rivalSeats = screen.getAllByText("Waiting for your rival");
    const hostSeats = screen.getAllByText("You");

    expect(rivalSeats).toHaveLength(1);
    expect(hostSeats).toHaveLength(1);
  });

  it("draws the invite whole", () => {
    const writeText = vi.fn(() => Promise.resolve());
    withClipboard(writeText);

    render(<WaitingTable code="7Q4M9K2T" onLeave={vi.fn()} />);

    expect(screen.getByText("7Q4M9K2T")).toBeDefined();
    expect(screen.getByLabelText("Invite link")).toBeDefined();
    expect(screen.getByRole("button", { name: "Copy the link" })).toBeDefined();
  });

  it("keeps the way back a link to the lobby that forgets the room", () => {
    const onLeave = vi.fn();
    render(<WaitingTable code="7Q4M9K2T" onLeave={onLeave} />);

    const backLink = screen.getByRole("link", { name: "Back to the lobby" });
    expect(backLink.getAttribute("href")).toBe("/");
    expect(backLink.className).toContain("border-hairline");

    const clickReturn = fireEvent.click(backLink);
    expect(onLeave).toHaveBeenCalledOnce();
    expect(clickReturn).toBe(true);
  });

  it("keeps saying that the room stays open", () => {
    render(<WaitingTable code="7Q4M9K2T" onLeave={vi.fn()} />);

    screen.getByText(
      "The room stays open. That link still works for your rival, and it brings you back.",
    );
  });

  it("renders exactly one section, and it is its own root", () => {
    const { container } = render(
      <WaitingTable code="7Q4M9K2T" onLeave={vi.fn()} />,
    );

    const sections = container.querySelectorAll("section");
    expect(sections).toHaveLength(1);
    expect(container.firstElementChild?.tagName).toBe("SECTION");
  });

  it("carries the card's column and never a second one", () => {
    const { container } = render(
      <WaitingTable code="7Q4M9K2T" onLeave={vi.fn()} />,
    );

    const root = container.firstElementChild as HTMLElement;
    const classes = root.className.split(" ");

    expect(classes).toContain("mx-auto");
    expect(classes).toContain("max-w-[560px]");
    expect(classes).toContain("min-h-[100dvh]");
    expect(classes).toContain("flex-col");

    // Verify only one element has max-w-[560px]
    const elementsWithMaxW = container.querySelectorAll(".max-w-\\[560px\\]");
    expect(elementsWithMaxW).toHaveLength(1);
  });
});
