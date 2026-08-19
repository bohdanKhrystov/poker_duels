# ADR-0059 — The record is searched when the player submits, not while they type

- **Status:** Accepted
- **Date:** 2026-08-19
- **Resolves:** `DEC-052` — when does the history search send its request: as the player types, or
  when they submit it? **Derived from the vision; the human did not state this call.** The licensing
  sentence is the positioning one — *"The reference points are **Lichess** and **Chess.com**, not
  PokerStars. Dark, quiet, fast, minimal."* — read against a screen whose rows and page position are
  discarded by every filter change: a list that reorganises itself between keystrokes is not quiet,
  and putting an unindexed scan behind every pause is not fast. The roadmap's v0.2 row
  (*"Persistent profile, duel coin counter, match history"*) is what makes the screen itself a
  commitment rather than a choice
- **Builds on:** [`ADR-0057`](ADR-0057-a-cursor-names-the-filter-it-was-drawn-under.md) §1 (a cursor
  is bound to the filter it was drawn under, so a term change invalidates the walk in progress by
  construction) and `STORY-0413`'s own rule that a filter change drops the cursor **and** the rows
- **Constrains:** `TASK-041312`, which is unblocked by this ADR and gains one Scope bullet, one word,
  one test and one `verify:` line — all four named in §5
- **Amends nothing**, changes no server behaviour, no wire shape and no `PROTOCOL_VERSION`. The
  request `duelsPath` builds is byte-identical under either answer; only what *causes* it moves

## Context

The screen is specified and eleven of `STORY-0413`'s fourteen tickets build it. What is not
specified is the act that sends a search — the story says only that the term is sent *"unmodified"*.

Read from what is merged and from the tickets that build on it:

- **A search is a filter change, and a filter change is destructive.** `TASK-041307` drops the
  cursor *and* the accumulated rows whenever the filter changes, because `ADR-0057` makes a cursor
  valid only under the filter it was drawn under. So every term the client sends throws away the
  walk in progress — not a re-render, a restart from page one.
- **The read behind it is admittedly not fast.** `STORY-0409` recorded the cost rather than
  discovering it: `duel.finished_at`, `duel_result.player_id`, `coin_delta` and `display_name` are
  all unindexed for this query, a `%term%` search cannot use a pattern-ops index in any case, and
  the note is verbatim *"Correct without one; not yet fast at a size v0.1 does not have."*
- **The client has exactly one timer, and it is not in the tree.** `protocol/reconnecting.ts`
  schedules its retry with `setTimeout`, outside React entirely — `ADR-0032` puts the socket outside
  the component tree, so no component has ever owned a clock. A debounce would be the first one
  inside it. `virtual-time.test.ts` is a merged sweep that fails the build for any test file naming
  `setTimeout`, `setInterval` or `requestAnimationFrame` without calling `vi.useFakeTimers()` first.
- **The client already has the submit idiom, twice over.** `Lobby.tsx` renders a `<form>` with a
  labelled input and `<button type="submit">Join the duel</button>` — refusing an empty value without
  spending a request — and `NameSurface.tsx` renders a second one for the display name. Enter submits
  both. `TASK-041305` reserved the search control's word for `TASK-041312` *"because `DEC-052`
  decides whether there is a control at all."*

### The forces

**As-you-type is what a search box is, and everyone knows how to use one.** The box and the rows
beneath it always agree; a player exploring half-remembered names gets feedback per pause; nothing
has to be discovered, labelled, or clicked. That is a genuine advantage and it is why this was a
real decision rather than an obvious one.

**Against it: the delay is a number with no evidence behind it.** Nobody has measured this read at
any size, no player has used this screen, and a debounce interval chosen today becomes a
product-visible constant that will be revisited by nobody. It also puts the first timer *inside* the
component tree, in the story that most wants to be asserted against a fake transport.

**And the cost of a stray pause is not a wasted request — it is the player's place in the record.**
Three pages into a walk, one pause mid-word discards both the cursor and every row already read, and
sends a term (*Hal*) the player had not finished typing (*Halvard*). Under submit-only the same
mistake costs a keystroke.

**The thing submit-only genuinely gives up** is agreement between the box and the list: a player can
leave a term in the box, never submit it, and read rows that do not match what they typed, with the
screen saying nothing about it. That is a real cost and §6 records it rather than pretending the
answer covers it.

## Decision

### 1. The search fires on submit, and typing sends nothing

The opponent box lives inside a `<form>`. **Exactly two acts send a search:** pressing Enter while
the box has focus, and activating the form's submit button. Typing, pausing, focusing and blurring
send nothing, ever. No timer of any kind is introduced by this screen.

### 2. The control is a submit button reading *Search*

`history-text.ts` gains **one** export, added by the ticket that needs it:

```ts
export const SEARCH = "Search";
```

Plain, in the register `ADR-0058` §4 fixed. The vocabulary rule the vision states is about *casino*
words — *buy-in*, *bankroll*, *jackpot*, *bonus* — not about the ordinary verb for what this control
does; inventing a duelling synonym for a search button would be costume, not vocabulary.

`TASK-041305`'s *"exactly these exports, and no others"* is a statement about **that** ticket's file
as it lands; it already named this word as `TASK-041312`'s to add, and this is the addition it meant.

### 3. Emptying the box is a search like any other

Committing an empty box is a submit: `filter.opponent` becomes `""`, `duelsPath` renders no
`opponent` parameter, and the record widens back. There is **no separate clear control**, because
clearing is the same act as searching and a second control would be a second thing to label, style
and test for a state the first one already reaches.

