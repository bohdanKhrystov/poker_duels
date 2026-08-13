---
id: STORY-0206
title: Rooms, join links and rematch
type: story
status: ready
parent: EPIC-02
module: poker-server
labels: [server, rooms, matchmaking]
depends_on: [STORY-0201]
---

## Goal

One player creates a room and gets a code. The second joins with it. The room seats exactly two,
never three, and when a duel finishes either player can offer a rematch. A room that nobody is
using goes away.

## Why

This is `docs/vision.md`'s first success condition in one component: *"send a link, she opens it in
a browser"*. Matchmaking in v0.1 is a link, not a queue — the roadmap puts queued matchmaking under
*later*, and a queue with two users is a worse experience than a link anyway.

It is deliberately written as a plain component over player identities, knowing nothing about JSON
or sockets, so that it can be built **in parallel with the whole protocol chain** and tested
without a server running.

## Design notes

- `RoomRegistry` is an in-memory component over `PlayerId`s: create, join by code, look up, close.
  No `ClientMessage` and no `ServerMessage` crosses its boundary — `STORY-0207` does that wiring.
  That is what keeps this story independent of `STORY-0202` and `STORY-0205`.
- Room lifecycle: `WAITING` → `PLAYING` → `FINISHED`, plus `ABANDONED`. A join into anything but
  `WAITING` is refused; a third joiner is refused with room-full. Heads-up is two players, never
  three (`docs/vision.md`), and this is the component that makes that structurally true.
- The code is short and human-typable — it goes in a link and possibly gets read aloud — and drawn
  from an **injected** `SecureRandom`-backed source. Never from the engine's `Rng`: engine
  randomness is reproducible by design, which is precisely the wrong property for a room code.
- **Single-writer rooms.** Each room is an actor with a mailbox (a coroutine consuming a channel),
  so no two frames ever mutate one room concurrently. Every race this epic could otherwise ship —
  double join, act-during-rematch, close-during-deal — is closed here by construction rather than
  by locking at each call site.
- Rematch: a `FINISHED` room accepts one offer per seat; when both have offered the room returns to
  `PLAYING` with a fresh `MatchState` and the button on the other seat. One offer alone changes
  nothing and expires with the room. Whether the *duel* actually restarts is `STORY-0207`'s job —
  this story owns the agreement, not the deal.
- Rooms are **not durable**. `ADR-0011` states in-flight duel state need not survive a restart, and
  a room is in-flight duel state. A restart loses rooms and that is the accepted answer.
- Cleanup: an abandoned or finished room is reaped after a bounded idle time, from `ServerConfig`
  and not a literal. `ADR-0013`'s "hold indefinitely" alternative was rejected partly because
  abandoned rooms accumulate with nothing to clean them up — this is the something.
- Time comes from the same injected source `STORY-0208` uses, so reaping is testable on virtual
  time. If that abstraction does not exist yet, this story declares it; `STORY-0208` is its bigger
  consumer.

## Open decisions

**DEC-012 — is holding the code enough to take the seat?** A room code is an invite: whoever
presents it sits down. The mechanism this story ships is 40 bits of `SecureRandom` in eight
Crockford-base32 characters, which makes guessing a *particular* room hopeless, but nothing here
rate-limits a client that tries codes in a loop, and nothing asks the host to confirm the face that
turns up. Registered in [`docs/adr/README.md`](../../docs/adr/README.md). Nothing is blocked on it:
the entropy is a constant in `RoomCode`, and rate limiting or host confirmation would be additive,
landing in `STORY-0207` or later.

