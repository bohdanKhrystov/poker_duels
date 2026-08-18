import "./styles/app.css";

import React from "react";
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

const container = document.getElementById("root");
if (!container) throw new Error("missing #root");

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

// One boot per tab, outside the tree (ADR-0032): StrictMode below may mount and
// unmount as often as it likes without opening a socket or sending a frame.
const client = bootDuelClient({
  connect: connectToDuelServer,
  joinRoomCode: roomCodeFromSearch(window.location.search),
  storage: localStorage,
});

ReactDOM.createRoot(container).render(
  <React.StrictMode>
    <ProfileProvider read={readProfile}>
      <SetNameProvider setName={setName}>
        <DuelProvider store={client.store} send={client.send}>
          <App />
        </DuelProvider>
      </SetNameProvider>
    </ProfileProvider>
  </React.StrictMode>,
);
