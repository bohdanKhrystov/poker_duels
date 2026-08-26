import { describe, expect, it } from "vitest";
import { revokeThisDevice } from "./revoke-device";
import {
  readSessionToken,
  SESSION_TOKEN_STORAGE_KEY,
} from "../protocol/session-token";
import { readDeviceId, DEVICE_ID_STORAGE_KEY } from "../protocol/device-id";
import type { ApiFetch, ApiResponse } from "../profile/api";

/**
 * An in-memory `Storage`, deliberately not the global `localStorage`: under
 * Vitest in Node, `localStorage` is shadowed and inert, so a fake that takes
 * no dependency on it is what makes these tests a property of this module
 * rather than of the Node version running them.
 */
function inMemoryStorage(
  initial: Readonly<Record<string, string>> = {},
): Storage {
  const entries = new Map<string, string>(Object.entries(initial));
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

function noContentResponse(): ApiResponse {
  return {
    status: 204,
    // The protocol's 204 body is empty; a client that parsed it would be
    // reading nothing. Throwing here proves revokeThisDevice never tries.
    json: async () => {
      throw new Error("a 204 body must never be parsed");
    },
  };
}

function conflictResponse(): ApiResponse {
  return {
    status: 409,
    json: async () => {
      throw new Error("a 409 body must never be parsed");
    },
  };
}

function unauthorizedResponse(): ApiResponse {
  return {
    status: 401,
    json: async () => {
      throw new Error("a 401 body must never be parsed");
    },
  };
}

function serverErrorResponse(): ApiResponse {
  return {
    status: 500,
    json: async () => {
      throw new Error("a 500 body must never be parsed");
    },
  };
}

describe("stopping this device signing in", () => {
  it("presents the session and never the device it is revoking", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-5",
      [DEVICE_ID_STORAGE_KEY]: "d-never-sent",
    });
    const { fetch, calls } = recordingFetch(async () => noContentResponse());

    await revokeThisDevice({ fetch, storage });

    expect(calls[0].path).toBe("/api/me/device");
    expect(calls[0].init.method).toBe("DELETE");
    expect(Object.keys(calls[0].init.headers).sort()).toEqual([
      "Authorization",
    ]);
    expect(calls[0].init.headers.Authorization).toBe("Bearer tok-5");
    expect(calls[0].init.body).toBeUndefined();
  });

  it("keeps the session it revoked with, because that is the one the player is standing on", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-keep",
      [DEVICE_ID_STORAGE_KEY]: "d-keep",
    });
    const { fetch } = recordingFetch(async () => noContentResponse());

    await revokeThisDevice({ fetch, storage });

    expect(readSessionToken(storage)).toBe("tok-keep");
    expect(readDeviceId(storage)).toBe("d-keep");
  });

  it("tells a profile with no password apart from a caller with no session", async () => {
    const noPasswordStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-9",
    });
    const { fetch: noPasswordFetch } = recordingFetch(async () =>
      conflictResponse(),
    );

    const noPasswordOutcome = await revokeThisDevice({
      fetch: noPasswordFetch,
      storage: noPasswordStorage,
    });

    const noSessionStorage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-orphan",
    });
    const { fetch: noSessionFetch } = recordingFetch(async () =>
      unauthorizedResponse(),
    );

    const noSessionOutcome = await revokeThisDevice({
      fetch: noSessionFetch,
      storage: noSessionStorage,
    });

    expect(noPasswordOutcome).toEqual({ kind: "no-credential" });
    expect(noSessionOutcome).toEqual({ kind: "no-session" });
    expect(noPasswordOutcome).not.toEqual(noSessionOutcome);
  });

  it("treats a broken server as a failure and not as a refusal", async () => {
    const serverErrorStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-11a",
    });
    const { fetch: serverErrorFetch } = recordingFetch(async () =>
      serverErrorResponse(),
    );

    const serverErrorOutcome = await revokeThisDevice({
      fetch: serverErrorFetch,
      storage: serverErrorStorage,
    });

    const rejectedStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-11b",
    });
    const { fetch: rejectedFetch } = rejectingFetch();

    const rejectedOutcome = await revokeThisDevice({
      fetch: rejectedFetch,
      storage: rejectedStorage,
    });

    const noCredentialStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-11c",
    });
    const { fetch: noCredentialFetch } = recordingFetch(async () =>
      conflictResponse(),
    );

    const noCredentialOutcome = await revokeThisDevice({
      fetch: noCredentialFetch,
      storage: noCredentialStorage,
    });

    const noSessionOutcome = { kind: "no-session" } as const;

    expect(serverErrorOutcome).toEqual({ kind: "failed" });
    expect(rejectedOutcome).toEqual({ kind: "failed" });
    expect(serverErrorOutcome).toEqual(rejectedOutcome);
    expect(serverErrorOutcome).not.toEqual(noCredentialOutcome);
    expect(serverErrorOutcome).not.toEqual(noSessionOutcome);
  });

  it("asks nothing of the server when no session is held", async () => {
    const storage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-untouched",
    });
    const { fetch, calls } = recordingFetch(async () => noContentResponse());

    const outcome = await revokeThisDevice({ fetch, storage });

    expect(outcome).toEqual({ kind: "no-session" });
    expect(calls.length).toBe(0);
    expect(readDeviceId(storage)).toBe("d-untouched");
  });

  it("reports one answer for a live binding and an already dead one", async () => {
    const liveStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-live",
    });
    const { fetch: liveFetch } = recordingFetch(async () =>
      noContentResponse(),
    );

    const liveOutcome = await revokeThisDevice({
      fetch: liveFetch,
      storage: liveStorage,
    });

    const deadStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-dead",
    });
    const { fetch: deadFetch } = recordingFetch(async () =>
      noContentResponse(),
    );

    const deadOutcome = await revokeThisDevice({
      fetch: deadFetch,
      storage: deadStorage,
    });

    expect(liveOutcome).toEqual(deadOutcome);
    expect(liveOutcome).toEqual({ kind: "revoked" });
  });
});
