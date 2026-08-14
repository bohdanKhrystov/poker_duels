import { Lobby } from "./lobby/Lobby";

export function App() {
  return (
    <main className="min-h-screen bg-bg p-6 font-ui text-text">
      <h1 className="text-title">Poker Duels</h1>
      <Lobby />
    </main>
  );
}
