import "./styles/app.css";

import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { roomCodeFromSearch } from "./lobby/room-link";
import { connectToDuelServer } from "./protocol";
import { bootDuelClient } from "./store/boot";
import { DuelProvider } from "./store/duel-provider";

const container = document.getElementById("root");
if (!container) throw new Error("missing #root");

// One boot per tab, outside the tree (ADR-0032): StrictMode below may mount and
// unmount as often as it likes without opening a socket or sending a frame.
const client = bootDuelClient({
  connect: connectToDuelServer,
  joinRoomCode: roomCodeFromSearch(window.location.search),
  storage: localStorage,
});

ReactDOM.createRoot(container).render(
  <React.StrictMode>
    <DuelProvider store={client.store} send={client.send}>
      <App />
    </DuelProvider>
  </React.StrictMode>,
);
