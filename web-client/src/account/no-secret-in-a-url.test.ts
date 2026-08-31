import { describe, it, expect, vi, afterEach } from "vitest";
import { signUp } from "./sign-up";
import { signIn } from "./sign-in";
import { attachRecoveryEmail } from "./attach-recovery-email";
import { forgotPassword } from "./forgot-password";
import { verifyEmail } from "./verify-email";
import { resetPassword } from "./reset-password";
import { signOut } from "./sign-out";
import { revokeThisDevice } from "./revoke-device";
import { writeDeviceId } from "../protocol/device-id";
import { writeSessionToken } from "../protocol/session-token";
import type { ApiFetch, ApiResponse } from "../profile/api";

/**
 * Distinctive on purpose (`TASK-041224`): each is a substring that appears nowhere else in this
 * client's fixed literal paths (`/api/auth/sign-up`, `/api/auth/sign-in`, `/api/auth/sign-out`,
 * `/api/me/device`), so a real leak is what makes a check fail here, never a coincidence. A short
 * common string like `"a"` would make a path check pass by accident; these cannot.
 */
const HANDLE = "zqx-handle-zqx";
const PASSWORD = "zqx-password-zqx";
const TOKEN = "zqx-token-zqx";
const DEVICE_ID = "zqx-device-zqx";

/**
 * The four recovery secrets, in the same `zqx-…-zqx` idiom: a recovery address the player types,
 * the two tokens a mailed link carries, and the new password a reset sets.
 */
const ADDRESS = "zqx-address-zqx";
const VERIFY_TOKEN = "zqx-verify-token-zqx";
const RESET_TOKEN = "zqx-reset-token-zqx";
const NEW_PASSWORD = "zqx-new-password-zqx";

/** The seven secrets a player types, is issued or is mailed; checked against every path and the address bar. */
const SECRETS = [
  HANDLE,
  PASSWORD,
  TOKEN,
  ADDRESS,
  VERIFY_TOKEN,
  RESET_TOKEN,
  NEW_PASSWORD,
] as const;

/** `EPIC-04`'s non-negotiable: a request body carrying one of these is a defect, whatever its value. */
const FORBIDDEN_BODY_KEYS = [
  "playerId",
  "player_id",
  "deviceId",
  "id",
] as const;

/**
 * Fixed by the order `driveEveryAccountCall` below awaits them in — used to name a failing call
 * against `hrefsAfterEachCall`, which has one entry per awaited call, not per request sent.
 */
const CALL_LABELS = [
  "signUp",
  "signIn",
  "attachRecoveryEmail",
  "forgotPassword",
  "verifyEmail",
  "resetPassword",
  "revokeThisDevice",
  "signOut",
] as const;

/**
 * Fixed by the order `driveEveryAccountCall` below sends requests in — used to name a failing
 * entry in `calls`, which `signUp` now contributes two of (`TASK-120601`): the request it sends
 * itself, and the `POST /api/auth/sign-in` it sends right after a `201`, before this file's own
 * explicit `signIn` gets its turn.
 */
const FETCH_LABELS = [
  "signUp",
  "signUp's follow-up signIn",
  "signIn",
  "attachRecoveryEmail",
  "forgotPassword",
  "verifyEmail",
  "resetPassword",
  "revokeThisDevice",
  "signOut",
] as const;

/**
 * An in-memory `Storage`, deliberately not the global `localStorage` — the same reason
 * `profile-no-derivation.test.tsx` keeps its own copy of this function: Node 24+ defines a
 * `localStorage` global that is present but inert under Vitest, shadowing jsdom's, so depending on
 * it would make this test a property of the Node version rather than of the module under test.
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

/** What one recorded call looked like: its path in full, its headers, and its body if it sent one. */
interface RecordedCall {
  readonly path: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly body: string | undefined;
}

/**
 * A `fetch` double that records every call it receives and answers them in order — the same shape
 * `profile-no-derivation.test.tsx`'s `answering` helper uses, extended to capture `body`, since six
 * of the eight calls here write one and the sweep must see it.
 */
function recordingFetch(...answers: readonly ApiResponse[]): {
  readonly calls: RecordedCall[];
  readonly fetch: ApiFetch;
} {
  const calls: RecordedCall[] = [];
  let answerIndex = 0;

  return {
    calls,
    fetch: async (path, init) => {
      calls.push({
        path,
        headers: { ...init.headers },
        body: init.body as string | undefined,
      });
      if (answerIndex >= answers.length) {
        throw new Error(
          `recordingFetch: no answer queued for call ${answerIndex + 1} (${path})`,
        );
      }
      return answers[answerIndex++];
    },
  };
}

