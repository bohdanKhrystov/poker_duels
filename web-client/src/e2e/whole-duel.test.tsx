import { within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { driveScriptedDuel } from "./drive-duel";
import { scriptedDuel } from "./scripted-duel";
import type { ServerStep } from "./scripted-duel";
import { PROTOCOL_VERSION } from "../protocol";
import { formatChips } from "../table/chips";
import type { DuelFinished } from "../protocol/protocol.gen";

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

  it("sends one Act for each YourTurn, and the frame the server recorded", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      // A witness a driver cannot fake by pushing the recorded frame straight
      // into `sent` instead of clicking: only a real click sets the real
      // `ActionBar`'s own `sent` state, which disables every button on it
      // (`TASK-030707`) until the next turn's key remounts it live again
      // (`TASK-031013`). Content, order and count below would all still hold
      // for that bypass — this would not, so it is captured independently of
      // them, keyed by step index rather than folded into the assertions
      // that already pass either way.
      const barState: Array<{ hasButtons: boolean; allDisabled: boolean }> = [];

      const run = driveScriptedDuel({
        viewerSeat: seat.viewerSeat,
        onStep: (_step, index, container) => {
          // Nullable: the lobby's own early steps, before the table (and so
          // the bar) ever mounts, are never among the indices read below —
          // but `onStep` still fires for them, and a `get*` here would throw.
          const bar = within(container).queryByLabelText("your move");
          const buttons =
            bar === null ? [] : within(bar).queryAllByRole("button");
          barState[index] = {
            hasButtons: buttons.length > 0,
            allDisabled:
              buttons.length > 0 &&
              buttons.every((button) => button.hasAttribute("disabled")),
          };
        },
      });

      // Independent of the recorded `"client"` steps below: if a turn ever
      // produced two `Act`s (or none), this count would drift from the
      // server's own `YourTurn`s even if the driver still walked every step.
      const yourTurnCount = seat.steps.filter(
        (step) => step.from === "server" && step.message.type === "YourTurn",
      ).length;
      const recordedActs = seat.steps
        .filter((step) => step.from === "client")
        .map((step) => JSON.parse(step.frame));
      const actsSent = run.sent
        .map((frame) => JSON.parse(frame))
        .filter((message) => message.type === "Act");

      expect(actsSent).toHaveLength(yourTurnCount);
      // The oracle is the server's own recording, not a frame rebuilt here —
      // and `toEqual` on two arrays checks position as well as content, so
      // this is the "in order, one for one" claim too.
      expect(actsSent).toEqual(recordedActs);

      seat.steps.forEach((step, index) => {
        if (step.from !== "client") return;

        // Right after this click, the bar it clicked on is quiet.
        expect(barState[index]).toEqual({
          hasButtons: true,
          allDisabled: true,
        });

        // And live again exactly when this seat is next asked — if it is
        // asked again at all; the seat's last action in the run has no next
        // turn to check.
        const nextYourTurn = seat.steps.findIndex(
          (later, laterIndex) =>
            laterIndex > index &&
            later.from === "server" &&
            later.message.type === "YourTurn",
        );
        if (nextYourTurn !== -1) {
          expect(barState[nextYourTurn]).toEqual({
            hasButtons: true,
            allDisabled: false,
          });
        }
      });
    }
  });

  it("echoes each turn's own handNumber and actionSequence, and its own seat", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });

      const yourTurns = seat.steps
        .filter((step) => step.from === "server")
        .map((step) => JSON.parse(step.frame))
        .filter((message) => message.type === "YourTurn");
      const acts = run.sent
        .map((frame) => JSON.parse(frame))
        .filter((message) => message.type === "Act");

      // One Act per YourTurn (proved on its own above) is what makes the
      // i-th Act the answer to the i-th YourTurn.
      expect(acts).toHaveLength(yourTurns.length);
      acts.forEach((act, i) => {
        expect(act.handNumber).toBe(yourTurns[i].handNumber);
        expect(act.actionSequence).toBe(yourTurns[i].actionSequence);
        expect(act.action.seat).toBe(seat.viewerSeat);
      });

      // A client that copied one identity everywhere would still satisfy
      // every check above on a duel this short unless the run really does
      // cross hands and turns. The recorded script gives each seat as few as
      // two distinct `actionSequence`s across the whole run, so ">1" is the
      // strongest claim true of both seats — still enough to catch a client
      // that sent the same `actionSequence` for every turn.
      expect(new Set(acts.map((act) => act.handNumber)).size).toBeGreaterThan(
        1,
      );
      expect(
        new Set(acts.map((act) => act.actionSequence)).size,
      ).toBeGreaterThan(1);
    }
  });

  it("sends the handshake before it acts, and never after the duel ends", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });
      const sent = run.sent.map((frame) => JSON.parse(frame));

      expect(sent[0]).toEqual({
        type: "Hello",
        deviceId: null,
        protocolVersion: PROTOCOL_VERSION,
        sessionToken: null,
      });
      expect(sent[1]).toEqual({ type: "JoinRoom", code: duel.roomCode });

      const clientStepCount = seat.steps.filter(
        (step) => step.from === "client",
      ).length;
      // Nothing sent before the handshake's two frames, and nothing sent
      // after the duel ends or twice for the same turn: the total is bounded
      // exactly, not just "at least".
      expect(sent).toHaveLength(2 + clientStepCount);
    }
  });

  it("feeds the server's steps to the client in the order the script recorded them", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });

      const scriptedServerFrames = seat.steps
        .filter((step) => step.from === "server")
        .map((step) => step.frame);

      expect(run.receivedFrames).toEqual(scriptedServerFrames);
    }
  });

  it("states the hand count and every final stack the last frame carried", () => {
    const duel = scriptedDuel();

    for (const seat of duel.seats) {
      // Get the last server step which should be DuelFinished
      const lastServerStep = [...seat.steps]
        .reverse()
        .find((step) => step.from === "server") as ServerStep | undefined;
      expect(lastServerStep).toBeDefined();
      expect(lastServerStep?.from).toBe("server");

      const lastMessage = lastServerStep!.message as DuelFinished;
      expect(lastMessage.type).toBe("DuelFinished");

      const { outcome } = lastMessage;
      const { handsPlayed, finalStacks } = outcome;

      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });
      const resultText = within(run.container).getByRole("region", {
        name: "the result",
      }).textContent;

      // Should contain the correct hand count
      const handWord = handsPlayed === 1 ? "hand" : "hands";
      expect(resultText).toContain(`${handsPlayed} ${handWord}`);

      // Should contain the correct final stacks with the correct labels
      expect(resultText).toContain(
        `You ${formatChips(finalStacks[seat.viewerSeat])}`,
      );
      expect(resultText).toContain(
        `Your rival ${formatChips(finalStacks[1 - seat.viewerSeat])}`,
      );

      // Should NOT contain incorrect hand count
      expect(resultText).not.toContain(`${handsPlayed + 1} hand`);

      // Should NOT contain swapped stack labels
      expect(resultText).not.toContain(
        `You ${formatChips(finalStacks[1 - seat.viewerSeat])}`,
      );
    }
  });

  it("gives the two seats opposite verdicts, off the winner and the seat alone", () => {
    const duel = scriptedDuel();
    const verdictsByRun: string[] = [];

    for (const seat of duel.seats) {
      // Get the last server step which should be DuelFinished
      const lastServerStep = [...seat.steps]
        .reverse()
        .find((step) => step.from === "server") as ServerStep | undefined;
      expect(lastServerStep).toBeDefined();

      const lastMessage = lastServerStep!.message as DuelFinished;
      expect(lastMessage.type).toBe("DuelFinished");

      const { outcome } = lastMessage;
      const { winner } = outcome;

      const run = driveScriptedDuel({ viewerSeat: seat.viewerSeat });
      const resultText = within(run.container).getByRole("region", {
        name: "the result",
      }).textContent;

      // Determine expected verdict based on winner and seat
      const isWinner = winner === seat.viewerSeat;

      if (isWinner) {
        expect(resultText).toContain("Victory");
        expect(resultText).toContain("+1 duel coin");
        verdictsByRun.push("Victory");
      } else {
        expect(resultText).toContain("Defeat");
        expect(resultText).toContain("−1 duel coin");
        verdictsByRun.push("Defeat");
      }
    }

    // Verify that each verdict appears in exactly one of the two runs
    const victoryCount = verdictsByRun.filter((v) => v === "Victory").length;
    const defeatCount = verdictsByRun.filter((v) => v === "Defeat").length;

    expect(victoryCount).toBe(1);
    expect(defeatCount).toBe(1);
  });
});
