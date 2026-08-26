import { describe, expect, it, vi } from "vitest";
import { signIn } from "./sign-in";
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

function okResponse(body: unknown): ApiResponse {
  return {
    status: 200,
    json: async () => body,
  };
}

function refusedResponse(): ApiResponse {
  return {
    status: 401,
    // The protocol's 401 body is empty; a client that parsed it would be
    // reading nothing. Throwing here proves signIn never tries.
    json: async () => {
      throw new Error("a 401 body must never be parsed");
    },
  };
}

describe("signing in", () => {
  it("stores the token and leaves the device id exactly where it was", async () => {
    const storage = inMemoryStorage({ [DEVICE_ID_STORAGE_KEY]: "d-before" });
    const { fetch } = recordingFetch(async () =>
      okResponse({ sessionToken: "tok-9" }),
    );

    const outcome = await signIn({
      fetch,
      storage,
      reload: vi.fn(),
      handle: "player-one",
      password: "hunter2",
    });

    expect(outcome).toEqual({ kind: "signed-in" });
    expect(readSessionToken(storage)).toBe("tok-9");
    expect(readDeviceId(storage)).toBe("d-before");
  });

  it("answers a wrong password and an unknown handle identically", async () => {
    const wrongPassword = recordingFetch(async () => refusedResponse());
    const unknownHandle = recordingFetch(async () => refusedResponse());

    const forWrongPassword = await signIn({
      fetch: wrongPassword.fetch,
      storage: inMemoryStorage(),
      reload: vi.fn(),
      handle: "known-handle",
      password: "wrong-password",
    });
    const forUnknownHandle = await signIn({
      fetch: unknownHandle.fetch,
      storage: inMemoryStorage(),
      reload: vi.fn(),
      handle: "unknown-handle",
      password: "any-password",
    });

    expect(forWrongPassword).toEqual({ kind: "refused" });
    expect(forUnknownHandle).toEqual({ kind: "refused" });
    expect(forWrongPassword).toEqual(forUnknownHandle);
  });

  it("sends no device id and no authorization of its own", async () => {
    const storage = inMemoryStorage({
      [DEVICE_ID_STORAGE_KEY]: "d-1",
      [SESSION_TOKEN_STORAGE_KEY]: "tok-old",
    });
    const { fetch, calls } = recordingFetch(async () => refusedResponse());

    await signIn({
      fetch,
      storage,
      reload: vi.fn(),
      handle: "player-one",
      password: "wrong-password",
    });

    expect(Object.keys(calls[0].init.headers).sort()).toEqual([]);
  });

  it("sends the handle and the password and nothing else", async () => {
    const { fetch, calls } = recordingFetch(async () => refusedResponse());

    await signIn({
      fetch,
      storage: inMemoryStorage(),
      reload: vi.fn(),
      handle: "player-one",
      password: "correct-horse-battery",
    });

    const body = JSON.parse(calls[0].init.body ?? "{}") as Record<
      string,
      unknown
    >;
    expect(Object.keys(body).sort()).toEqual(["handle", "password"]);
    expect(body.handle).toBe("player-one");
    expect(body.password).toBe("correct-horse-battery");
  });

  it("reloads the document once a session exists, and not before", async () => {
    const signedIn = recordingFetch(async () =>
      okResponse({ sessionToken: "tok-reload" }),
    );
    const reloadOnSignedIn = vi.fn();
    await signIn({
      fetch: signedIn.fetch,
      storage: inMemoryStorage(),
      reload: reloadOnSignedIn,
      handle: "player-one",
      password: "hunter2",
    });
    expect(reloadOnSignedIn).toHaveBeenCalledTimes(1);

    const refused = recordingFetch(async () => refusedResponse());
    const reloadOnRefused = vi.fn();
    await signIn({
      fetch: refused.fetch,
      storage: inMemoryStorage(),
      reload: reloadOnRefused,
      handle: "player-one",
      password: "wrong-password",
    });
    expect(reloadOnRefused).toHaveBeenCalledTimes(0);

    const rejected = rejectingFetch();
    const reloadOnRejected = vi.fn();
    await signIn({
      fetch: rejected.fetch,
      storage: inMemoryStorage(),
      reload: reloadOnRejected,
      handle: "player-one",
      password: "hunter2",
    });
    expect(reloadOnRejected).toHaveBeenCalledTimes(0);
  });

  it("stores nothing when a 200 carries no token", async () => {
    const storage = inMemoryStorage();
    const reload = vi.fn();
    const { fetch } = recordingFetch(async () => okResponse({}));

    const outcome = await signIn({
      fetch,
      storage,
      reload,
      handle: "player-one",
      password: "hunter2",
    });

    expect(outcome).toEqual({ kind: "failed" });
    expect(readSessionToken(storage)).toBeNull();
    expect(reload).toHaveBeenCalledTimes(0);
  });

  it("sends one request and never a second", async () => {
    const { fetch, calls } = recordingFetch(async () => refusedResponse());

    await signIn({
      fetch,
      storage: inMemoryStorage(),
      reload: vi.fn(),
      handle: "player-one",
      password: "wrong-password",
    });

    expect(calls.length).toBe(1);
  });
});
