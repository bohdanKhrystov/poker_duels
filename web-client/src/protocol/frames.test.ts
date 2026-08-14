import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { describe, expect, it } from "vitest";
import { decodeServerMessage, SERVER_MESSAGE_TYPES } from "./frames";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe("the frame codec", () => {
  it("decodes a frame the union names", () => {
    const message = decodeServerMessage(
      '{"type":"Welcome","deviceId":"d-1","protocolVersion":2}',
    );

    expect(message).not.toBeNull();
    expect(message?.type).toBe("Welcome");
  });

  it("drops a frame that is not JSON", () => {
    expect(decodeServerMessage("not json at all")).toBeNull();
  });

  it("drops a frame that is not an object", () => {
    expect(decodeServerMessage('"Welcome"')).toBeNull();
    expect(decodeServerMessage("[]")).toBeNull();
    expect(decodeServerMessage("42")).toBeNull();
  });

  it("drops a frame with no type", () => {
    expect(decodeServerMessage('{"deviceId":"d-1"}')).toBeNull();
  });

  it("drops a frame whose type the union does not name", () => {
    expect(decodeServerMessage('{"type":"Nonsense"}')).toBeNull();
  });

  it("drops a frame that is not text", () => {
    expect(decodeServerMessage(new ArrayBuffer(4))).toBeNull();
  });

  it("knows exactly the variants the generated union declares", () => {
    const protocolGenPath = join(__dirname, "protocol.gen.ts");
    const content = readFileSync(protocolGenPath, "utf-8");

    const match = content.match(/^export type ServerMessage = (.+);$/m);
    if (!match) {
      throw new Error(
        "ServerMessage alias not found or has unexpected format in protocol.gen.ts",
      );
    }

    // Sound because `ADR-0020` derives both the union's member type names and
    // each member's `@SerialName` discriminator from the same Kotlin sealed
    // type, so the type names on this line and the wire discriminators in
    // `SERVER_MESSAGE_TYPES` are the same strings.
    const declaredTypes = match[1]
      .split("|")
      .map((name) => name.trim())
      .sort();

    expect(SERVER_MESSAGE_TYPES).toEqual(declaredTypes);
  });
});
