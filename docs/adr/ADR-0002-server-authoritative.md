# ADR-0002 — The server is authoritative

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

The engine is a library and can run anywhere, including compiled to JavaScript or WebAssembly
in the browser. That raises the question of where the *real* game runs.

No money is at stake here, which makes it tempting to be relaxed about it. That temptation
should be resisted: an untrusted client that can assert game facts can claim to hold aces, can
claim to have won, and can see cards it was never dealt. A leaderboard with no integrity is
worth less than no leaderboard, and the failure would be silent.

## Decision

**The server runs the engine and owns the truth.** A client message can express exactly one
thing: *"I intend to take this action."*

The server decides, and the client is told:

- which cards exist and who holds them,
- whose turn it is,
- whether an action is legal,
- the size of every pot,
- who won.

Consequences for the wire protocol:

- Clients receive a **`PlayerView`** — a projection with the opponent's hole cards removed.
  Redaction happens in the engine's projection layer, not by omitting a field at the transport
  boundary.
- The deck and the seed never leave the server while a hand is live. The seed may be published
  after a match ends, so a player can verify the deal was not manipulated.
- Every inbound action is validated against the server's own state. Client-supplied state is
  never trusted, and never merged.
- Actions carry the hand and action sequence number they respond to, so a replayed or
  out-of-order message is detected and dropped rather than applied twice.

The engine remains completely unaware of any of this. It is the server that chooses to run it.

## Consequences

**Gained**

- Cheating requires breaking the server, not the client.
- The client stays thin: render state, send intents. No rules duplicated in TypeScript.
- One implementation of the rules, so client and server cannot drift.

**Cost**

- Every action costs a round trip. For a turn-based game with no reflex component this is
  imperceptible, and it is the correct trade.
- The server holds per-room state in memory, which constrains how it can be scaled later. Not
  a concern at the scale this project targets; the event log makes room recovery possible if it
  ever becomes one.

## Alternatives considered

- **Engine in the browser, server as a relay** — rejected: every client becomes trusted, which
  is the same as trusting nobody's results.
- **Engine in both places, server verifies** — rejected for now. Running the same engine
  client-side for optimistic UI is genuinely attractive and Kotlin/JS or Wasm makes it
  possible, but it is a v2 optimisation, not an MVP requirement. The architecture leaves the
  door open.
