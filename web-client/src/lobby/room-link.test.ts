import { describe, it, expect } from "vitest";
import {
  normalizeRoomCode,
  roomCodeFromSearch,
  roomLink,
  roomCodeFromField,
} from "./room-link";

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

describe("the code the field yields", () => {
  it("reads the code out of a whole invite link", () => {
    expect(roomCodeFromField("http://192.168.0.142:5173/?room=918RHERX")).toBe(
      "918RHERX",
    );
  });

  it("still yields the bare code the field has always taken", () => {
    expect(roomCodeFromField("  abcdefgh  ")).toBe("ABCDEFGH");
    expect(roomCodeFromField("918rherx")).toBe("918RHERX");
  });

  it("reads the code among other parameters, whatever surrounds the link", () => {
    expect(
      roomCodeFromField(
        "  https://duels.example/?utm=mail&room=abcdefgh#seat ",
      ),
    ).toBe("ABCDEFGH");
  });

  it("hands back text carrying no room code exactly as the field always did", () => {
    expect(roomCodeFromField("https://duels.example/lobby")).toBe(
      "HTTPS://DUELS.EXAMPLE/LOBBY",
    );
    expect(roomCodeFromField("https://duels.example/?room=")).toBe(
      "HTTPS://DUELS.EXAMPLE/?ROOM=",
    );
  });

  it("yields nothing for an empty or whitespace-only field", () => {
    expect(roomCodeFromField("")).toBe("");
    expect(roomCodeFromField("   ")).toBe("");
  });
});
