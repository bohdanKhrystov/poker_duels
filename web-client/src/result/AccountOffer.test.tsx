import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { vi } from "vitest";
import { AccountOffer } from "./AccountOffer";
import {
  OFFER_HEADING,
  OFFER_BODY,
  OFFER_ACCEPT,
  OFFER_DISMISS,
} from "./account-offer-text";

describe("the offer", () => {
  it("names the stake before it asks for anything", () => {
    const onDismiss = vi.fn();
    render(<AccountOffer onAccept={vi.fn()} onDismiss={onDismiss} />);

    expect(screen.getByText(OFFER_HEADING)).toBeDefined();
    expect(screen.getByText(OFFER_BODY)).toBeDefined();
  });

  it("leads to the account screen through a page load", () => {
    const onDismiss = vi.fn();
    render(<AccountOffer onAccept={vi.fn()} onDismiss={onDismiss} />);

    const link = screen.getByRole("link", { name: OFFER_ACCEPT });
    expect(link.getAttribute("href")).toBe("/#/account");
  });

  it("carries no form of its own", () => {
    const onDismiss = vi.fn();
    render(<AccountOffer onAccept={vi.fn()} onDismiss={onDismiss} />);

    expect(screen.queryByRole("textbox")).toBeNull();
    expect(screen.queryByRole("form")).toBeNull();
    const inputs = screen.queryAllByRole("textbox");
    expect(inputs).toHaveLength(0);
  });

  it("calls onDismiss when Not now is taken", () => {
    const onDismiss = vi.fn();
    render(<AccountOffer onAccept={vi.fn()} onDismiss={onDismiss} />);

    const button = screen.getByRole("button", { name: OFFER_DISMISS });
    fireEvent.click(button);

    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("calls onAccept when the offer is taken, and leaves the loading to the link", () => {
    const onAccept = vi.fn();
    const onDismiss = vi.fn();
    render(<AccountOffer onAccept={onAccept} onDismiss={onDismiss} />);

    const link = screen.getByRole("link", { name: OFFER_ACCEPT });
    const result = fireEvent.click(link);

    expect(onAccept).toHaveBeenCalledTimes(1);
    expect(onDismiss).toHaveBeenCalledTimes(0);
    expect(result).toBe(true);
    expect(link.getAttribute("href")).toBe("/#/account");
  });

  it("both of the offer's controls are dressed", () => {
    render(<AccountOffer onAccept={vi.fn()} onDismiss={vi.fn()} />);

    const link = screen.getByRole("link", { name: OFFER_ACCEPT });
    const button = screen.getByRole("button", { name: OFFER_DISMISS });

    expect(button.getAttribute("class")).toBe(link.getAttribute("class"));
  });
});
