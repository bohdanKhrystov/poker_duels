import { useEffect, useState, type ReactElement } from "react";
import type { SeatPresence } from "../protocol";
import { secondsRemaining } from "./presence-countdown";
import { presenceLine } from "./presence-text";

/**
 * The line under the table for the rival's presence, and, while a grace window is running, the
 * whole seconds left of it.
 *
 * The countdown is started once and never acted upon (`ADR-0028` §3). Reaching zero enables no
 * control, sends nothing, marks no hand lost and assumes no resumption: the duel is paused until
 * an `OpponentPresence` says otherwise. This component sends nothing, because it is handed no
 * way to.
 */
export function PresenceNotice(props: {
  presence: SeatPresence | null;
  returned: boolean;
  graceRemainingMillis: number | null;
}): ReactElement {
  // Anchored once, on mount. The parent remounts this component per presence frame
  // (`key={presenceCount}`, `TASK-031309`), so a second window starts a second countdown even
  // when it carries the same remaining as the first.
  const [deadline] = useState(() =>
    props.graceRemainingMillis === null
      ? null
      : Date.now() + props.graceRemainingMillis,
  );
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (deadline === null) return;
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, [deadline]);

  return (
    <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
      {presenceLine(props.presence, props.returned)}
      {deadline !== null && (
        <span className="font-mono tabular-nums">
          {secondsRemaining(deadline, now)}
        </span>
      )}
    </p>
  );
}
