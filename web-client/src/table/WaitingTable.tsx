import { type ReactElement } from "react";
import { InvitePanel } from "./InvitePanel";

/**
 * The host waits here alone after creating a room and before the rival arrives.
 * The rival's empty seat says "Waiting for your rival", the invite is drawn at the
 * table, and the way back is beside the host's own seat. ADR-0110 §3: no game fact
 * is shown before the opening Snapshot — no pot, no board, no bar, no dealer button,
 * no stack numeral, no blind level, no hand number, no street name, no card or suit
 * glyph, no timer, no amount.
 */
export function WaitingTable(props: {
  readonly code: string;
  readonly onLeave: () => void;
}): ReactElement {
  return (
    <section className="[container-type:inline-size] mx-auto flex min-h-[100dvh] max-w-[560px] flex-col gap-[var(--wgap)] p-[var(--wgap)] [--wgap:clamp(4px,calc((100cqi-340px)/12.5),16px)]">
      {/* The rival's seat — one plate, drawn as the card's dashed twin, carrying
          the single string "Waiting for your rival": capital W, no full stop,
          no status line, no stack, no button (ADR-0110 §2). */}
      <div className="flex items-center gap-4 rounded-medium border border-dashed border-hairline px-5 py-4">
        <span className="block text-text-faint">Waiting for your rival</span>
      </div>

      {/* The invite panel — code, link box, copy button with three states
          (at rest, copied, refused, no clipboard) — (ADR-0110 §5). */}
      <InvitePanel code={props.code} />

      {/* The host's seat — the same plate, solid, carrying the single string
          "You" and no stack (ADR-0110 §2). */}
      <div className="flex items-center gap-4 rounded-medium border border-hairline bg-surface px-5 py-4">
        <span className="block font-medium">You</span>
      </div>

      {/* The way back to the lobby (ADR-0073 §§2-3): "Back to the lobby" as an
          anchor link to "/" with today's class list, and beside it the promise
          message that the room stays open and the rival's link still works. */}
      <a
        className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
        href="/"
        onClick={props.onLeave}
      >
        Back to the lobby
      </a>
      <p className="text-small text-text-muted">
        The room stays open. That link still works for your rival, and it brings
        you back.
      </p>
    </section>
  );
}
