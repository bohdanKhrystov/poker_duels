// @vitest-environment node
import { describe, it, expect } from "vitest";
import config from "../vite.config";

describe("dev server proxy", () => {
  it("sends /api to the Ktor server", () => {
    const apiProxy = config.server?.proxy?.["/api"];
    expect(apiProxy).toBe("http://localhost:8080");
  });

  it("sends /ws to the Ktor server as a websocket", () => {
    const wsProxy = config.server?.proxy?.["/ws"];
    expect(wsProxy).toEqual({ target: "ws://localhost:8080", ws: true });
  });
});
