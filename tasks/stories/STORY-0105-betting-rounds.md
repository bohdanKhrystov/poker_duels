---
id: STORY-0105
title: Betting rounds
type: story
status: done
parent: EPIC-01
module: poker-engine
labels: [engine, rules]
depends_on: [STORY-0104]
---

## Goal

A hand can be played from the posting of blinds to the river: blinds, action order, legal
actions, min-raise arithmetic, all-ins, and advancing from street to street.

## Why

This is where the rules actually live, and where heads-up differs most from what everyone
assumes. It is also where the majority of real poker-engine bugs are found.

## Design notes

Rules are specified in [`duel-rules.md`](../../docs/duel-rules.md); this story implements them
and adds nothing.

The traps, listed so they are impossible to miss:

- **The button is the small blind in heads-up.** It acts *first* preflop and *last* on every
  street afterwards. Getting this backwards produces a game that looks entirely normal and is
  wrong in every hand.
- Min-raise is the size of the largest previous bet or raise **on the current street**, not the
  size of the current bet.
- An all-in for less than a full raise **does not reopen** betting for a player who already
  faced the full raise. They may call, not re-raise.
- A bet larger than the opponent's stack is capped, and the excess returns as an uncalled bet.
- The big blind has the option to raise preflop when the action is merely called.

## Tasks

Schema 2, one dependency chain: exactly one ticket is startable at a time, because almost every
ticket extends the file the one before it created. IDs `010501`–`010504` were the schema-1 split
and are retired, not reused.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010505](../tasks/TASK-010505-heads-up-seat-order.md) | Name the heads-up blind and action order once | ready |
| [TASK-010506](../tasks/TASK-010506-post-the-blinds.md) | Open a hand by posting both blinds | backlog |
| [TASK-010507](../tasks/TASK-010507-deal-hole-cards.md) | Deal the hole cards and put the action on the button | backlog |
| [TASK-010508](../tasks/TASK-010508-legal-actions-core.md) | Compute the legal actions at an ordinary decision point | backlog |
| [TASK-010509](../tasks/TASK-010509-legal-actions-all-in.md) | Restrict the legal actions around an all-in | backlog |
| [TASK-010510](../tasks/TASK-010510-action-validation.md) | Turn an illegal action into the reason it is illegal | backlog |
| [TASK-010511](../tasks/TASK-010511-action-to-event.md) | Turn an accepted action into the event that records it | backlog |
| [TASK-010512](../tasks/TASK-010512-default-engine.md) | Handle one betting action in a real engine | backlog |
| [TASK-010513](../tasks/TASK-010513-round-completion.md) | Decide whether the betting round has anyone left to act | backlog |
| [TASK-010514](../tasks/TASK-010514-pass-the-action.md) | Pass the action to the other seat while the round runs | backlog |
| [TASK-010515](../tasks/TASK-010515-engine-contract-test.md) | Run the engine contract against the real engine | backlog |
| [TASK-010516](../tasks/TASK-010516-fold-ends-the-hand.md) | End the betting the moment a player folds | backlog |
| [TASK-010517](../tasks/TASK-010517-street-advance.md) | Close the round and deal the next street | backlog |
| [TASK-010518](../tasks/TASK-010518-all-in-run-out.md) | Run the board out when nobody can bet again | backlog |
| [TASK-010519](../tasks/TASK-010519-opening-run-out.md) | Do not stall a hand whose blinds leave nobody able to act | backlog |
| [TASK-010520](../tasks/TASK-010520-hand-walkthrough-test.md) | Play one scripted hand from blinds to showdown | backlog |
| [TASK-010521](../tasks/TASK-010521-betting-invariant-property.md) | Assert the betting invariants over a thousand random hands | backlog |

## Where this story stops

The engine takes a hand as far as it can go without deciding who won: a fold leaves the chips
swept into the pot with one seat marked folded, and a played-out hand stops at `ShowdownReached`.
`UncalledBetReturned`, `HandRevealed`, `PotAwarded` and `HandFinished` are STORY-0106. The
uncalled part of a bet stays recoverable because `committedThisHand` never decreases.

## Acceptance criteria

- [ ] The button posts the small blind, acts first preflop and last on every later street.
- [ ] Illegal actions are rejected with a reason, and leave the state untouched.
- [ ] Min-raise is computed from the largest raise increment on the current street.
- [ ] A short all-in does not reopen betting for a player who has already faced a full raise.
- [ ] The big blind may raise preflop after a call.
- [ ] Chip conservation holds after every single action, asserted as a property.
- [ ] A street ends exactly when both players have acted and matched, or one is all-in.

## Out of scope

- Awarding the pot — STORY-0106.
- Anything spanning more than one hand — STORY-0107.
- Action clocks and timeouts — EPIC-02.
