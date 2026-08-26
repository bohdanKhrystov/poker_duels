import { describe, it, expect, beforeEach } from "vitest";
import type { ApiFetch, ApiResponse } from "../profile/api";
import { authorizedFetch } from "./authorized-fetch";

describe("a fetch under this browser's session", () => {
  let storage: Storage;
  let recordedCalls: Array<{
    path: string;
    init: {
      headers: Readonly<Record<string, string>>;
      method?: string;
      body?: string;
    };
  }>;
  let mockFetch: ApiFetch;

  beforeEach(() => {
    // Use a simple in-memory storage that actually stores values
    const data: Record<string, string> = {};
    storage = {
      getItem: (key: string) => data[key] ?? null,
      setItem: (key: string, value: string) => {
        data[key] = value;
      },
      removeItem: (key: string) => {
        delete data[key];
      },
      clear: () => {
        for (const key in data) {
          delete data[key];
        }
      },
      key: (index: number) => {
        const keys = Object.keys(data);
        return keys[index] ?? null;
      },
      get length(): number {
        return Object.keys(data).length;
      },
    };

    recordedCalls = [];

    // Create a recording double for ApiFetch
    mockFetch = async (path, init) => {
      recordedCalls.push({ path, init });
      return {
        status: 200,
        json: async () => ({}),
      } as ApiResponse;
    };
  });

  it("adds a bearer header when this browser holds a session", async () => {
    storage.setItem("pd.sessionToken", "tok-7");

    const wrapped = authorizedFetch(mockFetch, storage);
    await wrapped("/api/me", { headers: {} });

    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0].init.headers.Authorization).toBe("Bearer tok-7");
  });

  it("sends exactly the headers it was given when this browser holds none", async () => {
    const wrapped = authorizedFetch(mockFetch, storage);
    await wrapped("/api/me", { headers: { "X-Device-Id": "d-1" } });

    expect(recordedCalls).toHaveLength(1);
    const headerKeys = Object.keys(recordedCalls[0].init.headers);
    expect(headerKeys).toEqual(["X-Device-Id"]);
  });

  it("keeps the device id header the caller built", async () => {
    storage.setItem("pd.sessionToken", "tok-7");

    const wrapped = authorizedFetch(mockFetch, storage);
    await wrapped("/api/me", { headers: { "X-Device-Id": "d-1" } });

    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0].init.headers["X-Device-Id"]).toBe("d-1");
    expect(recordedCalls[0].init.headers.Authorization).toBe("Bearer tok-7");
  });

  it("passes the path, the method and the body through untouched", async () => {
    const wrapped = authorizedFetch(mockFetch, storage);
    const body = '{"key": "value"}';
    await wrapped("/api/me?query=1", { headers: {}, method: "PUT", body });

    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0].path).toBe("/api/me?query=1");
    expect(recordedCalls[0].init.method).toBe("PUT");
    expect(recordedCalls[0].init.body).toBe(body);
  });

  it("reads the token on every call, not once", async () => {
    const wrapped = authorizedFetch(mockFetch, storage);

    // First call with no token
    await wrapped("/api/me", { headers: {} });
    expect(recordedCalls[0].init.headers.Authorization).toBeUndefined();

    // Write a token
    storage.setItem("pd.sessionToken", "tok-7");

    // Second call should pick up the token
    await wrapped("/api/me", { headers: {} });
    expect(recordedCalls[1].init.headers.Authorization).toBe("Bearer tok-7");
  });
});
