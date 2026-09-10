import { type ReactElement } from "react";
import { InvitePanel } from "./InvitePanel";

/**
 * ADR-0129 §1: the host-alone table says, in one sentence, that the duel begins
 * by itself when the rival arrives. §4 pins no literal — the glyphs are the card's
 * (design/screens/duel-table.html, the <p class="autostart"> node), so this is a
 * transcription and never a re-wording. A verify: gate compares the two.
 */
const DUEL_STARTS_BY_ITSELF =
  "The duel starts by itself the moment your rival arrives, with nothing more needed from you.";

/**
 * The host waits here alone after creating a room and before the rival arrives.
 * The rival's empty seat says "Waiting for your rival", the invite is drawn at the
 * table, and the way back is beside the host's own seat. ADR-0110 §3: no game fact
 * is shown before the opening Snapshot — no pot, no board, no bar, no dealer button,
 * no stack numeral, no blind level, no hand number, no street name, no card or suit
 * glyph, no timer, no amount.
 *
 * The host's own plate is not drawn: with no name, no stack and no status to put
 * on it, a bordered box with nothing inside read as a rendering fault rather than
 * as a seat.
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
        <span className="waiting-dot" aria-hidden="true" />
        <span className="block text-text-faint">Waiting for your rival</span>
      </div>

      {/* The invite panel — code, link box, copy button with two states
          (at rest and after either outcome) — (ADR-0110 §5 as ADR-0128 §§1 and 3 amend it). */}
      <div className="flex flex-1 flex-col justify-center gap-4 py-6">
        <InvitePanel code={props.code} />
        {/* ADR-0129 §1: one sentence, once, in this state only. */}
        <p className="text-center text-small text-text-muted">
          {DUEL_STARTS_BY_ITSELF}
        </p>
      </div>

      {/* The way back to the lobby (ADR-0073 §§2-3): "Back to the lobby" as an
          anchor link to "/" with today's class list, and beside it the promise
          message that the room stays open and the rival's link still works. */}
      <a
        className="rounded-medium border border-hairline px-5 py-4 text-center leading-tight font-medium text-text"
        href="/"
        onClick={props.onLeave}
      >
        Back to the lobby
      </a>
      <p className="text-center text-small text-text-muted">
        The room stays open. That link still works for your rival, and it brings
        you back.
      </p>
    </section>
  );
}
