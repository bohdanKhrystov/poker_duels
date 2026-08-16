import { within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { driveScriptedDuel } from "./drive-duel";
import { scriptedDuel } from "./scripted-duel";
import { PROTOCOL_VERSION } from "../protocol";

describe("a whole duel through the client", () => {
  it("plays every frame of the script and ends on the result screen", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      let replayed = 0;
      const run = driveScriptedDuel({
        viewerSeat: seat.viewerSeat,
        onStep: () => {
          replayed += 1;
        },
      });

      expect(replayed).toBe(seat.steps.length);
      // The double's own count, not the loop's: a replay that handed a
      // "client" step's frame to `receive` too would still walk every step
      // and could still land on the result screen, since the decoder just
      // drops what it cannot parse as a ServerMessage. Only this catches it.
      expect(run.receivedCount).toBe(
        seat.steps.filter((step) => step.from === "server").length,
      );
      // "getByRole" alone would pass for a client that rendered the result
      // beside the table rather than in place of it; "your move" gone too is
      // what proves the table itself is no longer on screen.
      expect(
        within(run.container).getByRole("region", { name: "the result" }),
      ).toBeDefined();
      expect(within(run.container).queryByLabelText("your move")).toBeNull();
    }
  });

  it("is on the table, not the lobby, while the duel is running", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      let firstSnapshotIndex: number | null = null;
      let midDuel: { createRoom: boolean; potStrip: boolean } | null = null;
      let atEnd: { result: boolean; potStrip: boolean } | null = null;

      driveScriptedDuel({
        viewerSeat: seat.viewerSeat,
        onStep: (step, index, container) => {
          if (
            firstSnapshotIndex === null &&
            step.from === "server" &&
            step.message.type === "Snapshot"
          ) {
            firstSnapshotIndex = index;
          }

          if (firstSnapshotIndex !== null && index === firstSnapshotIndex + 1) {
            midDuel = {
              createRoom:
                within(container).queryByText("Create a duel room") !== null,
              potStrip: within(container).queryByText(/^Pot \d+/) !== null,
            };
          }

          if (index === seat.steps.length - 1) {
            atEnd = {
              result:
                within(container).queryByRole("region", {
                  name: "the result",
                }) !== null,
              potStrip: within(container).queryByText(/^Pot \d+/) !== null,
            };
          }
        },
      });

      // Two observations at two different points: "ends on the result
      // screen" cannot be satisfied by a client that showed the result the
      // whole time, because here it plainly did not — the table was up
      // (pot strip, no lobby text) right after the first Snapshot, and only
      // the result is up by the last step.
      expect(midDuel).toEqual({ createRoom: false, potStrip: true });
      expect(atEnd).toEqual({ result: true, potStrip: false });
    }
  });

  it("sends the handshake and nothing more, because nothing asked it to act", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });

      expect(run.sent.map((frame) => JSON.parse(frame))).toEqual([
        { type: "Hello", deviceId: null, protocolVersion: PROTOCOL_VERSION },
        { type: "JoinRoom", code: duel.roomCode },
      ]);
    }
  });
});
