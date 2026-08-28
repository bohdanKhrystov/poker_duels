import { describe, it, expect, beforeEach } from "vitest";
import {
  attachRecoveryEmail,
  type AttachRecoveryOutcome,
} from "./attach-recovery-email";
import type { ApiFetch, ApiResponse } from "../profile/api";
import { writeDeviceId } from "../protocol/device-id";

const ADDRESS = "zqx-address-zqx";
const CURRENT = "zqx-current-zqx";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`.
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

/** Creates a fetch that rejects immediately on json(), as if reading an empty body would. */
function rejectingOnJson(): {
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
      return {
        status: 202,
        json: async () => {
          throw new Error("body must not be read for 202");
        },
      };
    },
  };
}

/** Constructs a response with no body, for a status `docs/protocol.md` answers empty. */
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

describe("attaching a recovery email", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = inMemoryStorage();
  });

  it("sends the address and the current password in a body, and neither in the path", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(emptyBody(202));

    await attachRecoveryEmail({
      fetch: mock.fetch,
      storage,
      address: ADDRESS,
      currentPassword: CURRENT,
    });

    const [call] = mock.calls;
    expect(mock.calls).toHaveLength(1);
    expect(call.method).toBe("POST");

    // Presence first: prove both values really did travel
    const body = parsedBody(call) as Record<string, unknown>;
    expect(body).toEqual({
      address: ADDRESS,
      currentPassword: CURRENT,
    });

    // Now absence assertions over something that was proven to exist
    expect(call.path).toBe("/api/auth/recovery-email");
    expect(call.path).not.toContain(ADDRESS);
    expect(call.path).not.toContain(CURRENT);

    // The device id binds this request to an account
    expect(call.headers["X-Device-Id"]).toBe("d-1");
  });

  it("maps every status the endpoint documents to its own outcome", async () => {
    const statusCases: ReadonlyArray<{
      readonly status: number;
      readonly kind: AttachRecoveryOutcome["kind"];
    }> = [
      { status: 202, kind: "accepted" },
      { status: 400, kind: "address-refused" },
      { status: 401, kind: "no-profile" },
      { status: 403, kind: "password-refused" },
      { status: 500, kind: "failed" },
    ];

    const kinds: AttachRecoveryOutcome["kind"][] = [];

    for (const statusCase of statusCases) {
      const s = inMemoryStorage();
      writeDeviceId(s, "d-1");
      const mock = answering(emptyBody(statusCase.status));

      const outcome = await attachRecoveryEmail({
        fetch: mock.fetch,
        storage: s,
        address: "test@example.com",
        currentPassword: "password123",
      });

      kinds.push(outcome.kind);
      expect(outcome.kind).toBe(statusCase.kind);
    }

    // Assert that all five statuses map to distinct kinds
    expect(new Set(kinds).size).toBe(5);
  });

  it("sends nothing at all when this browser holds no device id", async () => {
    const mock = answering();

    const outcome = await attachRecoveryEmail({
      fetch: mock.fetch,
      storage,
      address: "test@example.com",
      currentPassword: "password123",
    });

    expect(mock.calls).toHaveLength(0);
    expect(outcome.kind).toBe("no-profile");
  });

  it("sends the address exactly as it was given", async () => {
    const testAddress = " TestAddress.";
    writeDeviceId(storage, "d-1");
    const mock = answering(emptyBody(202));

    await attachRecoveryEmail({
      fetch: mock.fetch,
      storage,
      address: testAddress,
      currentPassword: "password123",
    });

    const [call] = mock.calls;
    const body = parsedBody(call) as Record<string, string>;
    expect(body.address).toBe(testAddress);
  });

  it("answers failed when the fetch rejects, and does not throw", async () => {
    writeDeviceId(storage, "d-1");
    const mock = rejecting();

    const outcome = await attachRecoveryEmail({
      fetch: mock.fetch,
      storage,
      address: "test@example.com",
      currentPassword: "password123",
    });

    expect(outcome.kind).toBe("failed");
    expect(mock.calls).toHaveLength(1);

    // Now test with a double whose json() rejects: the module never calls json(),
    // so this must still be accepted.
    const s = inMemoryStorage();
    writeDeviceId(s, "d-1");
    const mock2 = rejectingOnJson();

    const outcome2 = await attachRecoveryEmail({
      fetch: mock2.fetch,
      storage: s,
      address: "test@example.com",
      currentPassword: "password123",
    });

    expect(outcome2.kind).toBe("accepted");
    expect(mock2.calls).toHaveLength(1);
  });

  it("sends one request and never a second", async () => {
    writeDeviceId(storage, "d-1");
    const mock = answering(emptyBody(403));

    await attachRecoveryEmail({
      fetch: mock.fetch,
      storage,
      address: "test@example.com",
      currentPassword: "password123",
    });

    expect(mock.calls).toHaveLength(1);
  });
});
