---
schema: 2
id: TASK-031004
title: A socket the tab has replaced starts no retry of its own
type: task
status: backlog
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol, websocket, reconnect]
depends_on: [TASK-031003]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +293 passed \(293\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'ignores the close of a socket it has already replaced'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts the failures of the live socket, not of the one it replaced'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes to the socket it opened last'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'drops a send made while no socket is open'
  - cd web-client && npm run check
---

## Goal

`ADR-0018` says a second socket adopts the seat and the server closes the first. That close reaches
this tab, and it must not be mistaken for a drop: the retry loop is idempotent per tab and never
fights its own adoption.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/reconnecting.ts` | modify — a generation counter and its guard |
| `web-client/src/protocol/reconnecting.test.ts` | modify — four tests added, none changed |
| `docs/adr/ADR-0018-a-second-socket-adopts-the-seat.md` | read — the rule this pins |

## Scope

- Each attached socket captures the generation it was attached at; a `onclose` whose captured
  generation is no longer the current one returns without touching `live`, `failures` or the timer.
- The generation advances when a socket's close is *accepted*, so the socket that just closed is
  history from that instant — a second `close` event on the same socket is stale too.

  ```ts
  function attach(): void {
    const mine = generation;
    const socket = options.openSocket();
    current = openConnection({ socket, storage: options.storage, onMessage: forward });
    live = true;
    socket.onclose = (): void => {
      // A socket this tab has already replaced. ADR-0018 has the server close
      // the first socket when the second adopts the seat, so this close is
      // this tab, one attempt earlier — not a drop, and not a reason to retry.
      if (mine !== generation) return;
      generation += 1;
      live = false;
      scheduleRetry();
    };
  }
  ```

- Nothing else moves. The delays, the `Welcome` reset and the forwarding are `TASK-031003`'s and
  stay byte-identical.

## Out of scope

- Ending the loop on a version mismatch — `TASK-031005`.
- Closing the *old* socket ourselves. The server does that (`ADR-0018`); a client that closed it
  would be racing the adoption it is trying not to fight.
- Anything about two browser tabs. `ADR-0018` is explicit that two tabs on one device id will take
  the seat from each other and that this is accepted in v0.1. This ticket makes one tab idempotent,
  not two tabs cooperative.

## Tests

`web-client/src/protocol/reconnecting.test.ts`, same describe block, same fixture, same virtual
time. `jitter` stays `() => 0`.

| Test | Proves |
| --- | --- |
| `ignores the close of a socket it has already replaced` | close socket 1, advance 250 ms so socket 2 exists, then fire socket 1's `onclose` again and advance an hour → still exactly **two** sockets |
| `counts the failures of the live socket, not of the one it replaced` | after that stale close, closing socket 2 opens socket 3 at **500** ms (the second failure), not at 1000 ms — a guard that returned early *without* leaving `failures` alone would pass the test above and fail this one |
| `writes to the socket it opened last` | after a reconnect, `connection.send({ type: "CreateRoom" })` appears in socket 2's `sent` and socket 1's `sent` gains nothing beyond its own `Hello` |
| `drops a send made while no socket is open` | between socket 1's close and socket 2's arrival, `connection.send({ type: "CreateRoom" })` throws nothing, and once socket 2 is open its `sent` holds only its `Hello` — the frame was dropped, not queued |

```ts
it("ignores the close of a socket it has already replaced", () => {
  const { sockets } = openOverFakeSockets();

  sockets[0].close();
  vi.advanceTimersByTime(250);
  expect(sockets).toHaveLength(2);

  // ADR-0018: the server closes the socket the new one adopted the seat from.
  sockets[0].close();
  vi.advanceTimersByTime(60_000);

  expect(sockets).toHaveLength(2);
});
```

Four tests added, none of the five changed. Two hundred and eighty-nine exist, so the suite reports
**293**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 293 passed (293)` | four ran, the five before them still do, and nothing else moved |
| the four `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks and lints |

**Name the edit that makes each assertion red:**

1. Delete the `if (mine !== generation) return;` line → `ignores the close of a socket it has
   already replaced` fails with three sockets. Revert.
2. Keep the guard but move `generation += 1` into `attach()` → `counts the failures of the live
   socket, not of the one it replaced` fails, because the stale close is accepted and spends an
   attempt. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the reconnecting connection > ignores the close of a socket it has already replaced` passes
- [ ] `the reconnecting connection > counts the failures of the live socket, not of the one it replaced` passes
- [ ] `the reconnecting connection > writes to the socket it opened last` passes
- [ ] `the reconnecting connection > drops a send made while no socket is open` passes
- [ ] The five tests `TASK-031003` added are byte-identical, and no assertion in them is weakened
- [ ] `reconnecting.test.ts` still contains no `await new Promise`
- [ ] `npm run --silent test` reports `Tests  293 passed (293)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
