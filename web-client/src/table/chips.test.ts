import { describe, it, expect } from "vitest";
import { formatChips } from "./chips";

describe("a chip amount", () => {
  it("leaves three digits ungrouped", () => {
    expect(formatChips(150)).toBe("150");
  });

  it("groups a four-figure stack", () => {
    expect(formatChips(4150)).toBe("4,150");
  });

  it("groups a five-figure stack", () => {
    expect(formatChips(13400)).toBe("13,400");
  });

  it("writes zero as zero", () => {
    expect(formatChips(0)).toBe("0");
  });

  it("groups the same way wherever it runs", () => {
    expect(formatChips(1234567)).toBe("1,234,567");
  });
});
