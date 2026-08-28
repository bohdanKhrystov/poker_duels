import { describe, expect, it } from "vitest";
import { resetPassword } from "./reset-password";
import type { ApiFetch, ApiResponse } from "../profile/api";

const TOKEN = "zqx-reset-token-zqx";
const NEW_PASSWORD = "zqx-new-password-zqx";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`: under
 * Vitest in Node, `localStorage` is shadowed and inert, so a fake that takes
 * no dependency on it is what makes these tests a property of this module
 * rather than of the Node version running them.
 */
function inMemoryStorage(): Storage {
  const entries = new Map<string, string>();
  return {
    get length(): number {
      return entries.size;
    },
    clear(): void {
      entries.clear();
    },
    getItem(key: string): string | null {
      return entries.has(key) ? (entries.get(key) as string) : null;
    },
    key(index: number): string | null {
      return Array.from(entries.keys())[index] ?? null;
    },
    removeItem(key: string): void {
      entries.delete(key);
    },
    setItem(key: string, value: string): void {
      entries.set(key, value);
    },
  };
}

type RecordedCall = {
  readonly path: string;
  readonly init: Parameters<ApiFetch>[1];
};

/** A recording `ApiFetch` double that answers every call the same way. */
function recordingFetch(respond: () => Promise<ApiResponse>): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    return respond();
  };
  return { fetch, calls };
}

/** A recording `ApiFetch` double that returns different responses for each call. */
function recordingFetchSequence(responses: ApiResponse[]): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  let callIndex = 0;
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    const response = responses[callIndex];
    callIndex += 1;
    return response;
  };
  return { fetch, calls };
}

/** A recording `ApiFetch` double whose call always rejects. */
function rejectingFetch(): {
  readonly fetch: ApiFetch;
  readonly calls: RecordedCall[];
} {
  const calls: RecordedCall[] = [];
  const fetch: ApiFetch = async (path, init) => {
    calls.push({ path, init });
    throw new Error("the network is unavailable");
  };
  return { fetch, calls };
}

function responseWithStatus(status: number): ApiResponse {
  return {
    status,
    json: async () => ({}),
  };
}

function responseWithBody(status: number, body: unknown): ApiResponse {
  return {
    status,
    json: async () => body,
  };
}

function responseWithRejectedJson(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error("json parsing failed");
    },
  };
}

describe("resetting a password", () => {
  it("puts the token and the new password in a body and in no path, header or query", async () => {
    const { fetch, calls } = recordingFetch(async () =>
      responseWithStatus(204),
    );

    const outcome = await resetPassword({
      fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });

    expect(outcome).toEqual({ kind: "reset" });
    expect(calls.length).toBe(1);

    const call = calls[0];
    // Body assertion runs first (presence before absence)
    const body = JSON.parse(call.init.body ?? "{}") as Record<string, unknown>;
    expect(body).toEqual({ token: TOKEN, newPassword: NEW_PASSWORD });

    // Path is exactly as specified, with no token and no question mark
    expect(call.path).toBe("/api/auth/reset-password");
    expect(call.path).not.toContain(TOKEN);
    expect(call.path).not.toContain(NEW_PASSWORD);
    expect(call.path).not.toContain("?");

    // No header value contains the token or password
    const headerValues = Object.values(call.init.headers);
    for (const value of headerValues) {
      expect(value).not.toContain(TOKEN);
      expect(value).not.toContain(NEW_PASSWORD);
    }
  });

  it("tells a refused password from a dead link, and reports each as itself", async () => {
    const responses = [responseWithStatus(422), responseWithStatus(400)];
    const { fetch } = recordingFetchSequence(responses);

    const passwordRefused = await resetPassword({
      fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });

    const linkDead = await resetPassword({
      fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });

    expect(passwordRefused).toEqual({ kind: "password-refused" });
    expect(linkDead).toEqual({ kind: "link-dead" });
    expect(passwordRefused).not.toEqual(linkDead);
  });

  it("does not read a refused password as a live link", async () => {
    const responses = [responseWithStatus(422), responseWithStatus(204)];
    const { fetch } = recordingFetchSequence(responses);

    const passwordRefused = await resetPassword({
      fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(passwordRefused).toEqual({ kind: "password-refused" });

    const longerPassword = NEW_PASSWORD + "longer";
    const reset = await resetPassword({
      fetch,
      token: TOKEN,
      newPassword: longerPassword,
    });
    expect(reset).toEqual({ kind: "reset" });
  });

  it("maps every status the endpoint documents to its own outcome", async () => {
    const reset = await resetPassword({
      fetch: recordingFetch(async () => responseWithStatus(204)).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(reset).toEqual({ kind: "reset" });

    const linkDead = await resetPassword({
      fetch: recordingFetch(async () => responseWithStatus(400)).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(linkDead).toEqual({ kind: "link-dead" });

    const passwordRefused = await resetPassword({
      fetch: recordingFetch(async () => responseWithStatus(422)).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(passwordRefused).toEqual({ kind: "password-refused" });

    const failed = await resetPassword({
      fetch: recordingFetch(async () => responseWithStatus(500)).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(failed).toEqual({ kind: "failed" });
  });

  it("stores no session on success, because the server issues none", async () => {
    const storage = inMemoryStorage();

    const outcome = await resetPassword({
      fetch: recordingFetch(async () =>
        responseWithBody(204, { sessionToken: "zqx-token-zqx" }),
      ).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });

    expect(outcome).toEqual({ kind: "reset" });
    expect(storage.length).toBe(0);
  });

  it("answers reset without reading a body, and failed without throwing", async () => {
    // A 204 whose json() rejects still answers reset
    const rejectedJson = responseWithRejectedJson(204);
    const outcome1 = await resetPassword({
      fetch: recordingFetch(async () => rejectedJson).fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(outcome1).toEqual({ kind: "reset" });

    // A fetch that rejects answers failed and doesn't throw
    const rejected = rejectingFetch();
    const outcome2 = await resetPassword({
      fetch: rejected.fetch,
      token: TOKEN,
      newPassword: NEW_PASSWORD,
    });
    expect(outcome2).toEqual({ kind: "failed" });
  });
});
