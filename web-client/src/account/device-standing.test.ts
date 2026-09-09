import { describe, expect, it } from "vitest";

import type { ApiFetch, ApiResponse } from "../profile/api";
import { readDeviceStanding } from "./device-standing";

/** A `Storage` double good enough for `readSessionToken` and `readDeviceId`. */
class MemoryStorage implements Storage {
  private readonly values = new Map<string, string>();

  get length(): number {
    return this.values.size;
  }

  clear(): void {
    this.values.clear();
  }

  getItem(key: string): string | null {
    return this.values.has(key) ? (this.values.get(key) as string) : null;
  }

  key(index: number): string | null {
    return Array.from(this.values.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.values.delete(key);
  }

  setItem(key: string, value: string): void {
    this.values.set(key, value);
  }
}

interface RecordedCall {
  readonly path: string;
  readonly headers: Readonly<Record<string, string>>;
}

/** An `ApiFetch` double that records every call and answers a fixed response. */
function recordingFetch(
  calls: RecordedCall[],
  respond: () => Promise<ApiResponse> | ApiResponse,
): ApiFetch {
  return async (path, init) => {
    calls.push({ path, headers: init.headers });
    return respond();
  };
}

function jsonResponse(status: number, body: unknown): ApiResponse {
  return {
    status,
    json: async () => body,
  };
}

describe("readDeviceStanding", () => {
  it("a browser holding no session asks nothing and keeps its profile", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.deviceId", "device-1");
    const calls: RecordedCall[] = [];
    const fetch = recordingFetch(calls, () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );

    const answer = await readDeviceStanding({ fetch, storage });

    expect(calls.length).toBe(0);
    expect(answer).toBe(false);
  });

  it("a browser holding a token but no device id still asks", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.sessionToken", "token-1");
    const calls: RecordedCall[] = [];
    const fetch = recordingFetch(calls, () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );

    await readDeviceStanding({ fetch, storage });

    expect(calls.length).toBe(1);
    expect(calls[0]?.path).toBe("/api/me/device");
    expect(Object.keys(calls[0]?.headers ?? {})).toEqual(["Authorization"]);
  });

  it("a browser holding both presents both", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.sessionToken", "token-2");
    storage.setItem("pd.deviceId", "device-2");
    const calls: RecordedCall[] = [];
    const fetch = recordingFetch(calls, () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );

    await readDeviceStanding({ fetch, storage });

    const headers = calls[0]?.headers ?? {};
    expect(Object.keys(headers).sort()).toEqual([
      "Authorization",
      "X-Device-Id",
    ]);
    expect(headers["Authorization"]).toBe("Bearer token-2");
    expect(headers["X-Device-Id"]).toBe("device-2");
  });

  it("the answer is the one the server gave", async () => {
    const storageTrue = new MemoryStorage();
    storageTrue.setItem("pd.sessionToken", "token-3");
    const fetchTrue = recordingFetch([], () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );
    expect(
      await readDeviceStanding({ fetch: fetchTrue, storage: storageTrue }),
    ).toBe(true);

    const storageFalse = new MemoryStorage();
    storageFalse.setItem("pd.sessionToken", "token-4");
    const fetchFalse = recordingFetch([], () =>
      jsonResponse(200, { signOutHandsANewProfile: false }),
    );
    expect(
      await readDeviceStanding({ fetch: fetchFalse, storage: storageFalse }),
    ).toBe(false);
  });

  it("a refusal is a keep", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.sessionToken", "token-5");
    const refused = recordingFetch([], () => jsonResponse(401, {}));
    expect(await readDeviceStanding({ fetch: refused, storage })).toBe(false);

    const allowed = recordingFetch([], () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );
    expect(await readDeviceStanding({ fetch: allowed, storage })).toBe(true);
  });

  it("an answer this client cannot read is a keep", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.sessionToken", "token-6");

    const missingField = recordingFetch([], () => jsonResponse(200, {}));
    expect(await readDeviceStanding({ fetch: missingField, storage })).toBe(
      false,
    );

    const stringField = recordingFetch([], () =>
      jsonResponse(200, { signOutHandsANewProfile: "true" }),
    );
    expect(await readDeviceStanding({ fetch: stringField, storage })).toBe(
      false,
    );

    const nullField = recordingFetch([], () =>
      jsonResponse(200, { signOutHandsANewProfile: null }),
    );
    expect(await readDeviceStanding({ fetch: nullField, storage })).toBe(false);

    const rejectingJson: ApiFetch = async () => ({
      status: 200,
      json: async () => {
        throw new Error("malformed body");
      },
    });
    expect(await readDeviceStanding({ fetch: rejectingJson, storage })).toBe(
      false,
    );
  });

  it("a server that never answers is a keep", async () => {
    const storage = new MemoryStorage();
    storage.setItem("pd.sessionToken", "token-7");

    const rejectingFetch: ApiFetch = async () => {
      throw new Error("network down");
    };
    expect(await readDeviceStanding({ fetch: rejectingFetch, storage })).toBe(
      false,
    );

    const serverError = recordingFetch([], () =>
      jsonResponse(500, { signOutHandsANewProfile: true }),
    );
    expect(await readDeviceStanding({ fetch: serverError, storage })).toBe(
      false,
    );
  });

  it("a storage that throws on the token read is a keep", async () => {
    const storage: Storage = {
      length: 0,
      clear() {},
      getItem(key: string) {
        if (key === "pd.sessionToken") {
          throw new Error("storage access blocked");
        }
        return null;
      },
      key() {
        return null;
      },
      removeItem() {},
      setItem() {},
    };
    const calls: RecordedCall[] = [];
    const fetch = recordingFetch(calls, () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );

    const answer = await readDeviceStanding({ fetch, storage });

    expect(calls.length).toBe(0);
    expect(answer).toBe(false);
  });

  it("a storage that throws on the device-id read is a keep", async () => {
    const storage: Storage = {
      length: 0,
      clear() {},
      getItem(key: string) {
        if (key === "pd.deviceId") {
          throw new Error("storage access blocked");
        }
        // Return token for session token read
        return key === "pd.sessionToken" ? "token-8" : null;
      },
      key() {
        return null;
      },
      removeItem() {},
      setItem() {},
    };
    const calls: RecordedCall[] = [];
    const fetch = recordingFetch(calls, () =>
      jsonResponse(200, { signOutHandsANewProfile: true }),
    );

    const answer = await readDeviceStanding({ fetch, storage });

    expect(calls.length).toBe(0);
    expect(answer).toBe(false);
  });
});
