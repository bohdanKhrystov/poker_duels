import "./styles/app.css";

import React, {
  createContext,
  useContext,
  type ReactElement,
  type ReactNode,
} from "react";
import ReactDOM from "react-dom/client";
import { authorizedFetch } from "./account/authorized-fetch";
import { AccountProvider, type AccountCalls } from "./account/account-provider";
import { signIn } from "./account/sign-in";
import { signOut } from "./account/sign-out";
import { signUp } from "./account/sign-up";
import { revokeThisDevice } from "./account/revoke-device";
import { App } from "./App";
import { roomCodeFromSearch } from "./lobby/room-link";
import { connectToDuelServer } from "./protocol";
import { bootDuelClient } from "./store/boot";
import { DuelProvider } from "./store/duel-provider";
import { ProfileProvider } from "./profile/profile-provider";
import {
  readProfileStrip,
  type ProfileStripState,
} from "./profile/profile-strip";
import { SetNameProvider } from "./profile/set-name-provider";
import { setDisplayName } from "./profile/set-name";
import { readDuelPage, type DuelPageRead } from "./profile/duel-page";
import type { HistoryQuery } from "./profile/duels-query";
import { readLadderPage, type LadderRead } from "./ladder/ladder-read";
import { readSessionToken } from "./protocol/session-token";
import type { ApiFetch } from "./profile/api";
import { hashForScreen } from "./routing/screen";

// Module scope, built once, so every read below shares one wrapper rather
// than each opening its own. authorizedFetch reads the session token on
// every call (not once), so this outlives a sign-out without keeping a
// signed-out browser signed in.
//
// The known consequence: readFromApi still answers no-profile without a
// request when this browser holds no device id, so a browser that holds a
// session token but no device id would read nothing. That case cannot
// arise — the device id is written by the first Welcome this browser ever
// receives and is never cleared and never overwritten, not on sign-in and
// not on sign-out (ADR-0030 §8) — so every browser able to hold a session
// token already holds a device id.
const apiFetch = authorizedFetch(
  (path, init) => window.fetch(path, init),
  localStorage,
);

// Module scope, so the provider's effect sees one stable reference and one
// mount means one read. An arrow written inline in the JSX would be a new
// function on every render.
const readProfile = (): Promise<ProfileStripState> =>
  readProfileStrip({
    fetch: apiFetch,
    storage: localStorage,
  });

// Module scope, so the provider hands down one stable reference. A reference
// that changed on every render would be a new function on every render.
const setName = (name: string) =>
  setDisplayName({
    fetch: apiFetch,
    storage: localStorage,
    name,
  });

// Module scope, so a component's effect sees one stable reference and one
// mount means one read setup. An arrow written inline in the JSX would be a new
// function on every render.
const readHistory = (query: HistoryQuery): Promise<DuelPageRead> =>
  readDuelPage({
    fetch: apiFetch,
    storage: localStorage,
    query,
  });

// Module scope, for the reason readHistory beside it gives: the effect that
// calls this needs one stable reference across renders, and an arrow written
// inline in the JSX would be a new function on every render.
const readLadder = (after: string | null): Promise<LadderRead> =>
  readLadderPage({
    fetch: apiFetch,
    storage: localStorage,
    after,
  });

const plainFetch: ApiFetch = (path, init) => window.fetch(path, init);

const reload = (): void => window.location.reload();

// ADR-0083 §5: a successful sign-in starts the next boot at #/account, never
// back at the screen it has just finished using. A replace rather than an
// assignment, because a pushed entry would put the Back button on #/sign-in
// for a browser that is now signed in.
const reloadAtAccount = (): void => {
  window.history.replaceState(null, "", hashForScreen("account"));
  window.location.reload();
};

const accountCalls: AccountCalls = {
  signUp: (handle, password) =>
    signUp({ fetch: plainFetch, storage: localStorage, handle, password }),
  signIn: (handle, password) =>
    signIn({
      fetch: plainFetch,
      storage: localStorage,
      reload: reloadAtAccount,
      handle,
      password,
    }),
  signOut: () => signOut({ fetch: plainFetch, storage: localStorage, reload }),
  revokeThisDevice: () =>
    revokeThisDevice({ fetch: plainFetch, storage: localStorage }),
};

