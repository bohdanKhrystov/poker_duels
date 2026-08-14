/**
 * A `WebSocket` double: no network, no port, no timer. A test calls `open`,
 * `receive` and `close` to move it, and reads `sent` to see what the client
 * wrote. `STORY-0301` set the precedent that no client test reaches the
 * network and `EPIC-03` keeps it.
 */
export class FakeSocket {
  readonly sent: string[] = [];
  closed = false;

  onopen: (() => void) | null = null;
  onmessage: ((event: { data: unknown }) => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;

  send(data: string): void {
    this.sent.push(data);
  }

  close(): void {
    this.closed = true;
    if (this.onclose) {
      this.onclose();
    }
  }

  /** Drive it: the socket finished connecting. */
  open(): void {
    if (this.onopen) {
      this.onopen();
    }
  }

  /** Drive it: the server sent this frame. */
  receive(data: unknown): void {
    if (this.onmessage) {
      this.onmessage({ data });
    }
  }

  /** The one cast in this module, at the seam where the double stands in. */
  asWebSocket(): WebSocket {
    return this as unknown as WebSocket;
  }
}
