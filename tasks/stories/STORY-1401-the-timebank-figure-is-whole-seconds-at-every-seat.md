---
id: STORY-1401
title: The timebank figure is whole seconds at every seat
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, table, bug]
depends_on: []
---

## Goal

Every seat's timebank reads `m:ss` — a minute figure, a colon, exactly two digits — at both seats
and at every beat, whatever fraction of a second the server's bank happens to hold. The client
stops disagreeing with the card that already draws it that way.

## Why

**It is the epic's one correction that decides nothing.** `EPIC-14`'s *Scope* row 3c marks the
figure a defect with a known cause and records that `DEC-135` does not gate it: the bar's contents
are a product question, and this number is not. It goes first because it is the cheapest thing on
the list, because the human photographed it on their **own** plate — `Timebank 1:58.623999999999995`
— and because that plate is in the background of every other table screenshot this epic works from.

**It shipped through a full suite and a QA cycle because nothing could see it.** Every merged
fixture that carries `bankRemainingMillis` holds whole seconds — `180_000`, `72_000`, `60_000`,
`45_000`, `12_000`, `10_000`, `0` — across `turn-clock.test.ts`, `DuelTable.test.tsx`,
`Lobby.test.tsx`, `no-derivation.test.tsx`, `null-view.test.tsx` and `duel-state.test.ts`. The
defect leaves every one of those values unchanged, so no assertion in the repository could ever
have failed on it. That is the story's real content: the fix is one expression, and the test is the
part that has to be got right.

## Design notes

Everything below is measured on `develop` at `7c39fd3d` or is merged, and no ticket re-litigates it.

- **The cause, exactly.** `bankFigure` declares its input in its own KDoc — *"@param seconds Whole
  seconds remaining, clamped to zero if negative"* — and floors nothing
  (`web-client/src/table/turn-clock.ts:35-40`). It has two call sites in `seatClock`. The seat the
  clock **names** passes `secondsRemaining(...)`, which is whole (`countdown.ts:12`, `Math.ceil`).
  The seat the clock does **not** name passes `clock.bankRemainingMillis[seat] / 1000` raw
  (`turn-clock.ts:105`). `118.624 % 60` is `58.623999999999995`, and that branch is the plate of the
  rival's-turn seat — which is where the human was looking at their own bank while the rival acted.
- **The contract stays where it is: the KDoc is right and the call site is wrong.** The repair is to
  hand `bankFigure` what it documents. `bankFigure` and `clockFigure` keep their signatures, their
  KDoc and their copied-from-the-card comments (`design/components/seat-and-pot.html`, `ADR-0024`
  §3).
- **Whole seconds by `Math.ceil`, not `Math.floor`, and the reason is merged.** `secondsRemaining`
  ceils so that *"the last whole second is shown for the whole of it and zero is reached at the
  deadline rather than a second before it"*, and `bankFigure`'s own KDoc records why the bank is
  drawn `m:ss` at all: to let a player *"distinguish a spent clock (which reads as `0`) from an
  exhausted bank (which reads as `0:00`)"* (`ADR-0108` §5). Flooring would print `0:00` — the
  shipped way to read an **exhausted** bank — for a bank that still holds 999 ms. Ceiling also makes
  the two call sites agree, which is what stopped one of them from being checked against the other.
- **No merged assertion moves.** For a whole number of seconds `Math.ceil(ms / 1000) === ms / 1000`,
  so every fixture listed above renders the string it renders today. No test file in the blast
  radius is edited except to **add**; if a change reddens an existing assertion, it did something
  this story did not ask for.
- **No card, and the reason is that the drawing is already correct.** `ADR-0091` §2 puts a card
  first where a story puts a **new surface** in front of a player. This story puts none: the merged
  component card `design/components/seat-and-pot.html` already draws `Timebank 3:00`, `Timebank
  1:12` and `Timebank 0:00`, and the client is what fails to transcribe it. There is nothing to draw
  and nothing for the human's eye to grade.
- **The test must use a fixture the defect changes, at both seats, with two different values.**
  A whole-second fixture cannot detect this bug at all, and a suite that asserts one seat's bank
  with one value passes a version that reads a hard-coded seat index or returns a constant. The
  assertion belongs at `seatClock`, not at `bankFigure`: `bankFigure` is correct for its documented
  input, so a `bankFigure`-only test cannot fail before the fix.

## Tasks

Split on **2026-09-06** into the one ticket the shape called for.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-140101`](../tasks/TASK-140101-the-bank-of-the-seat-the-clock-does-not-name-is-whole-seconds.md) | XS | `Math.ceil` at `turn-clock.ts:105`, and two tests at `seatClock` on a **fractional** fixture. A third `verify:` command deletes the `Math.ceil` again, requires the two tests to go red printing `1:58.623999999999995`, and restores the file — so the test is *proved* to have detected the defect rather than asserted to |

**A second ticket was considered and refused.** The three other places that format a countdown —
`bankFigure`'s second call site and both of `clockFigure`'s — were measured before splitting: all
three pass `secondsRemaining(...)`, which is already whole. There is nothing for a second ticket to
repair, and inventing one to look thorough costs a dispatch for nothing.

## Acceptance criteria

- [ ] With `bankRemainingMillis` holding values that are **not** multiples of 1000, `seatClock`
      returns a bank string matching `^\d+:\d{2}$` for **both** seats — the seat the clock names and
      the seat it does not — using **two different** fractional values, so that a constant or a
      hard-coded seat index fails
- [ ] The ticket's report records the string the new assertion produces **before** the fix on
      `develop` (`1:58.623999999999995` for `118_624` ms at the seat the clock does not name) and
      after it (`1:59`), so the test is known to have failed for the reason it names
- [ ] A bank of 1 ms reads `0:01` and a bank of 0 ms reads `0:00`, so a live bank is never drawn as
      an exhausted one (`ADR-0108` §5)
- [ ] `bankFigure`'s and `clockFigure`'s signatures, KDoc and behaviour for whole-second inputs are
      unchanged, and every call site passes whole seconds
- [ ] No existing assertion in `web-client/src` is deleted, weakened or rewritten
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0

## Out of scope

- **The action bar's `off` state** — the second annotation on `edits3.png` (*show disabled
  controllers*) was `DEC-135`'s, and it is **answered *no*** by
  [`ADR-0127`](../../docs/adr/ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md):
  a control stands only for a decision the server has opened, so the `off` state keeps its box, its
  reserved rows and its one sentence. `STORY-1406` is retired rather than split, and nothing about
  the bar is owed by this story or any other.
- **The pot's chip pile and the arrow drawn from it** — item 2c, `STORY-1413`, and `ADR-0115` and
  `ADR-0102` govern it.
- **The countdown figure.** Both of `clockFigure`'s call sites already pass `secondsRemaining(...)`;
  nothing about the clock's own numeral is in question.
- **Where the bank is drawn, what it is labelled, and its treatments.** `ADR-0108` §5 and the merged
  component card own the word `Timebank`, the `m:ss` shape and the four treatments. This story
  changes one argument, not one pixel of the drawing.
- **The phone fit.** The same plate is clipped at the screen edge in `mobile.jpeg`; that is
  `DEC-136`'s and `STORY-1405`'s, and it is not fixed by a shorter string.
