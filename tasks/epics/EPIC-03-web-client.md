---
id: EPIC-03
title: Web client
type: epic
status: ready
module: web-client
labels: [client, react, typescript, ui]
---

## Goal

A browser client that turns the duel server into a game two people can actually play. It opens a
socket, completes the handshake, creates a room and hands out a link, joins one from a pasted code,
renders the table it is told about, offers exactly the actions the server says are legal, shows who
won, survives a reload, and reads back the coin.

When this epic closes, the vision's success condition is met by hand — *send a link, she opens it in
a browser, we play a full heads-up match, someone wins* — and one automated test drives the client
through a whole duel of real server frames and proves that nothing an opponent held ever reached the
screen.

## Why now

`EPIC-02` is done. The server plays a complete duel over `/ws`, `GET /api/me` and
`GET /api/me/duels` answer, and the coin is written and readable. It is a game nobody can see. Every
line of `docs/vision.md`'s first success condition that is still missing is missing in the browser.

The two inputs this epic waited for have both landed:
`web-client/src/protocol/protocol.gen.ts` is committed, generated from the Kotlin serial descriptors
and typechecked under `tsc --strict` in CI ([`ADR-0020`](../../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md)),
and `design/tokens/tokens.css` is merged, so the client composes 57 named values instead of
inventing colours ([`ADR-0024`](../../docs/adr/ADR-0024-design-follows-the-code-workflow.md)).

## Scope

- The `web-client` toolchain: package manifest, lockfile, build, typecheck, lint, test, and the CI
  step that runs them — replacing today's ad-hoc `npx tsc` on the generated protocol file.
- The design tokens wired into the client's styling layer, structurally, so no screen can invent a
  colour.
- The socket client: handshake, device identity, and the only place in the codebase that touches a
  raw frame.
- The store: `ServerMessage`s folded into what the screens render, with the last `Snapshot` as the
  single truth.
- The lobby: create a room, show the code and a shareable link, join by code or by link, wait for
  the opponent.
- The duel table: a rendered `PlayerView`. Board, pot, stacks, blinds, button, your cards face-up,
  the opponent's face-down until the server reveals them.
- The action bar: `YourTurn.legalActions` rendered as buttons, `Act` echoing the turn's identity,
  `Rejected` shown as sent.
- The result screen: `DuelOutcome` — win, loss, or the draw that `ADR-0015` says is a real result.
- Rematch, once the wire can carry one (`DEC-023`).
- Reconnect: a dropped socket or a reloaded tab returns to the same seat.
- The profile strip: the coin balance and the recent duels, read over HTTP, signed and unclamped.
- One test that plays a whole scripted duel through the client and asserts the secrecy property from
  the client side.

## Out of scope

| Not here | Where |
| --- | --- |
| Any change to the server, the protocol, or the rules | Nowhere in this epic. A client that needs a new frame raises a decision (`DEC-023`) rather than editing Kotlin |
| Deciding the visual language — palette, type, the table's composition | EPIC-06. This epic consumes `design/tokens/tokens.css` and the screen designs; it authors none |
| Accounts, the claim flow, display names | EPIC-04. No name is on the wire today, so no name is rendered — see `DEC-017` |
| Leaderboard, seasons, ratings | EPIC-05 |
| Replay viewer, equity, decision quality | EPIC-08 |
| Hosting, TLS, serving the built assets in production, Docker | EPIC-07. This epic builds a bundle and runs a dev server; delivering it is not its problem |
| A bot driving the client | EPIC-09 |
| Spectating | `DEC-009`, unanswered, and no `PlayerView` exists for a third party |
| What a player is shown while the opponent is away | `DEC-018`, the human's, unanswered. Nothing in this epic renders a pause state |
| Sound, chip animation, celebration | Later. The event log lands in the store here and drives nothing yet |
| Internationalisation, an accessibility audit, a native mobile app | Later. The client is English, dark, and follows whatever responsive layout EPIC-06's designs give it |

## Non-negotiables this epic is most likely to break

Every one of these is cheap to violate in a component and expensive to notice in a browser:

- **The client asserts no game fact.** It computes no legality, no pot, no min-raise, no winner, no
  hand rank. `LegalActions`, `PlayerView` and `DuelOutcome` exist precisely so it does not have to.
  A helper named `canCheck` or `isBetLegal` is a defect, not a convenience.
- **The last `Snapshot` is the truth.** The server sends one to each seat after every transition and
  its own code calls it *"the authoritative last word on state"*. `Events` are narration — they may
  animate and they may log, and they may never move a chip. A reducer that rebuilds state from
  events has re-implemented the rules in TypeScript.
- **No protocol type is ever hand-written.** Every wire shape comes from `protocol.gen.ts`
  (`ADR-0020`). The one stated exception is the two plain-HTTP endpoints, which are not
  `ServerMessage`s and are contracted in `docs/protocol.md` — declared once, in the HTTP module, and
  nowhere else.
- **The generated file is untouchable.** No formatter, linter, codemod or import-sorter may rewrite
  a byte of `web-client/src/protocol/protocol.gen.ts`; `./gradlew :poker-server:verifyProtocolTypes`
  byte-compares it on every `check` and a prettier run would fail the Kotlin build.
- **Cards the client was not sent do not exist.** An empty `SeatView.holeCards` is rendered as a
  card back and stored as nothing. The client never holds a card it has not been given, never
  guesses one, and has nowhere to leak one from.
