/* @vitest-environment node */

import { describe, it, expect } from "vitest";
import { readFileSync } from "fs";

describe("the vendored token sheet", () => {
  it("is byte-identical to design/tokens/tokens.css", () => {
    // Locate both files from import.meta.url
    const vendoredPath = new URL("./tokens.css", import.meta.url);
    const canonicalPath = new URL(
      "../../../design/tokens/tokens.css",
      import.meta.url,
    );

    // Read both files as buffers and strings
    const vendoredBuffer = readFileSync(vendoredPath);
    const canonicalBuffer = readFileSync(canonicalPath);
    const vendoredString = vendoredBuffer.toString("utf-8");
    const canonicalString = canonicalBuffer.toString("utf-8");

    // Assert string equality (readable diff on failure)
    expect(vendoredString).toBe(canonicalString);

    // Assert byte equality (catches line endings and BOM)
    expect(Buffer.compare(vendoredBuffer, canonicalBuffer)).toBe(0);
  });
});
