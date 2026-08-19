import "./styles/app.css";

import React, {
  createContext,
  useContext,
  type ReactElement,
  type ReactNode,
} from "react";
import ReactDOM from "react-dom/client";
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

// Module scope, so the provider's effect sees one stable reference and one
// mount means one read. An arrow written inline in the JSX would be a new
// function on every render.
const readProfile = (): Promise<ProfileStripState> =>
  readProfileStrip({
    fetch: (path, init) => window.fetch(path, init),
    storage: localStorage,
  });

// Module scope, so the provider hands down one stable reference. A reference
// that changed on every render would be a new function on every render.
const setName = (name: string) =>
  setDisplayName({
    fetch: (path, init) => window.fetch(path, init),
    storage: localStorage,
    name,
  });

// Module scope, so a component's effect sees one stable reference and one
// mount means one read setup. An arrow written inline in the JSX would be a new
// function on every render.
const readHistory = (query: HistoryQuery): Promise<DuelPageRead> =>
  readDuelPage({
    fetch: (path, init) => window.fetch(path, init),
    storage: localStorage,
    query,
  });

// One boot per tab, outside the tree (ADR-0032): StrictMode below may mount and
// unmount as often as it likes without opening a socket or sending a frame.
const client = bootDuelClient({
  connect: connectToDuelServer,
  joinRoomCode: roomCodeFromSearch(window.location.search),
  storage: localStorage,
});

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

const container = document.getElementById("root");
if (container) {
  ReactDOM.createRoot(container).render(
    <React.StrictMode>
      <ProfileProvider read={readProfile}>
        <SetNameProvider setName={setName}>
          <HistoryProvider>
            <DuelProvider store={client.store} send={client.send}>
              <App />
            </DuelProvider>
          </HistoryProvider>
        </SetNameProvider>
      </ProfileProvider>
    </React.StrictMode>,
  );
}
