---
id: STORY-0504
title: What a row leads to — another player, seen by a stranger
type: story
status: dropped
parent: EPIC-05
module: poker-server, web-client
labels: [server, client, leaderboard, read-path]
depends_on: [STORY-0503]
---

## Dropped — `ADR-0067`, on 2026-08-22

**The decision that killed it:**
[`ADR-0067`](../../docs/adr/ADR-0067-a-leaderboard-row-is-text-and-no-id-turns-into-a-profile.md),
answering `DEC-057`. **A leaderboard row leads nowhere.** It is a rank, a name or `No name`, and a
season standing rendered as one line — not a link, not a button, not a control — so there is no
destination, no endpoint and no screen to build. What a stranger reads about another player is
**exactly the four fields the ladder row already carries** (`rank`, `displayName`, `coins`,
`playerId`), all of which shipped with `STORY-0502`; §3 of the ADR enumerates field by field what
they do not read, and the duel list stays the property of the player it belongs to. The premise
below — *a leaderboard row stops being a dead end* — turned out to be false, which is exactly what
`dropped` is for.

The file stays, unrewritten below this line, because the epic predicted this story might not survive
its decision and the trail is worth more than the tidiness. It is the second of the two it named;
`STORY-0505` was the first.

**Where its work went.** Nowhere, and nothing was lost. Every design note below was a constraint on
a thing that is not being built. The one assertion that survives — *a row leads nowhere* — already
ships: `TASK-050313` asserts that inside the leaderboard section `queryAllByRole("link")` is empty,
`querySelectorAll("a")` is empty and `queryAllByRole("button")` is empty, and `ADR-0067` §1 turns
that from *"until `DEC-057` is answered"* into the decision itself. `STORY-0503`'s out-of-scope line
*"Clicking a row through to a player — `STORY-0504`, and only if `DEC-057` says so"* is left as
written: it was true when it was written and it now has an answer.

**What it does *not* mean.** The ladder is not private — `GET /api/standings` is unauthenticated and
lists every player who finished a duel this season (`ADR-0063` §1), so a name and a season standing
are already public. What does not exist, and what `ADR-0067` §4 keeps from existing, is any route
that takes a player id as its subject: `/api/me` still means me, `GET /api/me/duels` gains no player
parameter, and no path in this product turns a name or an id into a lookup of a person
(`ADR-0029` §7). Reopening this is a new `DEC` naming one field at a time, not a ticket — and
publishing a fact a player did not choose to publish is the human's call, per `ADR-0067` §6.

---

## Goal

A leaderboard row stops being a dead end: it leads to whatever `DEC-057` says a stranger may see
about another player. If the answer is *nothing*, this story ends as `dropped` and the epic is one
story shorter.

## Why

`EPIC-04` parked it here by name — *"viewing another player's profile or history … it needs a name
per leaderboard row and owns what a row links to. Here, `/api/me` means me."* Every read path in
the product today answers about the requester and only the requester; a ladder is the first surface
that shows a player facts about someone else, so the boundary has to be drawn deliberately rather
than discovered by the first person who tries an id in a URL.

## Design notes

**Settled, and true whatever `DEC-057` answers:**

- **`/api/me` keeps meaning me.** Whatever this story adds is a *new* endpoint. No existing route
  grows a "which player" parameter, and `GET /api/me/duels` is not made to serve somebody else's
  history by adding an argument — that is how an authorisation boundary quietly becomes optional.
- **A stranger is not authenticated as the subject.** The reader presents their own identity
  (`X-Device-Id` or `ADR-0027`'s bearer token) and receives facts about a *different* player. The
  test that matters is the one where those two are not the same player, and it must exist.
- **No credential, session, device id, email or handle about the subject may appear**, in any field,
  ever. `docs/protocol.md` already records why the duel summary carries `opponentPlayerId` and never
  a device id: *"a device ID is the sole authentication token in v0.1, so handing one to the other
  player would hand over their account."*
- **No path turns a name into an identity.** `ADR-0029` §7 — the reason history search returns duels
  and never players. A lookup keyed on a display name is out; a lookup keyed on the `player.id` a
  ladder row already carried is a different thing, and the distinction is the whole rule.
- **Hole cards, folded cards and mucked cards appear nowhere**, as everywhere else.
- **`nameOrNone` prints the name, or `No name`** — `ADR-0058`, one function, one string.
- **The engine learns nothing.**

**Blocked on `DEC-057`**, which decides whether this story exists at all and, if it does, exactly
which fields a stranger may read: display name, coin balance, duels played, win/loss record, or the
duel list itself. Nothing here is buildable from a guess, because every one of those is a separate
disclosure and the ADR is what makes each of them deliberate.

**Inherits `DEC-054`.** A row that leads somewhere is a *link*, and a client with no addresses has
none to give it. If routes have not landed, the best this story can do is another in-client swap —
which works, and which means the destination cannot be shared, bookmarked or linked to from
anywhere else.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `DEC-057` — run `/plan-story STORY-0504` once it is answered, or set this story `dropped` if the answer is that a row is inert.* | — |

## Acceptance criteria

Written against the branch where `DEC-057` says a row leads somewhere. If it says a row is inert,
this story is `dropped` and `STORY-0503`'s *"a row is text and the tests assert no link"* criterion
is the whole of the answer.

- [ ] A reader who is not the subject reads the subject's public facts, asserted with two distinct
      players — never one player reading their own row and calling it proof.
- [ ] Every field `DEC-057` did not permit is absent from the response, asserted field by field
      against the serialised JSON rather than against a DTO.
- [ ] No device id, credential, session token, handle or email about the subject appears in any
      response, asserted on the raw body.
- [ ] An unknown player id answers the same way whether the id is malformed or simply not present,
      so the endpoint is not an existence oracle.
- [ ] `GET /api/me` and `GET /api/me/duels` are byte-identical in behaviour before and after this
      story: their existing tests pass unchanged and gain no "which player" case.
- [ ] `docs/protocol.md` contracts the new endpoint and states in prose what it deliberately does
      not carry.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **Anything about a player the reader has never met that `DEC-057` did not name.** A field not in
  the answer is not in the response; when in doubt the story is re-scoped, not widened.
- **Friends, rivals, head-to-head records** — v0.4.
- **A replay of another player's duel** — `DEC-008` and `EPIC-08`; the `MatchLog` is not persisted.
- **Messaging, challenging or following a player from their page** — matchmaking and social are
  *later* on the vision's roadmap, and a challenge button is matchmaking wearing a coat.