**DEC-013 — per-room mutex, or a channel-fed actor?** The design notes above call for an actor with
a mailbox. What `TASK-020609` builds is a `ConcurrentHashMap<RoomCode, Holder>` where each holder
carries its own `Mutex`, and every mutation is read-decide-write inside that lock. That gives the
same property the actor was wanted for — no two callers ever mutate one room concurrently, proved
by `TASK-020610`'s hundred racing joiners — with suspending methods, so `STORY-0207` can run
engine work and I/O inside the same critical section without changing a signature. What it does
*not* give is a queue with an order: if `STORY-0207` needs frames for one room processed in arrival
order, or a single place to fan events out from, the holder becomes an actor. Registered in
[`docs/adr/README.md`](../../docs/adr/README.md); nothing in this story is blocked on it, because
the mutex is a strict subset of what an actor would offer behind the same API.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-020601](../tasks/TASK-020601-server-clock.md) | Declare the injectable ServerClock and a test clock that never sleeps | ready |
| [TASK-020602](../tasks/TASK-020602-room-code-type.md) | A RoomCode value type that only accepts a human-typable code | backlog |
| [TASK-020603](../tasks/TASK-020603-room-code-source.md) | Mint room codes from an injected secure source, never from the engine Rng | backlog |
| [TASK-020604](../tasks/TASK-020604-room-state.md) | The Room value and its four states, with the seating invariants in the type | backlog |
| [TASK-020605](../tasks/TASK-020605-room-join.md) | Seat the second player, and refuse the third | backlog |
| [TASK-020606](../tasks/TASK-020606-room-finish-and-abandon.md) | Finish, abandon and touch a room | backlog |
| [TASK-020607](../tasks/TASK-020607-room-rematch.md) | Both seats must offer before a rematch starts, and the button changes sides | backlog |
| [TASK-020608](../tasks/TASK-020608-room-timeouts.md) | RoomTimeouts, the two idle limits a room is reaped against | backlog |
| [TASK-020609](../tasks/TASK-020609-room-registry-create.md) | A RoomRegistry that creates a room under a code nobody else holds | backlog |
| [TASK-020610](../tasks/TASK-020610-room-registry-join.md) | Join by code under the room's lock, so a hundred racing joiners seat exactly one | backlog |
| [TASK-020611](../tasks/TASK-020611-room-registry-lifecycle.md) | Finish, abandon and offer a rematch through the registry | backlog |
| [TASK-020612](../tasks/TASK-020612-reap-idle-rooms.md) | Reap idle rooms on the injected clock, and never a room that is playing | backlog |
| [TASK-020613](../tasks/TASK-020613-room-timeouts-in-server-config.md) | Read the room idle limits from ServerConfig instead of a literal | backlog |
| [TASK-020615](../tasks/TASK-020615-room-registry-finish-is-called-by-nobody.md) | RoomRegistry.finish is called by no production code — remove it or say why it stays | backlog |

Nothing in this story touches `DuelSocket.kt`, `Application.kt` or the protocol package: the
registry is reached only from its own tests until `STORY-0207` wires it.

## Acceptance criteria

- [ ] Creating a room yields a code; joining with that code seats the guest and moves the room to
      `PLAYING`; a third join is refused with room-full and the room is unchanged.
- [ ] Joining an unknown, finished or abandoned code is refused with room-not-found, and creates
      nothing.
- [ ] The host cannot join their own room as the guest.
- [ ] Both seats offering a rematch returns the room to `PLAYING` with the button on the other
      seat; one offer alone leaves it `FINISHED`.
- [ ] 100 concurrent joins against one `WAITING` room admit exactly one guest, asserted by a
      concurrency test.
- [ ] Room codes are drawn from an injected secure source and produce no duplicate in 100 000
      draws; a test asserts the engine `Rng` is not used.
- [ ] A room idle past the configured limit is reaped, asserted on virtual time with no
      `Thread.sleep`.

## Out of scope

- Any socket message that creates or joins a room — `STORY-0207` wires the registry to the
  protocol.
- Running the duel inside the room — `STORY-0207`.
- Reconnecting into a room after a drop — `STORY-0208`.
- A matchmaking queue, ranked pairing, or public room lists — `EPIC-05` and later.
- Persisting rooms — deliberately never, per `ADR-0011`.
