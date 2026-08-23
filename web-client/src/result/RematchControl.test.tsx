import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { vi } from "vitest";
import { RematchControl } from "./RematchControl";

describe("the rematch control", () => {
  it("offers one press, labelled Rematch", () => {
    const onOffer = vi.fn();
    render(<RematchControl mySeat={1} onOffer={onOffer} />);

    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(1);

    const button = buttons[0];
    expect(button.textContent).toBe("Rematch");
    expect(button.getAttribute("type")).toBe("button");
  });

  it("calls onOffer once for one press", () => {
    const onOffer = vi.fn();
    render(<RematchControl mySeat={1} onOffer={onOffer} />);

    const button = screen.getByRole("button", { name: "Rematch" });
    fireEvent.click(button);

    expect(onOffer).toHaveBeenCalledTimes(1);
  });

  it("stays live for a second press", () => {
    const onOffer = vi.fn();
    render(<RematchControl mySeat={1} onOffer={onOffer} />);

    const button = screen.getByRole("button", { name: "Rematch" });
    fireEvent.click(button);
    fireEvent.click(button);

    expect(onOffer).toHaveBeenCalledTimes(2);
    expect(button.hasAttribute("disabled")).toBe(false);
  });

  it("offers nothing to a client that holds no seat", () => {
    const onOffer = vi.fn();
    const { container } = render(
      <RematchControl mySeat={null} onOffer={onOffer} />,
    );

    expect(container.firstChild).toBeNull();
    expect(screen.queryByRole("button")).toBeNull();
  });
});
