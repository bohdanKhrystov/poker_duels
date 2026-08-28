import { act, render } from "@testing-library/react";
import {
  AccountProvider,
  type AccountCalls,
} from "../account/account-provider";
import { authorizedFetch } from "../account/authorized-fetch";
import { revokeThisDevice } from "../account/revoke-device";
import { signIn } from "../account/sign-in";
import { signOut } from "../account/sign-out";
import { signUp } from "../account/sign-up";
import { Lobby } from "../lobby/Lobby";
import { readDuelPage, type DuelPageRead } from "../profile/duel-page";
import type { HistoryQuery } from "../profile/duels-query";
import { ProfileProvider } from "../profile/profile-provider";
import { readProfileStrip } from "../profile/profile-strip";
import { setDisplayName } from "../profile/set-name";
import { SetNameProvider } from "../profile/set-name-provider";
import { openConnection } from "../protocol";
import { FakeSocket } from "../protocol/fake-socket";
import { readSessionToken } from "../protocol/session-token";
import {
  readOfferSettled,
  markOfferSettled,
} from "../result/account-offer-settled";
import { hashForScreen } from "../routing/screen";
import { bootDuelClient } from "../store/boot";
import { DuelProvider } from "../store/duel-provider";
import type { AccountServer } from "./account-server";

/**
 * The seam `main.tsx` cannot offer a prop for: `Lobby.tsx` reads
 * `useHistory`, `useSignedIn`, `offerSettledHere` and `settleOfferHere`
 * from `../main`'s module scope, each read straight off the import, so no
 * prop passed to `bootClient` can reach them.
 *
 * `vi.mock` is hoisted and file-scoped, so the mock that redirects those
 * four bindings has to live in the test file, never here — this is the
 * plain, mutable object the two sides share: the test file builds it with
 * `vi.hoisted` and points a partial `vi.mock("../main", …)` at its fields,
 * and `bootClient` below writes all four fields on every boot before
 * rendering.
 *
 * Carries no `ladder` field: `LadderProvider` is absent from the tree this
 * module builds, and no test in this story reaches the leaderboard through
 * it, so `useLadder` is left at `../main`'s own actual implementation, which
 * reads the same missing context and answers `null` on its own.
 */
export interface ArcWiring {
  history: ((query: HistoryQuery) => Promise<DuelPageRead>) | null;
  signedIn: boolean;
  offerSettled: () => boolean;
  settleOffer: () => void;
}

export interface BootOptions {
  readonly storage: Storage;
  readonly server: AccountServer;
  readonly wiring: ArcWiring;
  /** Replayed through `socket` after `socket.open()`, so this boot has an identity. */
  readonly welcomeFrame: string;
}

export interface BootResult {
  readonly container: HTMLElement;
  readonly socket: FakeSocket;
  /**
   * How many times this boot asked to reload. Nothing here actually
   * reloads the document — a caller that wants the next boot calls
   * `bootClient` again, which *is* what a reload is in this harness.
   */
  readonly reloads: () => number;
}

/**
 * Mounts the real `Lobby` over the real `AccountProvider`, `ProfileProvider`,
 * `SetNameProvider` and `DuelProvider`, wired exactly as `main.tsx` wires
 * them, except every read and write goes through `options.storage` and
 * `options.server.fetch` rather than `localStorage` and `window.fetch`, and
 * the socket underneath the duel connection is a `FakeSocket` the caller can
 * drive afterwards.
 *
 * `HistoryProvider`, `LadderProvider` and `SignedInProvider` are absent from
 * the tree: `options.wiring` stands in for the seam `Lobby.tsx` reaches for
 * instead (see `ArcWiring`).
 *
 * The profile read that `ProfileProvider` fires at mount runs before
 * `options.welcomeFrame` is replayed, over whatever device id
 * `options.storage` already held when this call started — a fresh `Storage`
 * therefore mints a device id and reads no profile in the same boot; a
 * second `bootClient` call over that same `Storage` is what reads it.
 */
export function bootClient(options: BootOptions): BootResult {
  const { storage, server, wiring, welcomeFrame } = options;

  // authorizedFetch(server.fetch, storage) for every /api/me read, mirroring
  // main.tsx's apiFetch — the unwrapped server.fetch is used directly below,
  // for the four AccountCalls only: authorized-fetch.ts's contract forbids
  // wrapping sign-in, and main.tsx keeps the other three unwrapped beside it.
  const apiFetch = authorizedFetch(server.fetch, storage);

  const readProfile = () => readProfileStrip({ fetch: apiFetch, storage });

  const setName = (name: string) =>
    setDisplayName({ fetch: apiFetch, storage, name });

  const readHistory = (query: HistoryQuery): Promise<DuelPageRead> =>
    readDuelPage({ fetch: apiFetch, storage, query });

  let reloadCount = 0;
  const reload = (): void => {
    reloadCount += 1;
  };
  // ADR-0083 §5, mirroring main.tsx's reloadAtAccount: a successful sign-in
  // starts the next boot at #/account. The address change is real; only the
  // reload itself is replaced by a count, since reloading the document mid-test
  // would discard the very tree this harness was asked to hand back.
  const reloadAtAccount = (): void => {
    window.history.replaceState(null, "", hashForScreen("account"));
    reloadCount += 1;
  };

  const accountCalls: AccountCalls = {
    signUp: (handle, password) =>
      signUp({ fetch: server.fetch, storage, handle, password }),
    signIn: (handle, password) =>
      signIn({
        fetch: server.fetch,
        storage,
        reload: reloadAtAccount,
        handle,
        password,
      }),
    signOut: () => signOut({ fetch: server.fetch, storage, reload }),
    revokeThisDevice: () => revokeThisDevice({ fetch: server.fetch, storage }),
  };

  wiring.history = readHistory;
  wiring.signedIn = readSessionToken(storage) !== null;
  wiring.offerSettled = () => readOfferSettled(storage);
  wiring.settleOffer = () => markOfferSettled(storage);

  const socket = new FakeSocket();
  const client = bootDuelClient({
    connect: (onMessage) =>
      openConnection({ socket: socket.asWebSocket(), storage, onMessage }),
    joinRoomCode: null,
    storage,
  });

  const { container } = render(
    <AccountProvider calls={accountCalls}>
      <ProfileProvider read={readProfile}>
        <SetNameProvider setName={setName}>
          <DuelProvider
            store={client.store}
            send={client.send}
            forgetRoom={client.forgetRoom}
          >
            <Lobby />
          </DuelProvider>
        </SetNameProvider>
      </ProfileProvider>
    </AccountProvider>,
  );

  act(() => {
    socket.open();
  });
  act(() => {
    socket.receive(welcomeFrame);
  });

  return {
    container,
    socket,
    reloads: () => reloadCount,
  };
}
