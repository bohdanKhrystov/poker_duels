import type { DuelState } from "../store/duel-state";

/**
 * The room this tab holds, read off frames the server sent. Never off the wire `RoomState`,
 * which is a server-side enum and appears in no frame and in no client type: `outcome`, `view`,
 * `roomCode` and `refusal` are set by `DuelFinished`, `Snapshot`, `RoomJoined` and `Failure`
 * and by nothing else.
 */
export type RoomStanding =
  "unknown" | "none" | "waiting" | "running" | "finished";

/**
 * What the room this tab holds is doing, using only facts the server sent.
 *
 * The order is the decision, not a style — the reducer clears nothing a frame established, so
 * `view` and `roomCode` both outlive the duel. Testing `finished` after `running` would make
 * `finished` unreachable. `running` is `ADR-0105` §2's "running means `PLAYING`": the grace
 * window and a mid-paint runout are both inside it, since `ADR-0102`'s paint holds the queued
 * `DuelFinished` behind its steps.
 *
 * @param state the reducer's current state
 * @param roomAwaited whether this tab is awaiting a room; distinguishes `unknown` from `none`
 * @returns which of the five standings the room presently holds
 */
export function roomStanding(
  state: DuelState,
  roomAwaited: boolean,
): RoomStanding {
  if (state.outcome !== null) return "finished";
  if (state.view !== null) return "running";
  if (state.roomCode !== null) return "waiting";
  return roomAwaited && state.refusal === null ? "unknown" : "none";
}
