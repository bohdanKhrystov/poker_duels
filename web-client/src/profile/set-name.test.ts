import { describe, it, expect, beforeEach } from "vitest";
import { setDisplayName, type SetNameOutcome } from "./set-name";
import type { ApiFetch, ApiResponse } from "./api";
import { writeDeviceId } from "../protocol/device-id";
import { meBody } from "./profile-fixture";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
 *
 * Node 24+ defines its own `localStorage` global which is present but inert
 * unless the process is started with `--localstorage-file`, and under Vitest it
 * shadows the one jsdom provides: `typeof localStorage` is `"undefined"` while
 * `sessionStorage` — which Node keeps in memory — works. `setDisplayName` takes
 * the `Storage` it acts on as a parameter, so the tests hand it one and rely on
 * no global at all.
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

/** Records the details of each fetch call made. */
interface FetchCall {
  readonly path: string;
  readonly method: string | undefined;
  readonly headers: Record<string, string>;
  readonly body: string | undefined;
}

/** The shape `ApiFetch`'s init parameter takes, mirrored here for the mocks below. */
type FetchInit = {
  readonly headers: Readonly<Record<string, string>>;
  readonly method?: string;
  readonly body?: string;
};

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
    fetch: async (path: string, init: FetchInit): Promise<ApiResponse> => {
      calls.push({
        path,
        method: init.method,
        headers: { ...init.headers },
        body: init.body,
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

/** Creates a mock fetch that records every call and always rejects, as a network failure would. */
function rejecting(): {
  readonly calls: FetchCall[];
  readonly fetch: ApiFetch;
} {
  const calls: FetchCall[] = [];

  return {
    calls,
    fetch: async (path: string, init: FetchInit): Promise<ApiResponse> => {
      calls.push({
        path,
        method: init.method,
        headers: { ...init.headers },
        body: init.body,
      });
      throw new Error("network unavailable");
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

/**
 * Constructs a response with no body, for a status `docs/protocol.md` answers
 * empty. `json()` throws so a `setDisplayName` that reads it by mistake fails
 * loudly instead of quietly landing on the right outcome for the wrong reason.
 */
function emptyBody(status: number): ApiResponse {
  return {
    status,
    json: async () => {
      throw new Error(`the body for status ${status} must not be read`);
    },
  };
}

/** Parses a recorded call's JSON body, failing loudly if the call carried none. */
function parsedBody(call: FetchCall): unknown {
  if (call.body === undefined) {
    throw new Error("Expected a body");
  }
  return JSON.parse(call.body);
}

describe("setting a display name", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("sends one PUT to /api/me/name carrying the device id and the name as typed", async () => {
    // Two distinct typed strings, so a hardcoded body cannot pass.
    const cases = [
      { deviceId: "d-1", name: "  Ada  " },
      { deviceId: "d-2", name: "Grace" },
    ];

    for (const testCase of cases) {
      const s = inMemoryStorage();
      writeDeviceId(s, testCase.deviceId);
      const mock = answering(ok(meBody()));

      await setDisplayName({
        fetch: mock.fetch,
        storage: s,
        name: testCase.name,
      });

      expect(mock.calls).toHaveLength(1);
      const [call] = mock.calls;
      expect(call.method).toBe("PUT");
      expect(call.path).toBe("/api/me/name");
      expect(call.headers["X-Device-Id"]).toBe(testCase.deviceId);
      expect(parsedBody(call)).toEqual({ name: testCase.name });
    }
  });

  it("sends a body whose only key is the name", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(ok(meBody()));

    await setDisplayName({ fetch: mock.fetch, storage, name: "Ada" });

    const [call] = mock.calls;
    expect(Object.keys(parsedBody(call) as Record<string, unknown>)).toEqual([
      "name",
    ]);
  });

  it("answers with the profile the server returned, not the name that was typed", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(ok(meBody({ displayName: "Ada" })));

    const outcome = await setDisplayName({
      fetch: mock.fetch,
      storage,
      name: "  ada  ",
    });

    expect(outcome.kind).toBe("named");
    if (outcome.kind !== "named") {
      throw new Error("Expected a named outcome");
    }
    expect(outcome.profile.displayName).toBe("Ada");
    // The exact string typed appears nowhere in the outcome: only the
    // server's canonical answer does.
    expect(JSON.stringify(outcome)).not.toContain("  ada  ");
  });

  it("gives each status its own outcome", async () => {
    const statusCases: ReadonlyArray<{
      readonly status: number;
      readonly kind: SetNameOutcome["kind"];
    }> = [
      { status: 400, kind: "rejected" },
      { status: 403, kind: "permanent" },
      { status: 409, kind: "conflict" },
      { status: 401, kind: "no-profile" },
      { status: 500, kind: "unavailable" },
    ];

    for (const statusCase of statusCases) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(statusCase.status));

      const outcome = await setDisplayName({
        fetch: mock.fetch,
        storage: s,
        name: "Ada",
      });

      expect(mock.calls).toHaveLength(1);
      expect(outcome.kind).toBe(statusCase.kind);
    }

    // An empty storage — no device id ever written — must answer no-profile
    // without making a request. Asserted on the call count first, so a client
    // that calls fetch anyway is caught here rather than by whatever outcome
    // an unconfigured mock happens to produce.
    const mock = answering();
    const outcome = await setDisplayName({
      fetch: mock.fetch,
      storage: inMemoryStorage(),
      name: "Ada",
    });

    expect(mock.calls).toHaveLength(0);
    expect(outcome.kind).toBe("no-profile");
  });

  it("answers unavailable when the fetch rejects, and sends nothing more", async () => {
    writeDeviceId(storage, "d-1");
    const mock = rejecting();

    const outcome = await setDisplayName({
      fetch: mock.fetch,
      storage,
      name: "Ada",
    });

    expect(outcome.kind).toBe("unavailable");
    expect(mock.calls).toHaveLength(1);
  });
});
