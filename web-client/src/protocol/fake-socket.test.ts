import { describe, it, expect } from "vitest";
import { FakeSocket } from "./fake-socket";

describe("the fake socket", () => {
  it("records every frame the client sent", () => {
    const socket = new FakeSocket();
    socket.send("a");
    socket.send("b");
    expect(socket.sent).toEqual(["a", "b"]);
  });

  it("hands a frame to the message handler", () => {
    const socket = new FakeSocket();
    const events: { data: unknown }[] = [];
    socket.onmessage = (event) => {
      events.push(event);
    };
    socket.receive("{}");
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ data: "{}" });
  });

  it("runs the open handler when it is opened", () => {
    const socket = new FakeSocket();
    let called = false;
    socket.onopen = () => {
      called = true;
    };
    socket.open();
    expect(called).toBe(true);
  });

  it("marks itself closed and tells the close handler", () => {
    const socket = new FakeSocket();
    let called = false;
    socket.onclose = () => {
      called = true;
    };
    socket.close();
    expect(socket.closed).toBe(true);
    expect(called).toBe(true);
  });

  it("opens without a handler and does nothing", () => {
    const socket = new FakeSocket();
    expect(() => socket.open()).not.toThrow();
  });

  it("receives without a handler and does nothing", () => {
    const socket = new FakeSocket();
    expect(() => socket.receive("{}")).not.toThrow();
    expect(socket.sent).toEqual([]);
  });
});
