import type { DuelState } from "../store/duel-state";
import type { Screen } from "./screen";

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

/**
 * Does mounting this screen send something the server cannot be asked twice?
 * True for screens that spend a secret — namely `"verify"` and `"reset"`,
 * which carry a mailed token that is consumed on first use.
 *
 * @param asked the screen being asked for
 * @returns true only for mailed screens that arrive with a one-time secret
 */
export function spendsOnArrival(asked: Screen): boolean {
  return asked === "verify" || asked === "reset";
}

/**
 * How the client rules on a screen the player asked for, given the room's standing.
 */
export type Ruling = "honour" | "refuse" | "hold";

/**
 * The ruling on a screen the player asked for.
 *
 * A player already at the first screen is always honoured — `replaceState` must not be called
 * on every arriving frame. A chosen screen is refused if the room is running a duel. For every
 * chosen screen, if the room's standing is unknown (store empty before the first frame), the
 * client holds and waits: it must not spend a mailed token, and it must not break the
 * address-screen agreement by showing the lobby while the address names a chosen screen. Only
 * the mailed screens (`verify` and `reset`) wait; the others render at once. In every other
 * standing the client honours the ask.
 *
 * @param asked the screen being asked for
 * @param standing the room's current standing
 * @returns the ruling: honour, refuse, or hold
 */
export function rulingOn(asked: Screen, standing: RoomStanding): Ruling {
  if (asked === "first") return "honour";
  if (standing === "running") return "refuse";
  if (standing === "unknown") return spendsOnArrival(asked) ? "hold" : "honour";
  return "honour";
}