- **Nothing is applied optimistically.** A click sends an intent and waits. Chips move when a
  `Snapshot` says they moved.

## Stories

| ID | Title | Depends on | Status |
| --- | --- | --- | --- |
| [STORY-0301](../stories/STORY-0301-web-client-toolchain.md) | The web-client toolchain and its first green check | — | blocked (`DEC-022`) |
| [STORY-0302](../stories/STORY-0302-design-tokens-in-the-client.md) | The design tokens are the client's only colours and sizes | 0301 | backlog |
| [STORY-0303](../stories/STORY-0303-typed-socket-and-handshake.md) | The typed socket: handshake and device identity | 0301 | backlog |
| [STORY-0304](../stories/STORY-0304-client-store.md) | The store: state is the last frame the server sent | 0303 | backlog |
| [STORY-0305](../stories/STORY-0305-lobby-and-room-link.md) | The lobby: create a room, join by code, share the link | 0302, 0304 | backlog |
| [STORY-0306](../stories/STORY-0306-duel-table-screen.md) | The duel table renders a `PlayerView` | 0305 | backlog |
| [STORY-0307](../stories/STORY-0307-action-bar.md) | The action bar: acting on your turn | 0306 | backlog |
| [STORY-0308](../stories/STORY-0308-result-screen.md) | The result screen: who won, and the coin | 0307 | backlog |
| [STORY-0309](../stories/STORY-0309-rematch.md) | Rematch from the result screen | 0308 | blocked (`DEC-023`) |
| [STORY-0310](../stories/STORY-0310-reconnect-and-resume.md) | Reconnect: the client resumes its seat | 0306 | backlog |
| [STORY-0311](../stories/STORY-0311-profile-strip.md) | The profile strip: my coins and my recent duels | 0302, 0303 | backlog |
| [STORY-0312](../stories/STORY-0312-whole-duel-through-the-client.md) | A whole duel through the client, frame by frame | 0308, 0310 | backlog |

## What can run in parallel

The chain here is longer than `EPIC-02`'s because screens compose. These are the branches that are
genuinely independent — no shared file, no shared type:

- **Two branches off the scaffold.** Once `STORY-0301` merges, `STORY-0302` (styling layer) and
  `STORY-0303` (socket) share nothing: one touches CSS and the build's theme, the other touches
  `src/protocol/` consumers and storage. They meet at the lobby.
- **`STORY-0311` is the other root.** The profile strip speaks HTTP, not the socket. After `0302`
  and `0303` it needs nothing from the store, the lobby or the table — only the device-id key that
  `0303` owns. It can be worked at any point in the second half of the epic.
- **`STORY-0310` (reconnect) forks after the table.** It needs a table to repaint and a stored room
  code, but nothing from the action bar or the result screen, so it runs alongside `0307`/`0308`.

And the honest non-parallelism, recorded so nobody tries to break it:

- `0305 → 0306 → 0307 → 0308` is a real chain, not a queue: each adds to the same screen shell and
  the same store selectors, and two of them open at once would conflict on every ticket.
- Everything waits on `0301`. There is no useful client work before the build exists, which is why
  `DEC-022` is the most urgent thing in this epic.

**Critical path:** `0301 → 0303 → 0304 → 0305 → 0306 → 0307 → 0308 → 0312`.

## Open decisions

| ID | Question | For | Blocks |
| --- | --- | --- | --- |
| `DEC-022` | What is the `web-client` toolchain, and how do its checks enter CI? | architect | `STORY-0301`, and therefore all of it |
| `DEC-023` | How does a rematch reach the server, given the wire carries no rematch message? | architect | `STORY-0309` only |
| `DEC-024` | Does this epic ship an automated two-browser end-to-end test, or is that proof manual in v0.1? | architect | nothing; decides whether a thirteenth story exists |
| `DEC-018` | What does a player see while the duel is paused around them? | the human | nothing here — the epic renders no pause state until it is answered |
| `DEC-017` | The display-name product rules | the human | nothing here — no name is on the wire, so `STORY-0311` renders none |

`ADR-0003` is **not** open: React, TypeScript and Tailwind are settled and `DEC-022` does not
reopen them.

## Definition of done

- [ ] Every story is `done` or `dropped`.
- [ ] From a clean clone, the client installs from a committed lockfile and its typecheck, lint,
      unit tests and production build all exit 0 — in CI, not only locally.
- [ ] `./gradlew :poker-server:verifyProtocolTypes` still passes, and
      `web-client/src/protocol/protocol.gen.ts` is byte-identical to what the emitter writes.
- [ ] No file outside `src/protocol/` declares a socket message type, and no client module exports a
      function that decides a legal action, a pot, a hand rank or a winner.
- [ ] One test drives the client from `Hello` to `DuelFinished` over real server frames and asserts
      the result screen.
- [ ] Across that duel, the opponent's hole cards appear nowhere in the rendered output before the
      snapshot that reveals them.
- [ ] Reloading mid-duel resumes the same seat and repaints from the next `Snapshot`, with no test
      sleeping on a real clock.
- [ ] A player whose only duel was a loss sees a balance of `−1`, unclamped.
- [ ] Checked by hand, once, and recorded: a link is sent, the other browser joins, and a duel is
      played to a winner. `docs/vision.md`'s success condition is not an automated assertion, and
      pretending otherwise would be the dishonest kind of green.

## Metrics

Filled in when the epic closes; feeds the Product B case study.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
