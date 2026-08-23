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
- Rematch: `OfferRematch` sent, `RematchOffered` rendered, the opening `Snapshot` taken as the start
  ([`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md)). The frames
  themselves are `EPIC-02`'s `STORY-0213`.
- Reconnect: a dropped socket or a reloaded tab returns to the same seat.
- The pause state: `OpponentPresence` rendered as away, timed out or back, with a countdown the
  client shows and never acts on, and `ActedForAbsent` labelling what the server did
  ([`ADR-0028`](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md), placed by
  [`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md)). The frames themselves are
  `EPIC-02`'s `STORY-0214`.
- The profile strip: the coin balance and the recent duels, read over HTTP, signed and unclamped.
- One test that plays a whole scripted duel through the client and asserts the secrecy property from
  the client side.

## Out of scope

| Not here | Where |
| --- | --- |
| Any change to the server, the protocol, or the rules | Nowhere in this epic. A client that needs a new frame raises a decision rather than editing Kotlin — `DEC-023` and `DEC-038` both did exactly that, and [`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) and [`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) put both server halves in `EPIC-02`, as `STORY-0213` and `STORY-0214`, which `STORY-0309` and `STORY-0313` consume |
| Deciding the visual language — palette, type, the table's composition | EPIC-06. This epic consumes `design/tokens/tokens.css` and the screen designs; it authors none |
| Accounts, the claim flow, display names | EPIC-04. No name is on the wire today, so no name is rendered — see [`ADR-0038`](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) |
| Leaderboard, seasons, ratings | EPIC-05 |
| Replay viewer, equity, decision quality | EPIC-08 |
| Hosting, TLS, serving the built assets in production, Docker | EPIC-07. This epic builds a bundle and runs a dev server; delivering it is not its problem |
| A bot driving the client | EPIC-09 |
| Spectating | [`ADR-0040`](../../docs/adr/ADR-0040-a-duel-may-be-watched-without-hole-cards.md) settles the shape — live, minus every hole card — but no `PlayerView` exists for a third party and no epic owns building one |
| What a player is shown while the opponent is away | In scope, and it is not `STORY-0310`'s. [`ADR-0028`](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md) answered it after this epic was written, and [`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) puts the rendering in `STORY-0313` — four of the five presence frames reach the player who *stayed*, at the table. The **words** a player reads stay the human's, reserved by `ADR-0028` |
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
| [STORY-0301](../stories/STORY-0301-web-client-toolchain.md) | The web-client toolchain and its first green check | — | done |
| [STORY-0302](../stories/STORY-0302-design-tokens-in-the-client.md) | The design tokens are the client's only colours and sizes | 0301 | done |
| [STORY-0303](../stories/STORY-0303-typed-socket-and-handshake.md) | The typed socket: handshake and device identity | 0301 | done |
| [STORY-0304](../stories/STORY-0304-client-store.md) | The store: state is the last frame the server sent | 0303 | done |
| [STORY-0305](../stories/STORY-0305-lobby-and-room-link.md) | The lobby: create a room, join by code, share the link | 0302, 0304 | done |
| [STORY-0306](../stories/STORY-0306-duel-table-screen.md) | The duel table renders a `PlayerView` | 0305 | done |
| [STORY-0307](../stories/STORY-0307-action-bar.md) | The action bar: acting on your turn | 0306 | ready |
| [STORY-0308](../stories/STORY-0308-result-screen.md) | The result screen: who won, and the coin | 0307 | backlog |
| [STORY-0309](../stories/STORY-0309-rematch.md) | Rematch from the result screen | 0308, `STORY-0213` | ready |
| [STORY-0310](../stories/STORY-0310-reconnect-and-resume.md) | Reconnect: the client resumes its seat | 0306 | backlog |
| [STORY-0311](../stories/STORY-0311-profile-strip.md) | The profile strip: my coins and my recent duels | 0302, 0303 | backlog |
| [STORY-0312](../stories/STORY-0312-whole-duel-through-the-client.md) | A whole duel through the client, frame by frame | 0308, 0310 | backlog |
| [STORY-0313](../stories/STORY-0313-the-table-names-an-absent-opponent.md) | The table names an absent opponent | 0307, 0310, `STORY-0214` | blocked |
| [STORY-0314](../stories/STORY-0314-a-host-can-leave-the-room-they-opened.md) | A host can leave the room they opened | 0309 | ready |

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
- **`0309` and `0313` are not this epic's to start.** Each waits on a `poker-server` story in
  `EPIC-02` — `STORY-0213` and `STORY-0214` — and those two land one at a time, because both move
  `PROTOCOL_VERSION` ([`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) §3).
  This epic's close date is therefore partly `EPIC-02`'s, and no client agent can move it.

**Critical path:** `0301 → 0303 → 0304 → 0305 → 0306 → 0307 → 0308 → 0312`.

## Open decisions

**One remains, and it does not wait on a human.** It is the architect's.

| ID | Question | For | Blocks |
| --- | --- | --- | --- |
| `DEC-024` | Does this epic ship an automated two-browser end-to-end test, or is that proof manual in v0.1? | architect | nothing; decides whether a fourteenth story exists — but it is due **before this epic closes** |

### Answered since this epic was written

| ID | Answered by | What it means here |
| --- | --- | --- |
| `DEC-070` | [ADR-0075](../../docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md) | **The mark lives as long as the absence that produced it.** Exactly two frames take it off — an `OpponentPresence` carrying `PRESENT`, and `DuelFinished` — as two keys in two case bodies of `duel-state.ts`; every other frame leaves it standing, and there is **no timer, no fade and no dismiss control**. *Clears on the next `Snapshot`* and *clears on the next `YourTurn`* were eliminated on the server's own code rather than on taste: `AbsentSeats.kt` prepends the mark to `next.outbound` and `act` composes that through `framesFor` = `broadcast + turnFor`, so the mark and the `Snapshot` about the mark's own action are **consecutive frames in one delivery** and either rule would clear it microseconds after it was set. It clears on the **frame**, not on a transition, so it needs no `rivalReturned`-style bookkeeping. **No new string** — `ADR-0046` §4's six stand. The failure the decision was raised on is impossible by construction: the frame that puts *Your rival is back.* on screen is the same frame that takes the mark off. Accepted cost, stated rather than discovered: **a mark can be older than the hand on screen**, because the present player can fold the button pre-flop and the turn never reaches the absent seat — the price of §4's *no action log*. **`TASK-031314` and `TASK-031315` are `backlog`**, behind `TASK-031313` like the rest of the chain; no Kotlin, no frame, no protocol step |
| `DEC-068` | [ADR-0073](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) | The *waiting for your rival* screen gains a way out, and it reads **`Back to the lobby`** — the string `DuelResult.tsx` already renders for the same action on the same memory, so this epic keeps one phrase for one action. It calls `ADR-0072` §4's `forgetRoom()` from an event handler and **does nothing to the room**: still `WAITING`, seat 0 still the host's, the code still resolving, and `ADR-0022`'s idle timeout still the only thing that ends it. **Exactly one line says so** — *The room stays open. That link still works for your rival, and it brings you back.* — and it names no duration, because the client owns no clock against a server window. **No confirmation.** Those two strings are the whole addition; a third needs a new ADR. *Cancel*, *Close the room* and *Leave* are refused by name as untrue. `design/screens/create-duel.html`'s waiting frame gains both strings verbatim as `EPIC-06`'s work, and **`STORY-0314` does not wait on it**. No Kotlin, no frame, no protocol step — this epic's rules hold. **`STORY-0314` is `ready` and unsplit** |
| `DEC-067` | [ADR-0072](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) | A tab remembers the room it is **seated in** until the player leaves it or the server refuses its own rejoin. `boot.ts`'s `DuelFinished` branch (`TASK-031009`) is **deleted**, so a reopened socket rejoins and `ADR-0044` §5's restatement becomes reachable; the way back to the lobby is what forgets, through a third member on `DuelClient` (`forgetRoom`), an optional third prop on `DuelProvider` and `useForgetRoom()` beside `useSend()` — `ADR-0032` §3's *event handlers only* extends to it. `DuelResult` keeps its `<a href="/">` and stays a function of its props. No reducer field, no frame, no Kotlin. **`STORY-0310` keeps its thirteen tickets** and `TASK-031009` stays `done` and unrewritten; **`STORY-0309` keeps its fourteen** and gains the transport half of its fourth criterion as a split of its own |
| `DEC-023` | [ADR-0044](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) | `ClientMessage.OfferRematch` (no fields) and `ServerMessage.RematchOffered(seat)` to both seats, idempotent on a repeat, restated after a reconnect's frames. No started frame: after a `DuelFinished`, a `Snapshot` **is** the rematch. Two refusals — `UNKNOWN_ROOM` (the room is gone, go to the lobby) and a transient `REMATCH_UNAVAILABLE` (not yet; nothing recorded). No deadline on the wire, so no countdown is rendered. `PROTOCOL_VERSION` moves one step, taking the next free number when it lands. **The server half is `EPIC-02`'s `STORY-0213`** — this epic's no-Kotlin rule holds, and `STORY-0309` is `ready` and consumes it |
| `DEC-022` | [ADR-0026](../../docs/adr/ADR-0026-vite-and-npm-drive-the-web-client.md) | Vite + npm on Node 24, Vitest, ESLint + Prettier, and a parallel `client` CI job. `STORY-0301` shipped on it |
| `DEC-018` | [ADR-0028](../../docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md) | `OpponentPresence` carries PRESENT/AWAY/ABSENT with a countdown the client renders but never acts on, and `ActedForAbsent` marks every action taken for an absent seat — **once the server emits them.** Neither type exists yet, in Kotlin or in `protocol.gen.ts`, and this epic writes no Kotlin, so `STORY-0310` was split without a pause state. `DEC-038` asked who ships the other half and `ADR-0045` answers it — see the row below |
| `DEC-038` | [ADR-0045](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md) | The server half is **`EPIC-02`'s `STORY-0214`**, on `ADR-0044`'s argument; `STORY-0208` stays `done`. It takes its **own** `PROTOCOL_VERSION` step, and only one protocol-bumping branch may be open at a time — `STORY-0213` is in front of it. The client half is **`STORY-0313`**, not a reopened `STORY-0310`: four of the five presence frames reach the player who stayed, at the table. `STORY-0310` keeps its thirteen tickets untouched, and `STORY-0313` is `blocked` until `STORY-0214` merges — this epic now has a story only `EPIC-02` can unblock. The **copy** it left open is `DEC-039`, answered by `ADR-0046` — see the row below |
| `DEC-039` | [ADR-0046](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md) | The words. The seat plate says `Away` and `Timed out`, and nothing for `PRESENT` — presence outranks `Their turn` and never outranks `Folded` or `All in`. The line that explains them: *Your rival is away. The duel is paused.* / *Your rival did not come back. The duel continues, and the server acts for them.* / *Your rival is back.*, that last one **only** when this client previously held `AWAY` or `ABSENT`, because a resuming client is always sent `PRESENT` and its rival never left. The countdown carries no word of its own and **nothing a player reads changes when it reaches zero**. An action the server took names the server: *The server folded for your rival.* / *The server checked for you.* — never the rival as the actor, never `auto-fold`, never `disconnected` or `left` (a cause the server cannot see), never the cash-game *sitting out*. Showing the most recent mark is enough; no action log is designed. Placement, colour and the countdown's typography stay `EPIC-06`'s. **`STORY-0313` is splittable the day `STORY-0214` merges** |
| `DEC-017` | [ADR-0038](../../docs/adr/ADR-0038-a-name-is-screened-when-set-and-can-be-taken-away.md) | Still nothing here — the rules are set-time and operator-side, and `STORY-0311` renders whatever name the wire carries |
| `DEC-009` | [ADR-0040](../../docs/adr/ADR-0040-a-duel-may-be-watched-without-hole-cards.md) | Spectating stays out of scope for this epic, but is no longer undecided: live, minus every hole card, through a third projection in the engine |
| `DEC-037` | [ADR-0043](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md) | A `Rejected` closes no decision point. The reducer keeps `pendingTurn`, clears `rejection` on the next `YourTurn`, `Snapshot` or `DuelFinished`, and counts refusals in a new `rejectionCount` the bar's remount key consumes. **This epic's self-imposed "no server, no protocol change" holds** — `guard` already accepts the identity the client holds after a rejection, so nothing on the wire moves. The fix is five files, so `TASK-030712` is the store half and the bar half is a sibling ticket |

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
