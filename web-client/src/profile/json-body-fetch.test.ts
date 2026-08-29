import { describe, it, expect, beforeEach } from "vitest";
import type { ApiFetch, ApiResponse } from "./api";
import { jsonBodyFetch } from "./json-body-fetch";

describe("a fetch that labels its own body", () => {
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

  it("declares application/json on a request that carries a body", async () => {
    const wrapped = jsonBodyFetch(mockFetch);
    const body = '{"handle":"a-handle","password":"a-password"}';

    await wrapped("/api/auth/sign-up", {
      headers: { "X-Device-Id": "d-1" },
      method: "POST",
      body,
    });

    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0].init.headers["Content-Type"]).toBe(
      "application/json",
    );
  });

  it("sends no content type on a request with no body", async () => {
    const wrapped = jsonBodyFetch(mockFetch);

    await wrapped("/api/me", { headers: { "X-Device-Id": "d-1" } });

    expect(recordedCalls).toHaveLength(1);
    // The whole headers object, not merely "not application/json": a
    // wrapper that mislabels a bodyless request as text/plain would still
    // pass a narrower check. Nothing may be added when there is no body.
    expect(recordedCalls[0].init.headers).toEqual({ "X-Device-Id": "d-1" });
  });

  it("does not overwrite a content type the caller set", async () => {
    const wrapped = jsonBodyFetch(mockFetch);
    const body = "<root/>";

    await wrapped("/api/legacy", {
      headers: { "Content-Type": "application/xml" },
      method: "POST",
      body,
    });

    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0].init.headers["Content-Type"]).toBe(
      "application/xml",
    );
  });

  it("keys off the body, not the method", async () => {
    const wrapped = jsonBodyFetch(mockFetch);

    // POST, no body: a wrapper gated on method rather than on init.body
    // would label this anyway — sign-out and revokeThisDevice post exactly
    // this shape today.
    await wrapped("/api/auth/sign-out", { headers: {}, method: "POST" });

    // DELETE, with a body: a wrapper gated on method would leave this
    // unlabelled, since DELETE is neither POST nor PUT.
    const body = '{"key":"value"}';
    await wrapped("/api/legacy", { headers: {}, method: "DELETE", body });

    expect(recordedCalls).toHaveLength(2);
    expect(recordedCalls[0].init.headers).toEqual({});
    expect(recordedCalls[1].init.headers["Content-Type"]).toBe(
      "application/json",
    );
  });
});
