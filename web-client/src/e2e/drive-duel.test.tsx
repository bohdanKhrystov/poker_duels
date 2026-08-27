import { describe, expect, it } from "vitest";
import { DEVICE_ID_STORAGE_KEY } from "../protocol/device-id";
import { driveScriptedDuel, inMemoryStorage } from "./drive-duel";
import { scriptedDuel } from "./scripted-duel";

describe("driveScriptedDuel storage", () => {
  it("writes the seat own device id into the storage it was handed", () => {
    const callerStorage = inMemoryStorage();

    const run = driveScriptedDuel({
      viewerSeat: 0,
      storage: callerStorage,
    });

    expect(run.storage).toBe(callerStorage);
    const deviceId = callerStorage.getItem(DEVICE_ID_STORAGE_KEY);
    expect(deviceId).toBe("device-seat-0");
  });

  it("two seats driven into two storages hold two different device ids", () => {
    // Drive seat 0
    const storage0 = inMemoryStorage();
    driveScriptedDuel({
      viewerSeat: 0,
      storage: storage0,
    });

    // Drive seat 1
    const storage1 = inMemoryStorage();
    driveScriptedDuel({
      viewerSeat: 1,
      storage: storage1,
    });

    const deviceId0 = storage0.getItem(DEVICE_ID_STORAGE_KEY);
    const deviceId1 = storage1.getItem(DEVICE_ID_STORAGE_KEY);

    // Both should have their respective ids
    expect(deviceId0).toBe("device-seat-0");
    expect(deviceId1).toBe("device-seat-1");

    // And they should be different
    expect(deviceId0).not.toBe(deviceId1);
  });

  it("a run given no storage still plays the whole script", () => {
    const duel = scriptedDuel();
    const seat0 = duel.seats.find((s) => s.viewerSeat === 0);
    if (!seat0) {
      throw new Error("No seat 0 in script");
    }

    const run = driveScriptedDuel({
      viewerSeat: 0,
    });

    // The run should have a storage (created internally)
    expect(run.storage).toBeDefined();

    // The storage should contain the device id
    const deviceId = run.storage.getItem(DEVICE_ID_STORAGE_KEY);
    expect(deviceId).toBe("device-seat-0");

    // The received count should match the number of server steps
    const serverStepCount = seat0.steps.filter(
      (step) => step.from === "server",
    ).length;
    expect(run.receivedCount).toBe(serverStepCount);
  });
});