### 4. Nothing else about the search moves

Everything `TASK-041312` already specifies stands verbatim: the term is sent as typed — no trim, no
case fold, no NFC normalisation, no truncation, no wildcard stripping — the server owns every
refusal, and a search drops the cursor and the rows while keeping the chosen outcome.

### 5. What `TASK-041312` gains, exactly

Four things, and nothing else:

| | |
| --- | --- |
| Scope | One bullet: the box is inside a `<form>`; the request is sent on submit — Enter or the button — and by nothing else; no timer is added |
| Word | `SEARCH = "Search"` in `history-text.ts`, which is the third file the ticket already lists |
| Test | `asks nothing while the player types, and once when the search is submitted` |
| `verify:` | One line greping that test name out of the verbose reporter |

The test types a term one character at a time, asserts the fake transport received **no** request,
then submits through `getByRole("button", { name: "Search" })` and asserts **exactly one** request
carrying the whole term. Both halves matter: the first fails against any debounce, the second against
a form that submits twice or navigates. Finding the button **by its accessible name** is also what
pins the string, so the word needs no golden test of its own and the ticket stays at three files.

`files_touched` stays `3` and the ticket's three existing tests stand unchanged. **`TASK-041314`'s
472 does not move**: its arithmetic already budgets `TASK-041312` at *"+4 (three written at the
split, one named by `DEC-052`'s ADR)"*, and this ADR names exactly one.

## Consequences

**What it costs, plainly:**

- **The box and the rows can disagree, and nothing says so.** A player who types *Halv* and never
  submits is reading a list that is not about *Halv*. This ADR does not add a *showing results for…*
  line, a pending marker or a highlight — the screen still has exactly the four states
  `STORY-0413` names, and this is the honest gap in the answer.
- **Widening back costs a deliberate act.** Emptying the box does nothing until it is submitted, so a
  player who deletes their term and walks away still sees a filtered record. Under as-you-type it
  would have widened by itself.
- **It feels older than the search box in every other product the player used today**, and there is
  no way to measure how much that costs us before we ship it.
- **One more control on a screen that already carries four outcome choices, a page control and a
  box** — and one more string for `EPIC-06` to style.

**What it buys:** one request per search instead of one per pause against a read nobody has
indexed; no timer inside the component tree, so every test on this screen stays a fake transport and
`virtual-time.test.ts` keeps being a sweep that never fires; and a walk that survives typing.

**What it forecloses:** nothing permanently. Adding a debounce later is **additive** — the submit
path stays, and a delay becomes a second trigger for the same code — while removing one is a
behaviour change players have already learned. That asymmetry is the reason to start here while the
evidence is this thin: **there are no players yet, and no measurement of this read at any size.**

**What would reverse it:** a measured p95 for the filtered read at a realistic row count, with an
index behind it, plus a delay chosen against that measurement rather than against habit. Until both
exist, as-you-type is a guess with a timer in it.

## Alternatives considered

**As-you-type, debounced.** The strongest case: it is what a search box *is* in 2026, so it needs no
discovery and no label; the box and the rows never disagree; it saves a keystroke on every search;
and a player who half-remembers a name gets the feedback that makes the feature usable at all. It is
also what Chess.com and Lichess both do in their own search fields, and the vision names both as
reference points. Rejected on four counts, in ascending order of weight. The delay is a number with
no evidence behind it and would be chosen by taste. It puts the first timer inside the component
tree, in the one story whose design notes say paging and search are *"asserted against a fake
transport, not a timer"*. Each pause is an unindexed `POSITION` scan over a join the story that built it recorded as
*not yet fast*. And decisively: because `ADR-0057` binds the cursor to the filter, every pause
**discards the player's place in the record** — a mid-word pause is not a wasted request, it is a
restart, and it sends a term the player never meant to search.

**As-you-type with no debounce — one request per keystroke.** The strongest case is that it removes
the very thing that made the option above hard: there is no delay to choose, no timer, no fake-timer
discipline, and it is the simplest code on this list. Rejected because it multiplies the unindexed
scan by the length of the term, and because this client has no request sequencing anywhere: two
in-flight reads can land out of order and leave the rows of a prefix under a longer term, which is
the worst possible failure on a screen whose whole promise is *this is your record*.

**Submit on blur.** The strongest case: no button, no new word, no keystroke, and the search happens
at the moment the player stops caring about the box. Rejected because the rule is invisible — nothing
on the screen says the search will happen when you leave — and because blur fires for reasons that
have nothing to do with searching: tabbing to the outcome filter, clicking *Show more*, switching
tabs. A trigger a player cannot see is one they cannot avoid.

**A debounce that only fires from the third character.** The strongest case is that it answers the
cost objection directly: short terms are the expensive ones, so exempt them. Rejected because it is a
second unevidenced number stacked on the first, and because it silently refuses to search the names
that exist — a two-character display name is legal, and a player looking for one would type it and
watch nothing happen.

**Search on submit, with a *Clear* control beside it.** The strongest case: emptying-and-submitting
is the one interaction this answer makes clumsy, and one button fixes it. Rejected as scope this
decision was not asked for — clearing reaches a state submitting already reaches, and a control that
duplicates another is a second thing to name, place and test. If the empty-and-submit path turns out
to be the thing players stumble on, that is a ticket, not this ADR.
