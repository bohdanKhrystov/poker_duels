import { describe, expect, it, vi } from "vitest";
import { signOut } from "./sign-out";
import {
  readSessionToken,
  SESSION_TOKEN_STORAGE_KEY,
} from "../protocol/session-token";
import { readDeviceId, DEVICE_ID_STORAGE_KEY } from "../protocol/device-id";
import { readRoomCode, ROOM_CODE_STORAGE_KEY } from "../protocol/room-memory";
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
    // reading nothing. Throwing here proves signOut never tries.
    json: async () => {
      throw new Error("a 204 body must never be parsed");
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

describe("signing out", () => {
  it("clears the token and leaves the device id where it was when the profile stays", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-1",
      [DEVICE_ID_STORAGE_KEY]: "d-before",
    });
    const { fetch } = recordingFetch(async () => noContentResponse());

    const outcome = await signOut({
      fetch,
      storage,
      reload: vi.fn(),
      handsANewProfile: false,
    });

    expect(outcome).toEqual({ kind: "signed-out" });
    expect(readSessionToken(storage)).toBeNull();
    expect(readDeviceId(storage)).toBe("d-before");
  });

  it("forgets the room this tab remembered, because it is somebody else now", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-2",
      [ROOM_CODE_STORAGE_KEY]: "ROOM0002",
    });
    const { fetch } = recordingFetch(async () => noContentResponse());

    await signOut({
      fetch,
      storage,
      reload: vi.fn(),
      handsANewProfile: false,
    });

    expect(readRoomCode(storage)).toBeNull();
  });

  it("presents the session and no device id", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-3",
      [DEVICE_ID_STORAGE_KEY]: "d-leak-check",
    });
    const { fetch, calls } = recordingFetch(async () => noContentResponse());

    await signOut({
      fetch,
      storage,
      reload: vi.fn(),
      handsANewProfile: false,
    });

    expect(Object.keys(calls[0].init.headers).sort()).toEqual([
      "Authorization",
    ]);
    expect(calls[0].init.headers.Authorization).toBe("Bearer tok-3");
    expect(calls[0].init.body).toBeUndefined();
  });

  it("clears the token even when the server never answers", async () => {
    const rejectedStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-4a",
      [ROOM_CODE_STORAGE_KEY]: "ROOM4001",
    });
    const { fetch: rejectedFetch } = rejectingFetch();
    const reloadOnRejected = vi.fn();

    const rejectedOutcome = await signOut({
      fetch: rejectedFetch,
      storage: rejectedStorage,
      reload: reloadOnRejected,
      handsANewProfile: false,
    });

    expect(rejectedOutcome).toEqual({ kind: "signed-out" });
    expect(readSessionToken(rejectedStorage)).toBeNull();
    expect(readRoomCode(rejectedStorage)).toBeNull();
    expect(reloadOnRejected).toHaveBeenCalledTimes(1);

    const serverErrorStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-4b",
      [ROOM_CODE_STORAGE_KEY]: "ROOM4002",
    });
    const { fetch: serverErrorFetch } = recordingFetch(async () =>
      serverErrorResponse(),
    );
    const reloadOnServerError = vi.fn();

    const serverErrorOutcome = await signOut({
      fetch: serverErrorFetch,
      storage: serverErrorStorage,
      reload: reloadOnServerError,
      handsANewProfile: false,
    });

    expect(serverErrorOutcome).toEqual({ kind: "signed-out" });
    expect(readSessionToken(serverErrorStorage)).toBeNull();
    expect(readRoomCode(serverErrorStorage)).toBeNull();
    expect(reloadOnServerError).toHaveBeenCalledTimes(1);
  });

  it("asks nothing of the server when no session is held", async () => {
    const storage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-untouched",
    });
    const { fetch, calls } = recordingFetch(async () => noContentResponse());
    const reload = vi.fn();

    const outcome = await signOut({
      fetch,
      storage,
      reload,
      handsANewProfile: true,
    });

    expect(outcome).toEqual({ kind: "not-signed-in" });
    expect(calls.length).toBe(0);
    expect(reload).toHaveBeenCalledTimes(0);
    expect(readDeviceId(storage)).toBe("d-untouched");
  });

  it("reloads once, after the local half is done", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-6",
    });
    const { fetch } = recordingFetch(async () => noContentResponse());
    const reload = vi.fn(() => {
      expect(readSessionToken(storage)).toBeNull();
    });

    await signOut({ fetch, storage, reload, handsANewProfile: false });

    expect(reload).toHaveBeenCalledTimes(1);
  });

  it("told the sign-out hands a new profile, it forgets the device id", async () => {
    const storage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-7",
      [DEVICE_ID_STORAGE_KEY]: "d-abandoned",
    });
    const { fetch } = recordingFetch(async () => noContentResponse());

    await signOut({
      fetch,
      storage,
      reload: vi.fn(),
      handsANewProfile: true,
    });

    expect(readDeviceId(storage)).toBeNull();
    expect(storage.getItem(DEVICE_ID_STORAGE_KEY)).toBeNull();
  });

  it("the device id goes whatever the server answered", async () => {
    const rejectedStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-8a",
      [DEVICE_ID_STORAGE_KEY]: "d-rejected",
    });
    const { fetch: rejectedFetch } = rejectingFetch();
    const reloadOnRejected = vi.fn();

    await signOut({
      fetch: rejectedFetch,
      storage: rejectedStorage,
      reload: reloadOnRejected,
      handsANewProfile: true,
    });

    expect(readDeviceId(rejectedStorage)).toBeNull();
    expect(reloadOnRejected).toHaveBeenCalledTimes(1);

    const serverErrorStorage = inMemoryStorage({
      [SESSION_TOKEN_STORAGE_KEY]: "tok-8b",
      [DEVICE_ID_STORAGE_KEY]: "d-server-error",
    });
    const { fetch: serverErrorFetch } = recordingFetch(async () =>
      serverErrorResponse(),
    );
    const reloadOnServerError = vi.fn();

    await signOut({
      fetch: serverErrorFetch,
      storage: serverErrorStorage,
      reload: reloadOnServerError,
      handsANewProfile: true,
    });

    expect(readDeviceId(serverErrorStorage)).toBeNull();
    expect(reloadOnServerError).toHaveBeenCalledTimes(1);
  });

  it("no key beyond the three ever moves", async () => {
    const NAME_ASK_SKIPPED_KEY = "pd.nameAskSkipped";

    const abandoningStorage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-abandoning",
      [SESSION_TOKEN_STORAGE_KEY]: "tok-9a",
      [ROOM_CODE_STORAGE_KEY]: "ROOM9001",
      [NAME_ASK_SKIPPED_KEY]: "1",
    });
    const { fetch: abandoningFetch } = recordingFetch(async () =>
      noContentResponse(),
    );

    await signOut({
      fetch: abandoningFetch,
      storage: abandoningStorage,
      reload: vi.fn(),
      handsANewProfile: true,
    });

    expect(abandoningStorage.getItem(NAME_ASK_SKIPPED_KEY)).toBe("1");
    expect(abandoningStorage.length).toBe(1);

    const stayingStorage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-staying",
      [SESSION_TOKEN_STORAGE_KEY]: "tok-9b",
      [ROOM_CODE_STORAGE_KEY]: "ROOM9002",
      [NAME_ASK_SKIPPED_KEY]: "1",
    });
    const { fetch: stayingFetch } = recordingFetch(async () =>
      noContentResponse(),
    );

    await signOut({
      fetch: stayingFetch,
      storage: stayingStorage,
      reload: vi.fn(),
      handsANewProfile: false,
    });

    expect(readDeviceId(stayingStorage)).toBe("d-staying");
    expect(stayingStorage.getItem(NAME_ASK_SKIPPED_KEY)).toBe("1");
    expect(stayingStorage.length).toBe(2);
  });
});
