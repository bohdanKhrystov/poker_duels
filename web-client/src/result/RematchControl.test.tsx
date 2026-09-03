import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { vi } from "vitest";
import { RematchControl } from "./RematchControl";

describe("the rematch control", () => {
  it("offers one press, labelled Rematch", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[]}
        refusal={null}
      />,
    );

    const buttons = screen.getAllByRole("button");
    expect(buttons).toHaveLength(1);

    const button = buttons[0];
    expect(button.textContent).toBe("Rematch");
    expect(button.getAttribute("type")).toBe("button");
  });

  it("calls onOffer once for one press", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[]}
        refusal={null}
      />,
    );

    const button = screen.getByRole("button", { name: "Rematch" });
    fireEvent.click(button);

    expect(onOffer).toHaveBeenCalledTimes(1);
  });

  it("stays live for a second press", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[]}
        refusal={null}
      />,
    );

    const button = screen.getByRole("button", { name: "Rematch" });
    fireEvent.click(button);
    fireEvent.click(button);

    expect(onOffer).toHaveBeenCalledTimes(2);
    expect(button.hasAttribute("disabled")).toBe(false);
  });

  it("offers nothing to a client that holds no seat", () => {
    const onOffer = vi.fn();
    const { container } = render(
      <RematchControl
        mySeat={null}
        onOffer={onOffer}
        offers={[]}
        refusal={null}
      />,
    );

    expect(container.firstChild).toBeNull();
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("shows your own offer standing, and takes the button away", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[1]}
        refusal={null}
      />,
    );

    expect(
      screen.getByText("Rematch offered — waiting for your rival"),
    ).toBeDefined();
    expect(screen.queryByRole("button", { name: "Rematch" })).toBeNull();
  });

  it("shows the same offer from the other side above a live button", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={0}
        onOffer={onOffer}
        offers={[1]}
        refusal={null}
      />,
    );

    const rivalLine = screen.getByText("Your rival offers a rematch");
    const button = screen.getByRole("button", { name: "Rematch" });

    expect(rivalLine).toBeDefined();
    expect(button).toBeDefined();

    // The rival line should precede the button in document order
    expect(rivalLine.compareDocumentPosition(button)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it("draws the card's dealing frame once the accepting seat takes the button", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={0}
        onOffer={onOffer}
        offers={[1]}
        refusal={null}
      />,
    );

    const button = screen.getByRole("button", { name: "Rematch" });
    fireEvent.click(button);

    expect(onOffer).toHaveBeenCalledTimes(1);
    expect(
      screen.getByText(/The button changes sides.*dealing hand 1…/),
    ).toBeDefined();
    expect(screen.queryByRole("button")).toBeNull();
    expect(screen.queryByText("Your rival offers a rematch")).toBeNull();
  });

  it("draws the same dealing frame when the store already carries both seats", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={0}
        onOffer={onOffer}
        offers={[0, 1]}
        refusal={null}
      />,
    );

    expect(
      screen.getByText(/The button changes sides.*dealing hand 1…/),
    ).toBeDefined();
    expect(screen.queryByRole("button")).toBeNull();
  });

  it("takes the button away for seat zero too", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={0}
        onOffer={onOffer}
        offers={[0]}
        refusal={null}
      />,
    );

    expect(
      screen.getByText("Rematch offered — waiting for your rival"),
    ).toBeDefined();
    expect(screen.queryByRole("button", { name: "Rematch" })).toBeNull();
  });

  it("keeps the button while nobody has offered", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[]}
        refusal={null}
      />,
    );

    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
    expect(
      screen.queryByText("Rematch offered — waiting for your rival"),
    ).toBeNull();
    expect(screen.queryByText("Your rival offers a rematch")).toBeNull();
  });

  it("retires the control when the room is gone", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[0]}
        refusal="UNKNOWN_ROOM"
      />,
    );

    expect(screen.getByText("That duel room is gone.")).toBeDefined();
    expect(screen.queryByRole("button", { name: "Rematch" })).toBeNull();
    expect(screen.queryByText("Your rival offers a rematch")).toBeNull();
  });

  it("leaves the control alone for any other refusal", () => {
    const onOffer = vi.fn();
    render(
      <RematchControl
        mySeat={1}
        onOffer={onOffer}
        offers={[0]}
        refusal="NOT_IN_DUEL"
      />,
    );

    expect(screen.getByRole("button", { name: "Rematch" })).toBeDefined();
    expect(screen.getByText("Your rival offers a rematch")).toBeDefined();
    expect(screen.queryByText("That duel room is gone.")).toBeNull();
  });
});
