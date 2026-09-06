---
id: STORY-1403
title: The room code field takes the link as well as the code
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, lobby]
depends_on: [STORY-1402]
---

## Goal

A rival who was sent the whole invite link can paste it into the front door's room-code field and
reach the duel — the same parse the client already performs for the tab that *navigates* to that
link, performed for the person who pastes it instead.

## Why

**The link is what the host actually sends.** `ADR-0110` §5 puts the invite link in a read-only box
at the waiting table and `ADR-0022` makes the code the invite; the one thing a person does with a
link they were sent, other than click it, is paste it where the app asks for a code. Today that
paste spends one of the ten failed joins per minute `ADR-0022` budgets them and tells them nothing.

**It is the only front-door annotation with no decision and no drawing in it.** The parser exists,
the field exists, and nothing a player looks at changes.

It queues behind `STORY-1402` because both stories edit the front-door branch of
`web-client/src/lobby/Lobby.tsx` and its assertions in `Lobby.test.tsx`; two stories startable at
once on those files would conflict.

## Design notes

Measured on `develop` at `7c39fd3d`, or merged.

- **The parser exists and has one home.** `roomCodeFromSearch(search)` returns the code a URL's
  search string carries, or `null` (`web-client/src/lobby/room-link.ts:11`). Its one production
  caller is the tab's entry, `web-client/src/main.tsx:160` —
  `joinRoomCode: roomCodeFromSearch(window.location.search)` — which `store/boot.ts:84,99` then
  prefers over the remembered code. **Whatever this story adds must go through the same function**:
  the code the field yields for a link and the code a navigation yields for the same link may not be
  two answers from two parsers.
- **The shipped submit path.** `const code = normalizeRoomCode(typedCode)` (`Lobby.tsx:73`); the
  submit handler refuses an empty `code` — *"An empty code would spend one of the ten failed joins
  `ADR-0022` budgets this player every minute"* — and otherwise sends `{ type: "JoinRoom", code }`.
  The field itself is `Lobby.tsx:419`'s `Room code` label and input. All of that stays.
- **`ADR-0022`'s no-oracle rule governs everything added here.** `normalizeRoomCode` trims and
  upper-cases *"and nothing else"*, with its own comment recording why: the server answers an
  unparseable code and an unknown room identically on purpose, so validating a shape on the client
  hands back the oracle the server withholds. The field therefore **never says** whether what was
  typed looks like a code, looks like a link, or looks like nothing — before or after this story.
- **The shape of the change.** Derive the code from what was typed: the `room` parameter when the
  text parses as a URL that carries one, and today's normalised text otherwise. A link with no
  `room` parameter is not an error — it falls through to exactly today's behaviour and the server
  answers it. Whether the caller hands `new URL(text).search` to `roomCodeFromSearch` inside a
  guard, or `room-link.ts` grows one sibling that takes whole text and delegates, is the ticket's
  call.
- **No new string, no new visible state, and therefore no card.** The label stays `Room code`, the
  button stays `Join the duel`, nothing announces that a link was recognised, and
  `design/screens/enter-code.html`'s frame draws the field exactly as truly after this story as
  before it. `ADR-0091` §2 asks for a card where a story puts a new surface in front of a player;
  this one puts no surface at all, and there is nothing for the human's eye to grade.
- **The cases the tests must name**, because a cheap model will write the happy one and stop: the
  bare code (today's path, unchanged); the whole link exactly as the invite box holds it
  (`http://host:5173/?room=CODE`); a link whose `room` sits among other parameters; a link with **no**
  `room` parameter; leading and trailing whitespace; lower case; and the empty field, which still
  sends nothing. **Two different codes across those cases** — a single fixture value cannot tell a
  parse from a constant.

## Tasks

Split into two on 2026-09-07, the shape this story predicted.

| Task | What it settles |
| --- | --- |
| [TASK-140301](../tasks/TASK-140301-one-function-reads-a-code-out-of-a-pasted-invite-link.md) | The derivation and its five unit tests. `room-link.ts` grows **one** sibling, `roomCodeFromField(text)`, which hands `new URL(text.trim()).search` to `roomCodeFromSearch` inside a `try` and falls through to `normalizeRoomCode(text)`. `roomCodeFromSearch` is **not** generalised: its `null` is load-bearing at `store/boot.ts:84,99`, where `options.joinRoomCode ?? remembered` prefers the URL's code over the remembered one, and a version answering `"HTTPS://DUELS.EXAMPLE/LOBBY"` instead of `null` would win that `??` and send a returning player to nowhere |
| [TASK-140302](../tasks/TASK-140302-the-front-doors-field-joins-the-room-a-pasted-link-names.md) | The call site: two lines in `Lobby.tsx` and three tests through the rendered front door, on the `JoinRoom` the socket is sent |

**What a paste that is neither a code nor a link does**, decided here so no ticket invents it: it is
sent, trimmed and upper-cased, exactly as today, and the server answers it — including a URL that
carries no `room` and one whose `room` is blank. The field refuses only the empty and
whitespace-only cases, which it already refuses, and it reports nothing about what it made of the
text either way (`ADR-0022`).

## Acceptance criteria

- [ ] Pasting the exact string the invite box holds and pressing `Join the duel` sends the same
      `JoinRoom` message as typing the bare code, asserted both at `room-link.ts`'s level and
      through the rendered front door
- [ ] A bare code, a link with no `room` parameter, whitespace, lower case and an empty field all
      behave exactly as they do today, each named by its own assertion
- [ ] Exactly one function in `web-client/src` decides a code from a link, and both the paste path
      and `main.tsx`'s navigation path reach it
- [ ] The front door renders the same label, the same control and no new string, and a test pins
      that nothing is said about what was typed (`ADR-0022`)
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0

## Out of scope

- **The label.** Whether `Room code` should advertise that it takes a link is a copy question nobody
  has asked — the annotation says *"shoud also accept link"*, not *say so*. It is one string and one
  card frame, recorded here so the reversal is known to be cheap (`ADR-0110` §2's convention). Note
  that `enter-code.html` also draws a hint, *Eight characters, letters and numbers*, which the
  client has never shipped; if that hint is ever adopted, this question returns with it.
- **Validating the alphabet, the length, or the origin of a pasted link.** `ADR-0022`'s no-oracle
  rule forbids it, and a link from another origin carrying a valid code is still a code the server
  can answer.
- **The ten-failed-joins-a-minute budget** and the `TOO_MANY_ATTEMPTS` refusal — `ADR-0022` §2's,
  and `ADR-0105` §6 records that the budget is not built at `develop`.
- **The overlap and the rename** — `STORY-1402`.
- **Anything after `JoinRoom`.** Where a joining rival lands is `ADR-0094` §1's and is untouched.
