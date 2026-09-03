import type { ReactElement } from "react";
import type { SeatPresence } from "../protocol";
import { presenceLine } from "./presence-text";

/**
 * The line under the table for the rival's presence.
 *
 * Carried a running countdown here until `TASK-130805`: the remaining grace window is no longer
 * on the wire (`ADR-0113`), so this component renders `presenceLine` alone now. `STORY-1309`
 * moves a countdown onto the turn clock instead.
 */
export function PresenceNotice(props: {
  presence: SeatPresence | null;
  returned: boolean;
}): ReactElement {
  return (
    <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
      {presenceLine(props.presence, props.returned)}
    </p>
  );
}
