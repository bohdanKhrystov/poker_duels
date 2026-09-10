---
id: STORY-1415
title: The rematch offer finds the rival wherever they are
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, design, rematch, notice]
depends_on: []
---

## Goal

A player who walked off the result screen — to the ladder, to their record, to their account, to
sign-in, to a mailed screen — sees their rival's standing rematch offer arrive as a **panel above
the screen they are on**, answers it with one press, and can set it aside without answering it.
Nothing beneath the panel is scrimmed, disabled, focused away or moved, and no keypress the player
was already making can accept a duel.

## Why

**It is `EPIC-14` item 5, and the surface is the whole of it.** `ADR-0112` §3 let a finished player
walk off the result screen; `RematchControl` is constructed in exactly one branch of `Lobby.tsx` and
appears in no other file, so the product now invites a player to leave the only screen a rematch can
be seen on. A rival offering into that absence is offering into nothing, and a `FINISHED` room is
reaped after five minutes.

The fact is already on every screen and always was: `RematchOffered` accumulates into
`state.rematchOffers` (`duel-state.ts:376-383`) with no reference to which screen is showing.
**Nothing has to reach the client that does not already reach it** — this story is a surface and
nothing else.

## Design notes

Both sources are merged and neither is re-derived here.

- [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) is the
  product answer to `DEC-138`, the human's own words recorded rather than decided: *"when rematch
  offered it shoud be shown as popup; it shoud appear on any screen, like if opp come back to lobby
  it or any other page in still shoud be shown"*. §1 follows every `Screen` member but `first`, with
  **no carve-out**; §2 declines the half of *popup* that means **blocks**; §3 is incoming-only and
  never beside the result screen; §4 fixes the words and the two controls; §5 says it never retires
  itself; §7 says a dismissal hides the surface and never the offer; §8 keeps every question of
  dress for the card.