/**
 * Drives all eight account calls, in an order chosen so `signOut` — which forgets the stored
 * session token — runs last: `revokeThisDevice` needs that token still present when its own turn
 * comes, and the four recovery calls run between `signIn` and `revokeThisDevice` so neither one
 * disturbs the token `revokeThisDevice` and `signOut` still need (none of the four write it).
 * Storage is (re-)seeded with both credentials right before every call that reads them, so no call
 * is ever skipped for want of a precondition and the recorder always ends with exactly nine
 * entries — eight named calls, plus the sign-in `signUp` now sends on its own behalf after a `201`
 * (`TASK-120601`).
 */
async function driveEveryAccountCall(): Promise<{
  readonly calls: readonly RecordedCall[];
  readonly hrefsAfterEachCall: readonly string[];
}> {
  const storage = inMemoryStorage();
  const recorder = recordingFetch(
    { status: 201, json: async () => ({}) }, // signUp
    { status: 200, json: async () => ({ sessionToken: TOKEN }) }, // signUp's own follow-up signIn
    { status: 200, json: async () => ({ sessionToken: TOKEN }) }, // signIn
    { status: 202, json: async () => ({}) }, // attachRecoveryEmail
    { status: 202, json: async () => ({}) }, // forgotPassword
    { status: 204, json: async () => ({}) }, // verifyEmail
    { status: 204, json: async () => ({}) }, // resetPassword
    { status: 204, json: async () => ({}) }, // revokeThisDevice
    { status: 204, json: async () => ({}) }, // signOut — its response is never inspected
  );
  const reload = (): void => {};
  const hrefsAfterEachCall: string[] = [];

  writeDeviceId(storage, DEVICE_ID);
  await signUp({
    fetch: recorder.fetch,
    storage,
    handle: HANDLE,
    password: PASSWORD,
  });
  hrefsAfterEachCall.push(window.location.href);

  await signIn({
    fetch: recorder.fetch,
    storage,
    reload,
    handle: HANDLE,
    password: PASSWORD,
  });
  hrefsAfterEachCall.push(window.location.href);

  await attachRecoveryEmail({
    fetch: recorder.fetch,
    storage,
    address: ADDRESS,
    currentPassword: PASSWORD,
  });
  hrefsAfterEachCall.push(window.location.href);

  await forgotPassword({ fetch: recorder.fetch, address: ADDRESS });
  hrefsAfterEachCall.push(window.location.href);

  await verifyEmail({ fetch: recorder.fetch, token: VERIFY_TOKEN });
  hrefsAfterEachCall.push(window.location.href);

  await resetPassword({
    fetch: recorder.fetch,
    token: RESET_TOKEN,
    newPassword: NEW_PASSWORD,
  });
  hrefsAfterEachCall.push(window.location.href);

  writeSessionToken(storage, TOKEN);
  await revokeThisDevice({ fetch: recorder.fetch, storage });
  hrefsAfterEachCall.push(window.location.href);

  writeSessionToken(storage, TOKEN);
  await signOut({ fetch: recorder.fetch, storage, reload });
  hrefsAfterEachCall.push(window.location.href);

  return { calls: recorder.calls, hrefsAfterEachCall };
}

