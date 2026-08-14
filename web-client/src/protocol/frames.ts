import type { ClientMessage, ServerMessage } from "./protocol.gen";

// Keyed by discriminator so `satisfies` makes the compiler prove the set is the
// union: a missing key is TS1360, an extra key is TS2353. When `ADR-0028` adds
// `OpponentPresence` and `ActedForAbsentSeat` to `ServerMessage`, `tsc` fails
// here until they are added — which is the cheap, reviewed edit that change wants.
const SERVER_MESSAGE_TABLE = {
  DuelFinished: true,
  Events: true,
  Failure: true,
  Rejected: true,
  RoomJoined: true,
  Snapshot: true,
  Welcome: true,
  YourTurn: true,
} satisfies Record<ServerMessage["type"], true>;

/** Every discriminator this client can decode, sorted. */
export const SERVER_MESSAGE_TYPES: readonly string[] =
  Object.keys(SERVER_MESSAGE_TABLE).sort();

/** One outbound frame, as the server's `protocolJson` will read it. */
export function encodeClientMessage(message: ClientMessage): string {
  return JSON.stringify(message);
}

/** One inbound frame, or `null` if this client cannot read it. */
export function decodeServerMessage(data: unknown): ServerMessage | null {
  if (typeof data !== "string") {
    return null;
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(data);
  } catch {
    return null;
  }

  // `typeof null === "object"` and `Array.isArray` is not implied by the
  // `typeof` check, so both are excluded explicitly alongside every primitive.
  if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) {
    return null;
  }

  const { type } = parsed as Record<string, unknown>;
  if (typeof type !== "string" || !SERVER_MESSAGE_TYPES.includes(type)) {
    return null;
  }

  // The server is authoritative and writes every field (`encodeDefaults = true`,
  // ADR-0020), so narrowing by discriminator is sufficient: a structural
  // validator here would be a second, hand-written mirror of the schema — the
  // exact artefact ADR-0020 exists to prevent.
  return parsed as ServerMessage;
}
