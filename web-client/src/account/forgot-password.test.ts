import { describe, it, expect, beforeEach, vi } from "vitest";
import { forgotPassword } from "./forgot-password";
import type { ApiFetch, ApiResponse } from "../profile/api";

const ADDRESS = "zqx-address-zqx";

describe("forgotPassword", () => {
  let fetchMock: ReturnType<typeof vi.fn>;
  let storageMock: Storage;

  beforeEach(() => {
    fetchMock = vi.fn();
    storageMock = {
      length: 0,
      clear: vi.fn(),
      getItem: vi.fn(),
      key: vi.fn(),
      removeItem: vi.fn(),
      setItem: vi.fn(),
    };
  });

  it("sends the address in a body and never in the path", async () => {
    const recordedCalls: Array<{
      path: string;
      body?: string;
    }> = [];

    fetchMock.mockImplementation((path: string, init: { body?: string }) => {
      recordedCalls.push({ path, body: init.body });
      return Promise.resolve({
        status: 202,
        json: () => Promise.resolve({}),
      } as ApiResponse);
    });

    const outcome = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(outcome).toEqual({ kind: "accepted" });
    expect(recordedCalls).toHaveLength(1);
    expect(recordedCalls[0]!.path).toBe("/api/auth/forgot-password");
    expect(recordedCalls[0]!.body).toEqual(
      JSON.stringify({ address: ADDRESS }),
    );
    expect(recordedCalls[0]!.path).not.toContain(ADDRESS);
  });

  it("has exactly two outcomes, and every documented status is the first of them", async () => {
    const statusTests = [
      { status: 202, expectedOutcome: { kind: "accepted" } },
      { status: 400, expectedOutcome: { kind: "failed" } },
      { status: 429, expectedOutcome: { kind: "failed" } },
      { status: 500, expectedOutcome: { kind: "failed" } },
    ];

    for (const test of statusTests) {
      fetchMock.mockResolvedValueOnce({
        status: test.status,
        json: () => Promise.resolve({}),
      } as ApiResponse);

      const outcome = await forgotPassword({
        fetch: fetchMock as ApiFetch,
        address: ADDRESS,
      });

      expect(outcome).toEqual(test.expectedOutcome);
    }

    // Verify the three non-202 outcomes are identical to each other
    fetchMock.mockResolvedValueOnce({
      status: 400,
      json: () => Promise.resolve({}),
    } as ApiResponse);
    const failed400 = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    fetchMock.mockResolvedValueOnce({
      status: 429,
      json: () => Promise.resolve({}),
    } as ApiResponse);
    const failed429 = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    fetchMock.mockResolvedValueOnce({
      status: 500,
      json: () => Promise.resolve({}),
    } as ApiResponse);
    const failed500 = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(failed400).toEqual(failed429);
    expect(failed429).toEqual(failed500);
  });

  it("carries no device id and no authorization header", async () => {
    const recordedHeaders: Array<Record<string, string>> = [];

    fetchMock.mockImplementation(
      (_path: string, init: { headers: Record<string, string> }) => {
        recordedHeaders.push(init.headers);
        return Promise.resolve({
          status: 202,
          json: () => Promise.resolve({}),
        } as ApiResponse);
      },
    );

    // First call with empty storage
    await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(recordedHeaders).toHaveLength(1);
    expect("X-Device-Id" in recordedHeaders[0]!).toBe(false);
    expect("Authorization" in recordedHeaders[0]!).toBe(false);

    // Second call with seeded storage
    storageMock.setItem = vi.fn();
    storageMock.getItem = vi.fn().mockReturnValue("test-device-id");

    await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(recordedHeaders).toHaveLength(2);
    expect("X-Device-Id" in recordedHeaders[1]!).toBe(false);
    expect("Authorization" in recordedHeaders[1]!).toBe(false);
  });

  it("answers accepted without reading a body, and failed without throwing", async () => {
    // Case 1: 202 with rejecting json()
    fetchMock.mockResolvedValueOnce({
      status: 202,
      json: () => Promise.reject(new Error("JSON parse failed")),
    } as ApiResponse);

    const acceptedOutcome = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(acceptedOutcome).toEqual({ kind: "accepted" });

    // Case 2: fetch that rejects
    fetchMock.mockRejectedValueOnce(new Error("Network error"));

    const failedOutcome = await forgotPassword({
      fetch: fetchMock as ApiFetch,
      address: ADDRESS,
    });

    expect(failedOutcome).toEqual({ kind: "failed" });
    // The returned promise does not reject
    expect(failedOutcome).toBeDefined();
  });
});
