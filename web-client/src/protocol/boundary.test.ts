import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import {
  guardedFiles,
  rawFrameMentions,
  redeclaredWireTypes,
  reservedWireNames,
} from "./boundary";

describe("the protocol module boundary", () => {
  it("flags a redeclared wire type", () => {
    expect(
      redeclaredWireTypes("export interface Snapshot { view: number }", [
        "Snapshot",
      ]),
    ).toEqual(["Snapshot"]);
  });

  it("passes an import of a wire type", () => {
    expect(
      redeclaredWireTypes('import type { Snapshot } from "./protocol.gen";', [
        "Snapshot",
      ]),
    ).toEqual([]);
  });

  it("flags a raw frame API", () => {
    expect(
      rawFrameMentions(
        "const s = new WebSocket(url); function f(e: MessageEvent) {}",
      ),
    ).toHaveLength(2);
  });

  it("reads the wire names out of the generated file", () => {
    const names = reservedWireNames();
    expect(names).toContain("ServerMessage");
    expect(names).toContain("Welcome");
    expect(names).toContain("ProtocolVersion");
    expect(names.length).toBeGreaterThan(40);
  });

  it("guards the app source and skips the protocol module", () => {
    const files = guardedFiles();
    expect(files.some((file) => file.endsWith("/src/App.tsx"))).toBe(true);
    expect(
      files.some((file) => file.endsWith("/src/styles/color-literals.ts")),
    ).toBe(true);
    expect(
      files.some((file) => file.endsWith("/src/protocol/protocol.gen.ts")),
    ).toBe(false);
    expect(
      files.some((file) => file.endsWith("/src/protocol/boundary.ts")),
    ).toBe(false);
  });

  it("finds no wire type declared outside the protocol module", () => {
    const reserved = reservedWireNames();
    const offenders = guardedFiles()
      .map((file) => ({
        file,
        names: redeclaredWireTypes(readFileSync(file, "utf-8"), reserved),
      }))
      .filter((offender) => offender.names.length > 0);
    expect(offenders).toEqual([]);
  });

  it("finds no raw frame handled outside the protocol module", () => {
    const offenders = guardedFiles()
      .map((file) => ({
        file,
        names: rawFrameMentions(readFileSync(file, "utf-8")),
      }))
      .filter((offender) => offender.names.length > 0);
    expect(offenders).toEqual([]);
  });
});
