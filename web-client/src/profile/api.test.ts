import { describe, it, expect } from "vitest";
import { readFromApi, type ApiResponse, type ApiFetch } from "./api";

/** Records the details of each fetch call made. */
interface FetchCall {
  readonly path: string;
  readonly headers: Record<string, string>;
}

/**
 * Creates a mock fetch that records every call and answers them in order.
 * No network, no globals — purely synchronous recording and predetermined answers.
 */
function answering(...answers: readonly ApiResponse[]): {
  readonly calls: FetchCall[];
  readonly fetch: ApiFetch;
} {
  const calls: FetchCall[] = [];
  let answerIndex = 0;

  return {
    calls,
    fetch: async (
      path: string,
      init: { readonly headers: Readonly<Record<string, string>> },
    ): Promise<ApiResponse> => {
      calls.push({
        path,
        headers: { ...init.headers },
      });
      if (answerIndex >= answers.length) {
        throw new Error(
          `No more answers available (called ${answerIndex + 1} times)`,
        );
      }
      return answers[answerIndex++];
    },
  };
}

/** Constructs a successful response with the given body. */
function ok(body: unknown): ApiResponse {
  return {
    status: 200,
    json: async () => body,
  };
}

/** Constructs a failed response with the given status code. */
function refusedWith(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error("json() should not be called on error responses");
    },
  };
}

describe("the /api/me read", () => {
  it("sends the device id it was given", async () => {
    // First call with "d-1"
    const mock1 = answering(ok({ playerId: "p-1", coinBalance: 5 }));
    await readFromApi({
      fetch: mock1.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(mock1.calls).toHaveLength(1);
    expect(mock1.calls[0]?.headers["X-Device-Id"]).toBe("d-1");

    // Second call with "d-2" to ensure it's not hardcoded
    const mock2 = answering(ok({ playerId: "p-2", coinBalance: 10 }));
    await readFromApi({
      fetch: mock2.fetch,
      deviceId: "d-2",
      path: "/api/me",
    });
    expect(mock2.calls).toHaveLength(1);
    expect(mock2.calls[0]?.headers["X-Device-Id"]).toBe("d-2");
  });

  it("asks for the path it was given", async () => {
    // First call with "/api/me"
    const mock1 = answering(ok({ playerId: "p-1", coinBalance: 0 }));
    await readFromApi({
      fetch: mock1.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(mock1.calls).toHaveLength(1);
    expect(mock1.calls[0]?.path).toBe("/api/me");

    // Second call with "/api/me/duels" to ensure path is not hardcoded
    const mock2 = answering(ok({ duels: [] }));
    await readFromApi({
      fetch: mock2.fetch,
      deviceId: "d-1",
      path: "/api/me/duels",
    });
    expect(mock2.calls).toHaveLength(1);
    expect(mock2.calls[0]?.path).toBe("/api/me/duels");
  });

  it("asks for nothing at all with no device id in hand", async () => {
    const mock = answering(ok({}));
    const result = await readFromApi({
      fetch: mock.fetch,
      deviceId: null,
      path: "/api/me",
    });
    expect(result).toEqual({ kind: "no-profile" });
    // Verify that no fetch call was made
    expect(mock.calls).toHaveLength(0);
  });

  it("answers with the body of a 200", async () => {
    // First body
    const body1 = { playerId: "alice", coinBalance: 42 };
    const mock1 = answering(ok(body1));
    const result1 = await readFromApi({
      fetch: mock1.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(result1).toEqual({ kind: "body", body: body1 });

    // Second body to ensure we're not hardcoding
    const body2 = { playerId: "bob", coinBalance: -5, duels: [] };
    const mock2 = answering(ok(body2));
    const result2 = await readFromApi({
      fetch: mock2.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(result2).toEqual({ kind: "body", body: body2 });
  });

  it("answers no-profile on a 401", async () => {
    const mock = answering(refusedWith(401));
    const result = await readFromApi({
      fetch: mock.fetch,
      deviceId: "unknown-device",
      path: "/api/me",
    });
    expect(result).toEqual({ kind: "no-profile" });
  });

  it("answers unavailable on a 500, on a refused request and on a body that is not JSON", async () => {
    // Case 1: 500 status
    const mock1 = answering(refusedWith(500));
    const result1 = await readFromApi({
      fetch: mock1.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(result1).toEqual({ kind: "unavailable" });

    // Case 2: fetch promise rejects
    const mock2 = {
      calls: [],
      fetch: async () => {
        throw new Error("Network error");
      },
    } as { calls: FetchCall[]; fetch: ApiFetch };
    const result2 = await readFromApi({
      fetch: mock2.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(result2).toEqual({ kind: "unavailable" });

    // Case 3: json() rejects
    const responseWithBadJson: ApiResponse = {
      status: 200,
      json: async () => {
        throw new Error("Invalid JSON");
      },
    };
    const mock3 = answering(responseWithBadJson);
    const result3 = await readFromApi({
      fetch: mock3.fetch,
      deviceId: "d-1",
      path: "/api/me",
    });
    expect(result3).toEqual({ kind: "unavailable" });
  });
});
