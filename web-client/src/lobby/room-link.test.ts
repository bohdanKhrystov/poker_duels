import { describe, it, expect } from "vitest";
import { normalizeRoomCode, roomCodeFromSearch, roomLink } from "./room-link";

describe("the room link", () => {
  it("trims and upper-cases a pasted code", () => {
    expect(normalizeRoomCode("  abcdefgh  ")).toBe("ABCDEFGH");
  });

  it("reads the code the link carried", () => {
    expect(roomCodeFromSearch("?room=abcdefgh")).toBe("ABCDEFGH");
  });

  it("has no code when the link carried none", () => {
    expect(roomCodeFromSearch("")).toBe(null);
    expect(roomCodeFromSearch("?rematch=1")).toBe(null);
  });

  it("has no code when the room parameter is blank", () => {
    expect(roomCodeFromSearch("?room=")).toBe(null);
    expect(roomCodeFromSearch("?room=%20%20")).toBe(null);
  });

  it("builds the invite from this page's own origin", () => {
    expect(roomLink("https://duels.example", "ABCDEFGH")).toBe(
      "https://duels.example/?room=ABCDEFGH",
    );
  });
});
