import { Lobby } from "./lobby/Lobby";
import { RematchNotice } from "./result/RematchNotice";

export function App() {
  return (
    <main className="min-h-screen bg-bg font-ui text-text">
      <Lobby />
      <RematchNotice />
    </main>
  );
}
