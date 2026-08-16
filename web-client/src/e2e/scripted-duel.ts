import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { decodeServerMessage } from "../protocol/frames";
import type { Act, ServerMessage } from "../protocol/protocol.gen";

/** The rival's real hole cards for one hand, as the script's generator recorded them. */
export interface RivalHand {
  readonly handNumber: number;
  readonly cards: readonly string[];
}

/** One inbound frame, decoded by the same function the live socket uses. */
export interface ServerStep {
  readonly from: "server";
  readonly frame: string;
  readonly message: ServerMessage;
}

/** One outbound frame, parsed and typed with a single cast — see `decodeStep` below. */
export interface ClientStep {
  readonly from: "client";
  readonly frame: string;
  readonly act: Act;
}

export type ScriptStep = ServerStep | ClientStep;

/** One seat's half of the recorded duel: what it saw, and what it sent. */
export interface ScriptedSeat {
  readonly viewerSeat: number;
  readonly steps: readonly ScriptStep[];
  readonly rivalHoleCards: readonly RivalHand[];
}

/** The whole committed script, both seats. */
export interface ScriptedDuel {
  readonly roomCode: string;
  readonly seats: readonly ScriptedSeat[];
}

// The shape `scripted-duel.gen.json` is committed in: every step is still a raw
// wire string, undecoded. This module — not the generator — is the one place
// that runs each `frame` through the client's own decoder.
interface RawStep {
  readonly from: string;
  readonly frame: string;
}

interface RawSeat {
  readonly viewerSeat: number;
  readonly steps: readonly RawStep[];
  readonly rivalHoleCards: readonly RivalHand[];
}

interface RawScript {
  readonly roomCode: string;
  readonly seats: readonly RawSeat[];
}

// Resolved off this module's own URL, exactly as `boundary.ts` reaches
// `protocol.gen.ts` beside itself — not a JSON import, which would make `tsc`
// infer a literal type for a file of hundreds of thousands of characters, and
// would widen every `"Snapshot"` to `string` anyway.
function fixturePath(): string {
  return join(
    dirname(fileURLToPath(import.meta.url)),
    "scripted-duel.gen.json",
  );
}

function decodeStep(
  viewerSeat: number,
  index: number,
  raw: RawStep,
): ScriptStep {
  if (raw.from === "server") {
    const message = decodeServerMessage(raw.frame);
    if (message === null) {
      throw new Error(
        `scripted-duel: seat ${viewerSeat} step ${index} carries a server frame this client cannot decode: ${raw.frame}`,
      );
    }
    return { from: "server", frame: raw.frame, message };
  }

  if (raw.from === "client") {
    // `verifyDuelScript` (TASK-031203) gates this file against the server's own
    // message descriptors before it is committed, so a hand-written structural
    // validator here would be a second copy of the schema — the same reasoning
    // `decodeServerMessage` applies to every server frame.
    const act = JSON.parse(raw.frame) as Act;
    return { from: "client", frame: raw.frame, act };
  }

  throw new Error(
    `scripted-duel: seat ${viewerSeat} step ${index} has an unknown "from": ${raw.from}`,
  );
}

function decodeSeat(raw: RawSeat): ScriptedSeat {
  return {
    viewerSeat: raw.viewerSeat,
    steps: raw.steps.map((step, index) =>
      decodeStep(raw.viewerSeat, index, step),
    ),
    rivalHoleCards: raw.rivalHoleCards,
  };
}

/**
 * The committed two-seat duel script (`scripted-duel.gen.json`), decoded through
 * the client's own wire decoder rather than trusted as-is.
 *
 * See `scripted-duel.test.ts` for the guard that this really is a whole duel —
 * every later fixture-driven test leans on that guard, not on this file alone.
 */
export function scriptedDuel(): ScriptedDuel {
  const raw = JSON.parse(readFileSync(fixturePath(), "utf-8")) as RawScript;
  return {
    roomCode: raw.roomCode,
    seats: raw.seats.map(decodeSeat),
  };
}
