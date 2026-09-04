import { describe, it, expect } from "vitest";
import { presenceLine } from "./presence-text";

describe("the presence line", () => {
  it("says the rival is away, and says nothing about a pause", () => {
    expect(presenceLine("AWAY", false)).toBe("Your rival is away.");
  });

  it("says the duel continues once the rival did not come back", () => {
    expect(presenceLine("ABSENT", false)).toBe(
      "Your rival did not come back. The duel continues, and the server acts for them.",
    );
  });

  it("says the rival is back only to a client that saw them go", () => {
    expect(presenceLine("PRESENT", true)).toBe("Your rival is back.");
    expect(presenceLine("PRESENT", false)).toBe("");
  });

  it("says nothing before the server has stated a presence", () => {
    expect(presenceLine(null, false)).toBe("");
    expect(presenceLine(null, true)).toBe("");
  });

  it("lets the state the server sent outrank the return", () => {
    expect(presenceLine("AWAY", true)).toBe("Your rival is away.");
    expect(presenceLine("ABSENT", true)).toBe(
      "Your rival did not come back. The duel continues, and the server acts for them.",
    );
  });
});