- [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
  is the mechanism, answering `DEC-144`. A props-less `RematchNotice` mounted by `App` as the **last
  child of `<main>`**, beside `<Lobby />`; **`Lobby.tsx` is not edited**. The gate is a **second
  call** of `ADR-0114`'s `shown`/`ruling` with **no effect of any kind**. The dismissal is one
  `useState` boolean whose lifetime is the mount, cleared **during render** when the offer ends.
  Three prohibitions hold structurally: outside every `<form>`, no timer, out of flow. `role="status"`
  on the root, never `alert`/`dialog`/`alertdialog`; no focus call, no focus trap, no key handler.
  Nothing crosses the socket, so **no ticket in this story is `atomic:`**.

Everything below was **measured in this worktree on `develop` at `f20d07ed`, 2026-09-09**, not
recalled. Where a number is a specification rather than a measurement it says so.

### The story is eleven tickets and the first two are the card

`ADR-0123` §8 makes the card **minting** — a surface above another screen is visual language this
product does not have — so `ADR-0091` §3 puts it in the human's hands and `ADR-0091` §2 puts it
first. `design/screens/rematch-states.html` exists but is the **result screen's** card and draws
none of this; the panel has no card at all. `TASK-141501` mints it, `TASK-141502` sets it on two
screens at two widths (`ADR-0123` §8's last bullet), and no client ticket may start before both
merge.

### The one place `shown` is not the render — and yes, this split depends on the coincidence

`ADR-0123` §3 equates *shown* with *rendered*; `ADR-0114` §2 keeps a deliberate fall-through where
`shown === "duels"` with `read === null` falls past its branch into the room's own screen. On that
path a panel gated on `shown` alone would stand over the result screen, beside `RematchControl` —
the second surface §3 forbids. `ADR-0138` §9 fences this rather than registering it: each guard is a
provider `main.tsx` mounts above `<App />` unconditionally and for the life of the document, so **in
the shipped client `shown !== "first"` is exactly "a chosen screen is rendering"**.

**This split depends on that coincidence in exactly one place**: `TASK-141508`'s *one fact, one
surface* proof renders `App` under the full provider stack, which is what `main.tsx` ships, so it
exercises the coincidence rather than the general case. No ticket here asserts the fall-through
either way — asserting it would pin the divergence as intended. `ADR-0138` §9's repair becomes owed
on the day a chosen screen's guard can go false at runtime, and no such screen exists.

### `react-hooks` and the clear during render — probed, and no suppression is owed

`Lobby.tsx:170-181` carries a documented `// eslint-disable-next-line react-hooks/set-state-in-effect` at `:179`
for its `setHeldPress(null)`, so the question was whether the panel's **render-phase** clear owes the
same. Probed at `f20d07ed`: `eslint-plugin-react-hooks@^7`'s `recommended-latest` set has
**`react-hooks/set-state-in-render` at `error`**, and it is live — an unguarded
`setDismissed(!theirs)` in a render body fails `npx eslint` with *"Cannot call setState during
render"*. The **guarded** form `if (dismissed && !theirs) setDismissed(false);` in the same
component passes with **exit 0**. Both probes were run and both files reverted.

So: **`ADR-0138` §3's conditional set needs no suppression comment**, and a ticket that adds one is
adding noise. It still needs the ADR's own comment explaining why it is not an effect.

### The two booleans, and why both are cleared

`ADR-0138` §3 mandates the clear for `dismissed`, and §4 says the accepted span is *"held in its own
`useState` exactly as `RematchControl.tsx:32` does"*. `RematchControl` never clears `accepted`, and
copying that into a component whose lifetime is the whole document reproduces §3's own named failure
one field over: a player who accepted one rematch would see the dealing sentence in place of the
button on every later offer for the life of the tab. **Both booleans get §3's clear, for §3's stated
reason.** This is the merged rule applied, not a new one, and `TASK-141506` and `TASK-141507` each
name a test for their own.

`RematchControl`'s own stale `accepted` is **out of scope** and named below.

### What was measured

| Fact | Value at `f20d07ed` |
| --- | --- |
| `src/result/RematchControl.test.tsx` | **12** tests, green |
| `src/App.test.tsx` | **36** tests, green |
| `src/lobby/Lobby.test.tsx` | **113** tests, green |
| `App.tsx` | 9 lines: one `<main>`, one `<Lobby />`, no logic |
| `z-index` anywhere in `web-client/src` or `design/` | **0 occurrences** |
| `--pd-shadow-pop` in the product | declared (`tokens.css:117`), mapped (`app.css:73`), used by **no component** |
| `./design/check-drift.sh` | exits 0 — *"565 distinct mentions across 20 cards"* |
| jsdom implicit form submission on `Enter` | **does not happen** — `keyDown`/`keyPress`/`keyUp` on a field inside a `<form onSubmit>` fires the handler **0** times |
| `inset-x-0`, `inset-0`, `top-0`, `bottom-0`, `left-0`, `right-0` in the built stylesheet | **absent** — `app.css:55` sets `--spacing: initial`, so every `-0` spacing utility generates no CSS at all |
| `bottom-5`, `left-5`, `right-5`, `pb-5`, `fixed`, `z-10`, `pointer-events-*`, `shadow-pop`, `bg-surface-raised` | all generate, verified in `dist/assets/*.css` after `npx vite build` |

The `-0` finding is `TASK-140502`'s `min-w-0` defect in a new place, and it is the trap a fixed
panel walks into first. Every placement utility the panel wears must use a **named** spacing step
`1`–`9` or a `[…]` arbitrary value.

The jsdom finding bounds `ADR-0138` §8's *"a real `Enter` in the password field"*: that assertion
cannot be written with `fireEvent`, and a `keyDown` version of it would **pass against a panel
sitting inside the form**, which is the defect it is meant to catch. `TASK-141510` writes the
structural assertion instead — the panel is a descendant of no `<form>` — with `fireEvent.submit` and
a positive control that the form's own submit really ran.

`ADR-0138` §8's *"the same on the front door's room-code field"* is **vacuous by the gate**: the
front door is `shown === "first"`, where the panel never draws. The proof that carries it is
`TASK-141508`'s *one fact, one surface* count on `/`.

### `ADR-0143` governs this surface, and permits its accent

`ADR-0143` §1 is product-wide: nothing marks an act as irreversible by how it is drawn, and nothing
in the assistive register either — **no `role="alert"`, no `aria-live` urgency, no hidden
*Warning:* prefix**. A rematch offer is not an irreversible act, so §2's three obligations do not
bind it and no ticket here adds a second press or a cost sentence. What does bind is §1's list, and
`role="status"` — polite, `ADR-0138` §6's choice — is exactly what it permits.

**The accent is not the danger register.** §1's own measurement names *a standing offer* among the
things colour in this client already says, so the panel may wear `--pd-accent` for what it **is**.
The card ticket states this so the flatness question is not re-litigated at the pane.

### `ADR-0142`'s register is now merged, so the obligation is unconditional

**This changed during the split and the split was re-measured rather than left alone.** When it
began, `develop` was `899d81f7` and `web-client/src/design/card-text.test.ts` did not exist;
`TASK-141203` merged as **`f20d07ed`** while these tickets were being written. Every number below
was re-measured at that head — `RematchControl.test.tsx` 12, `App.test.tsx` 36, `Lobby.test.tsx`
113, `check-drift.sh` at 20 cards, all unchanged — and one ticket changed shape.

`ADR-0142` §6's last test globs `web-client/src/**/*-text.ts` and fails on any module in neither
`PAIRS` nor `NO_CARD`, so **creating `rematch-text.ts` without registering it reddens the suite**.
`TASK-141503` therefore opens three files rather than two and adds the pair
`result/rematch-text.ts → rematch-panel.html`, all six exports `carded`, taking that file from
**7** tests to **10**.

`ADR-0142` §5c matches a carded value against **one** text unit of the card, which is the second
reason `ADR-0138` §4 made the dealing sentence two constants: the card's `<br>` is a tag, §2 replaces
every tag with the separator, and one joined constant would match no unit at all.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-141501](../tasks/TASK-141501-the-card-mints-the-panel-and-its-three-states.md) | The card mints the panel, and draws its three states and the screen without it | backlog |
| [TASK-141502](../tasks/TASK-141502-the-card-sets-the-panel-on-two-screens-at-two-widths.md) | The card sets the panel on two screens, at the phone and on the laptop | backlog |
| [TASK-141503](../tasks/TASK-141503-one-module-holds-the-words-both-rematch-surfaces-say.md) | One module holds the words both rematch surfaces say | backlog |
| [TASK-141504](../tasks/TASK-141504-the-notice-stands-where-an-incoming-offer-stands.md) | The notice stands where an incoming offer stands, and nowhere else | backlog |
| [TASK-141505](../tasks/TASK-141505-the-accept-is-one-press-one-frame-and-the-span-it-opens.md) | The accept is one press, one frame, and the span it opens | backlog |
| [TASK-141506](../tasks/TASK-141506-a-gone-room-retires-the-accept-and-says-so.md) | A gone room retires the accept and says so, and no other refusal touches it | backlog |
| [TASK-141507](../tasks/TASK-141507-not-now-hides-the-surface-and-the-next-offer-brings-it-back.md) | *Not now* hides the surface, sends nothing, and the next offer brings it back | backlog |
| [TASK-141508](../tasks/TASK-141508-the-app-mounts-the-panel-and-it-follows-onto-every-screen.md) | The app mounts the panel beside the lobby, and it follows onto every chosen screen | backlog |
| [TASK-141509](../tasks/TASK-141509-the-dismissal-outlives-the-screen-and-the-agreement-takes-it.md) | The dismissal outlives a walk through the lobby, and the agreement takes the screen | backlog |
| [TASK-141510](../tasks/TASK-141510-what-the-panel-takes-from-the-player-is-nothing.md) | What the panel takes from the player is nothing | backlog |
| [TASK-141511](../tasks/TASK-141511-the-result-screens-card-says-the-words-the-result-screen-says.md) | The result screen's card says the words the result screen says | backlog |

**The mount is the eighth ticket, not the fourth, and that is deliberate.** `TASK-141504`–
`TASK-141507` build the component while `App` renders nothing, so `develop` never carries a mounted
panel a player cannot dismiss. Every cross-screen proof needs the mount and therefore comes after
it.

## Acceptance criteria

- [ ] A player holding a `FINISHED` room, on any of `duels`, `leaderboard`, `account`, `sign-in`,
      `verify` and `reset`, sees `Your rival offers a rematch` and a `Rematch` control when the
      rival's seat has offered, and pressing it sends exactly one `OfferRematch`
- [ ] On `/` the same offer produces that sentence **exactly once** — `RematchControl`'s — and no
      panel
- [ ] A player whose **own** offer stands is followed nowhere: the same `rematchOffers` array shows
      the panel at one seat and nothing at the other
- [ ] `Not now` sends nothing, hides the panel across a screen change **and a trip through `/`**,
      survives no reload, stores no key, and does not hide the **next** offer
- [ ] Delivering the offer moves no focus, disables no control on the screen beneath, and advancing
      any amount of fake time never retires the panel
- [ ] The agreeing `Snapshot` leaves the duel table on screen, no panel anywhere, and
      `window.location.hash` empty — `hashForScreen("first")` is `"/"`, which `replaceState` writes
      as an empty fragment, and `App.test.tsx:967` already asserts exactly that for the same restore
- [ ] `Lobby.tsx` is byte-identical to `f20d07ed`, and `App.test.tsx` and `Lobby.test.tsx` are not
      opened by any ticket in this story
- [ ] `git diff --stat develop -- poker-engine poker-server docs/protocol.md` is empty

## Out of scope

- **Any modal, scrim, focus trap, portal, notice host or notice queue.** `ADR-0138`'s Alternatives
  refuse each by name and each stays one client-only PR away.
- **`Lobby.tsx`, `use-screen.ts`, `screen.ts`, `room-standing.ts`, `rematch-stand.ts`,
  `duel-state.ts`, `duel-store.ts`, `boot.ts`, `DuelResult.tsx`.** `ADR-0138` §7 names them as not
  edited, and `ADR-0114` §§1–3's cascade is the reason.
- **Collapsing `Lobby.tsx`'s six chosen-screen branches into one wrapper.** A real refactor with its
  own merits, refused here because it is a refactor of the most recently argued code in the client
  undertaken in a PR about a different surface (`ADR-0138` Alternatives 2). Not yet ticketed.
- **`RematchControl`'s own stale `accepted`.** Its surface is `/`, where a second `DuelFinished`
  re-renders the same branch with the flag still true. Found here, real, and a different component's
  defect. Not yet ticketed.
- **`ADR-0142` §7's third direction — card text ⇒ module.** Its named trigger has now fired, and
  this split is what fired it: `design/screens/rematch-states.html` says *ImKate offers a rematch*
  and *Rematch offered — waiting for ImKate* where the merged client says *your rival*, and it draws
  no frame at all for `That duel room is gone.` `TASK-141511` repairs the three sentences —
  `ADR-0123` §4 calls *Your rival offers a rematch* *"the result screen's own line"*, so the client
  is right and the card is stale, with nothing to decide. **Whether a register should now catch that
  class is `DEC-158`, the architect's, and it blocks nothing.**
- **A second notice of any kind, a persisted dismissal, an in-product notification setting.**