// One boot per tab, outside the tree (ADR-0032): StrictMode below may mount and
// unmount as often as it likes without opening a socket or sending a frame.
const client = bootDuelClient({
  connect: connectToDuelServer,
  joinRoomCode: roomCodeFromSearch(window.location.search),
  storage: localStorage,
});

// Fallback Storage for test environments where localStorage is undefined.
// In the browser, localStorage is always defined; Node tests must provide jsdom
// or mock main.tsx. This fallback is used when neither condition is met.
const nullStorage: Storage = {
  getItem: () => null,
  setItem: () => {},
  removeItem: () => {},
  clear: () => {},
  key: () => null,
  length: 0,
};

// Module scope read of the session token, run once at boot. Node 24+ defines an
// inert `localStorage` global that shadows jsdom's under Vitest (DEC-032), so a
// component that reaches for the global is a component whose tests do not test
// the browser. Sign-in and sign-out reload the document, so a fresh boot is what
// recomputes it. The hook never re-reads: there is no storage event listener and
// no subscription.
const signedIn = readSessionToken(localStorage ?? nullStorage) !== null;

const HistoryContext = createContext<
  ((query: HistoryQuery) => Promise<DuelPageRead>) | null
>(null);

/**
 * Runs `readHistory` once above the tree and puts it within reach of the lobby.
 *
 * The read is a module-scope constant (not an inline arrow) so a component's
 * effect sees one stable reference: a reference that changes on every render
 * would re-run the effect on every render with it.
 */
export function HistoryProvider(props: { children: ReactNode }): ReactElement {
  return (
    <HistoryContext.Provider value={readHistory}>
      {props.children}
    </HistoryContext.Provider>
  );
}

/** The history read bound to `window.fetch` and `localStorage`, or `null` where no provider is above. */
export function useHistory():
  ((query: HistoryQuery) => Promise<DuelPageRead>) | null {
  return useContext(HistoryContext);
}

const LadderContext = createContext<
  ((after: string | null) => Promise<LadderRead>) | null
>(null);

/**
 * Runs `readLadder` once above the tree and puts it within reach of the lobby.
 *
 * The read is a module-scope constant (not an inline arrow) so a component's
 * effect sees one stable reference: a reference that changes on every render
 * would re-run the effect on every render with it.
 */
export function LadderProvider(props: { children: ReactNode }): ReactElement {
  return (
    <LadderContext.Provider value={readLadder}>
      {props.children}
    </LadderContext.Provider>
  );
}

/** The ladder read bound to `window.fetch` and `localStorage`, or `null` where no provider is above. */
export function useLadder():
  ((after: string | null) => Promise<LadderRead>) | null {
  return useContext(LadderContext);
}

const SignedInContext = createContext<boolean>(false);

/**
 * Whether this browser holds a session token, once at module scope and never again.
 *
 * The read is a module-scope constant (not an inline arrow) so a component's
 * effect sees one stable reference: a reference that changes on every render
 * would re-run the effect on every render with it.
 */
export function SignedInProvider(props: { children: ReactNode }): ReactElement {
  return (
    <SignedInContext.Provider value={signedIn}>
      {props.children}
    </SignedInContext.Provider>
  );
}

/** Whether this browser holds a session token, or `false` where no provider is above. */
export function useSignedIn(): boolean {
  return useContext(SignedInContext);
}

const container = document.getElementById("root");
if (container) {
  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <SignedInProvider>
        <AccountProvider calls={accountCalls}>
          <ProfileProvider read={readProfile}>
            <SetNameProvider setName={setName}>
              <HistoryProvider>
                <LadderProvider>
                  <DuelProvider
                    store={client.store}
                    send={client.send}
                    forgetRoom={client.forgetRoom}
                  >
                    <App />
                  </DuelProvider>
                </LadderProvider>
              </HistoryProvider>
            </SetNameProvider>
          </ProfileProvider>
        </AccountProvider>
      </SignedInProvider>
    </React.StrictMode>,
  );
}
