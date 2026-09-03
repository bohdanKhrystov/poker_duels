import type { ProtocolVersion } from "./protocol.gen";

/**
 * The wire version this client speaks, sent on every `Hello`.
 *
 * Typed against the generated alias on purpose: when the server bumps
 * `PROTOCOL_VERSION`, the alias becomes a different literal and this line stops
 * compiling. That is the whole reason `ADR-0020` emits the alias — a stale
 * version must fail the build, not the handshake.
 */
export const PROTOCOL_VERSION: ProtocolVersion = 6;
