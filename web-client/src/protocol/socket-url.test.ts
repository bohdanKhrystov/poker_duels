import { describe, expect, it } from "vitest";
import { socketUrl } from "./socket-url";

describe("the socket URL", () => {
  it("points at /ws on the page's own host", () => {
    expect(socketUrl({ protocol: "http:", host: "localhost:5173" })).toBe(
      "ws://localhost:5173/ws",
    );
  });

  it("upgrades to a secure socket on a secure page", () => {
    expect(socketUrl({ protocol: "https:", host: "duels.example" })).toBe(
      "wss://duels.example/ws",
    );
  });
});
