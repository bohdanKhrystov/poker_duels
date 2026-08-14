import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { describe, expect, it } from "vitest";
import { PROTOCOL_VERSION } from "./version";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe("the protocol version", () => {
  it("is the version the generated alias declares", () => {
    const protocolGenPath = join(__dirname, "protocol.gen.ts");
    const content = readFileSync(protocolGenPath, "utf-8");

    const match = content.match(/^export type ProtocolVersion = (\d+);$/m);
    if (!match) {
      throw new Error(
        "ProtocolVersion alias not found or has unexpected format in protocol.gen.ts",
      );
    }

    const declaredVersion = parseInt(match[1], 10);
    expect(PROTOCOL_VERSION).toBe(declaredVersion);
  });
});