describe("no secret reaches a URL", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("puts no handle, password or token in any path it requests", async () => {
    const { calls } = await driveEveryAccountCall();

    // Presence, before absence: the drive produced exactly the nine calls it claims to, and the
    // secrets this test is about to search for really did reach a real request somewhere legitimate
    // — a body field, a header — so the checks below are over something, not over nothing.
    expect(calls.length).toBe(9);
    expect(calls.some((call) => call.body?.includes(HANDLE))).toBe(true);
    expect(calls.some((call) => call.body?.includes(PASSWORD))).toBe(true);
    expect(
      calls.some((call) =>
        Object.values(call.headers).some((value) => value.includes(TOKEN)),
      ),
    ).toBe(true);
    expect(calls.some((call) => call.body?.includes(ADDRESS))).toBe(true);
    expect(calls.some((call) => call.body?.includes(RESET_TOKEN))).toBe(true);

    // Absence: nine calls times seven secrets, every failure naming both.
    for (const [index, call] of calls.entries()) {
      for (const secret of SECRETS) {
        expect(
          call.path.includes(secret),
          `${FETCH_LABELS[index]}'s path (${JSON.stringify(call.path)}) must not contain ${JSON.stringify(secret)}`,
        ).toBe(false);
      }
    }
  });

  it("sends no player id in any body it writes", async () => {
    const { calls } = await driveEveryAccountCall();
    expect(calls.length).toBe(9);
    const [
      signUpCall,
      ,
      signInCall,
      attachRecoveryEmailCall,
      forgotPasswordCall,
      verifyEmailCall,
      resetPasswordCall,
      revokeCall,
      signOutCall,
    ] = calls;

    // Presence: signUp and signIn really do carry a body, and it is the real credentials — not an
    // empty object a forbidden-keys check would pass over vacuously.
    expect(JSON.parse(signUpCall.body as string)).toMatchObject({
      handle: HANDLE,
      password: PASSWORD,
    });
    expect(JSON.parse(signInCall.body as string)).toMatchObject({
      handle: HANDLE,
      password: PASSWORD,
    });

    // The two calls documented as sending no body are asserted, by name, to send none — so a body
    // appearing on either of them later is caught rather than silently skipped by a loop that only
    // looks where a body already exists.
    expect(revokeCall.body).toBeUndefined();
    expect(signOutCall.body).toBeUndefined();

    const namedBodies: ReadonlyArray<{
      readonly label: string;
      readonly body: string;
    }> = [
      { label: "signUp", body: signUpCall.body as string },
      { label: "signIn", body: signInCall.body as string },
      {
        label: "attachRecoveryEmail",
        body: attachRecoveryEmailCall.body as string,
      },
      { label: "forgotPassword", body: forgotPasswordCall.body as string },
      { label: "verifyEmail", body: verifyEmailCall.body as string },
      { label: "resetPassword", body: resetPasswordCall.body as string },
    ];
    for (const { label, body } of namedBodies) {
      const parsedKeys = Object.keys(JSON.parse(body));
      for (const forbidden of FORBIDDEN_BODY_KEYS) {
        expect(
          parsedKeys.includes(forbidden),
          `${label}'s body (keys: ${parsedKeys.join(", ")}) must not carry the key ${JSON.stringify(forbidden)}`,
        ).toBe(false);
      }
    }
  });

  it("leaves no secret in the address bar after any of the four calls", async () => {
    const before = window.location.href;
    expect(before.length).toBeGreaterThan(0);

    const { calls, hrefsAfterEachCall } = await driveEveryAccountCall();
    expect(calls.length).toBe(9);
    expect(hrefsAfterEachCall.length).toBe(8);

    for (const [index, href] of hrefsAfterEachCall.entries()) {
      for (const secret of SECRETS) {
        expect(
          href.includes(secret),
          `window.location.href after ${CALL_LABELS[index]} (${JSON.stringify(href)}) must not contain ${JSON.stringify(secret)}`,
        ).toBe(false);
      }
    }

    // None of the eight calls navigates, so the address bar this client controls is unchanged —
    // asserted rather than assumed. A real browser's Referer header is a different surface this
    // sweep cannot reach at all; see no-secret-in-a-url.md.
    expect(window.location.href).toBe(before);
  });

  it("puts no secret in anything it logs", async () => {
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});
    const warnSpy = vi.spyOn(console, "warn").mockImplementation(() => {});
    const errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    const { calls } = await driveEveryAccountCall();
    expect(calls.length).toBe(9);

    const logged = [
      ...logSpy.mock.calls,
      ...warnSpy.mock.calls,
      ...errorSpy.mock.calls,
    ]
      .flat()
      .map((argument) =>
        typeof argument === "string" ? argument : JSON.stringify(argument),
      )
      .join(" ");

    for (const secret of SECRETS) {
      expect(
        logged.includes(secret),
        `console output (${JSON.stringify(logged)}) must not contain ${JSON.stringify(secret)}`,
      ).toBe(false);
    }
  });

  it("sends the device id and the session token in headers and nowhere else", async () => {
    const { calls, hrefsAfterEachCall } = await driveEveryAccountCall();
    expect(calls.length).toBe(9);
    const [signUpCall, , , , , , , revokeCall, signOutCall] = calls;

    // Positive half: both bearer credentials really did travel, each in a header, at least once —
    // proving they were used, rather than proving nothing was ever sent.
    expect(signUpCall.headers["X-Device-Id"]).toBe(DEVICE_ID);
    expect(revokeCall.headers.Authorization).toBe(`Bearer ${TOKEN}`);
    expect(signOutCall.headers.Authorization).toBe(`Bearer ${TOKEN}`);

    // Negative half: neither value is anywhere else — not in a path, not in a body, not in the
    // address bar.
    const bearerSecrets = [DEVICE_ID, TOKEN] as const;
    for (const [index, call] of calls.entries()) {
      for (const secret of bearerSecrets) {
        expect(
          call.path.includes(secret),
          `${FETCH_LABELS[index]}'s path must not contain ${JSON.stringify(secret)}`,
        ).toBe(false);
        if (call.body !== undefined) {
          expect(
            call.body.includes(secret),
            `${FETCH_LABELS[index]}'s body must not contain ${JSON.stringify(secret)}`,
          ).toBe(false);
        }
      }
    }
    for (const href of hrefsAfterEachCall) {
      for (const secret of bearerSecrets) {
        expect(href.includes(secret)).toBe(false);
      }
    }
  });
});
