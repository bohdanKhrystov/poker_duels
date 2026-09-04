import { describe, it, expect } from "vitest";
import { clockFigure, bankFigure } from "./turn-clock";

describe("turn-clock", () => {
  it("writes the clock as bare seconds under a minute", () => {
    expect(clockFigure(24)).toBe("24");
    expect(clockFigure(6)).toBe("6");
  });

  it("writes the clock as m:ss from a minute up", () => {
    expect(clockFigure(167)).toBe("2:47");
    expect(clockFigure(72)).toBe("1:12");
  });

  it("switches shape at exactly a minute", () => {
    expect(clockFigure(59)).toBe("59");
    expect(clockFigure(60)).toBe("1:00");
  });

  it("writes a spent clock as a bare zero", () => {
    expect(clockFigure(0)).toBe("0");
  });

  it("writes the bank as m:ss, at every size", () => {
    expect(bankFigure(180)).toBe("3:00");
    expect(bankFigure(72)).toBe("1:12");
  });

  it("writes an empty bank as 0:00 and not as 0", () => {
    expect(bankFigure(0)).toBe("0:00");
  });

  it("pads the bank's and the clock's seconds to two figures", () => {
    expect(bankFigure(65)).toBe("1:05");
    expect(bankFigure(61)).toBe("1:01");
    expect(clockFigure(65)).toBe("1:05");
  });
});
