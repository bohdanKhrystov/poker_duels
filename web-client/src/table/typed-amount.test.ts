import { describe, expect, it } from "vitest";
import { rejectionText } from "./rejection-text";
import { readTypedAmount } from "./typed-amount";

describe("what the bar makes of what the player typed", () => {
  it("reads a plain run of digits as that many chips", () => {
    expect(readTypedAmount("1200", 100, 5000)).toEqual({
      kind: "amount",
      to: 1200,
    });
    expect(readTypedAmount("3650", 100, 5000)).toEqual({
      kind: "amount",
      to: 3650,
    });
    expect(readTypedAmount("  1200  ", 100, 5000)).toEqual({
      kind: "amount",
      to: 1200,
    });
  });

  it("reads the grouping the table itself prints", () => {
    expect(readTypedAmount("1,200", 100, 20000)).toEqual({
      kind: "amount",
      to: 1200,
    });
    expect(readTypedAmount("13,400", 100, 20000)).toEqual({
      kind: "amount",
      to: 13400,
    });
  });

  it("sends both ends of the interval and one total inside it", () => {
    expect(readTypedAmount("1200", 1200, 13400)).toEqual({
      kind: "amount",
      to: 1200,
    });
    expect(readTypedAmount("13400", 1200, 13400)).toEqual({
      kind: "amount",
      to: 13400,
    });
    expect(readTypedAmount("5000", 1200, 13400)).toEqual({
      kind: "amount",
      to: 5000,
    });

    expect(readTypedAmount("800", 800, 4000)).toEqual({
      kind: "amount",
      to: 800,
    });
    expect(readTypedAmount("4000", 800, 4000)).toEqual({
      kind: "amount",
      to: 4000,
    });
    expect(readTypedAmount("2500", 800, 4000)).toEqual({
      kind: "amount",
      to: 2500,
    });
  });

  it("refuses a total under the floor, quoting this turn's own floor", () => {
    expect(readTypedAmount("500", 1200, 13400)).toEqual({
      kind: "refused",
      sentence: "500 is under the minimum of 1,200.",
    });
    expect(readTypedAmount("500", 800, 4000)).toEqual({
      kind: "refused",
      sentence: "500 is under the minimum of 800.",
    });
    expect(readTypedAmount("1199", 1200, 13400).kind).toBe("refused");
  });

  it("refuses a total over the stack, quoting this turn's own ceiling", () => {
    expect(readTypedAmount("20000", 1200, 13400)).toEqual({
      kind: "refused",
      sentence: "20,000 is over the maximum of 13,400.",
    });
    expect(readTypedAmount("20000", 800, 4000)).toEqual({
      kind: "refused",
      sentence: "20,000 is over the maximum of 4,000.",
    });
    expect(readTypedAmount("13401", 1200, 13400).kind).toBe("refused");
  });

  it("takes a plain zero as a number and quotes the minimum", () => {
    const result = readTypedAmount("0", 1200, 13400);
    expect(result).toEqual({
      kind: "refused",
      sentence: "0 is under the minimum of 1,200.",
    });
    expect(result).not.toEqual({
      kind: "refused",
      sentence: "That is not an amount.",
    });
  });

  it("refuses everything that is not a plain or table-grouped run of digits", () => {
    const entries = [
      "",
      "   ",
      "-500",
      "+500",
      "12abc",
      "1.5",
      "1 200",
      "1,20",
      "12,3456",
      "1e3",
      "0x10",
    ];

    for (const entry of entries) {
      expect(readTypedAmount(entry, 1200, 13400)).toEqual({
        kind: "refused",
        sentence: "That is not an amount.",
      });
    }
  });

  it("says exactly what the server's own rejection says", () => {
    expect(readTypedAmount("500", 1200, 13400)).toEqual({
      kind: "refused",
      sentence: rejectionText({
        type: "AmountTooSmall",
        attempted: 500,
        minimum: 1200,
      }),
    });
    expect(readTypedAmount("300", 800, 4000)).toEqual({
      kind: "refused",
      sentence: rejectionText({
        type: "AmountTooSmall",
        attempted: 300,
        minimum: 800,
      }),
    });

    expect(readTypedAmount("20000", 1200, 13400)).toEqual({
      kind: "refused",
      sentence: rejectionText({
        type: "AmountTooLarge",
        attempted: 20000,
        maximum: 13400,
      }),
    });
    expect(readTypedAmount("5000", 800, 4000)).toEqual({
      kind: "refused",
      sentence: rejectionText({
        type: "AmountTooLarge",
        attempted: 5000,
        maximum: 4000,
      }),
    });
  });
});
