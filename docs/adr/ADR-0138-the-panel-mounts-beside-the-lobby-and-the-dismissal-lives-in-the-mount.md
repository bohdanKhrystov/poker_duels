# ADR-0138 — The panel mounts beside the lobby, and the dismissal lives in the mount

- **Status:** Accepted
- **Date:** 2026-09-07
- **Resolves:** `DEC-144` — **by what mechanism does a surface render above every chosen screen,
  and where does its dismissal live?** Registered open 2026-09-06 by
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §9, in five parts: where the
  panel is mounted; how it meets [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md)'s
  `shown`/`ruling` computation and the layout-effect restore; where a dismissal lives so that it
  survives a screen change but **not** a reload; whether the panel and `RematchControl` are one
  component or two; and how the surface is announced without **taking focus**.
- **Serves, and does not reopen:** `ADR-0123` §§1–7. **Every** clause of what a player sees is that
  ADR's and is a constraint this one is written inside — which screens the panel follows (§1), that
  it is never modal (§2), incoming-only and never beside the result screen (§3), its words and its
  two controls (§4), that it never retires itself (§5), that the accept is already shipped (§6), and
  that a dismissal hides the surface and never the offer (§7). None of them is re-argued here, and
  §8's card keeps every question of dress.
- **Applies, and amends nothing:** `ADR-0114` §§1–3 (one predicate above every branch, the branch
  order, and the layout-effect restore — `Lobby.tsx`'s cascade is **not edited**);
  [`ADR-0112`](ADR-0112-only-a-running-duel-refuses-another-screen.md) §§2–4;
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §§3–6 (an idempotent offer,
  the agreement with no frame of its own, silence as the decline, `UNKNOWN_ROOM` as the frame that
  ends a rematch);
  [`ADR-0115`](ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
  §§2–4 (the sheet's one reduced-motion block reaches this surface with nothing to re-decide);
  [`ADR-0127`](ADR-0127-a-control-stands-only-for-a-decision-the-server-has-opened.md) §1 (a control
  is drawn only for a decision the server opened — §4 below applies it to the panel's own buttons);
  [`ADR-0118`](ADR-0118-a-recovering-browser-shows-nothing-it-was-not-told.md) §1;
  [`ADR-0100`](ADR-0100-the-driver-reaches-an-amount-by-pressing-what-a-player-presses.md) §5 (no
  `data-testid`, no test-only prop);
  [`ADR-0086`](ADR-0086-the-offers-answer-is-one-key-owned-beside-the-predicate-it-feeds.md) §2 (the
  reach for `localStorage` lives in `main.tsx` — §3 below never reaches at all);
  [`ADR-0032`](ADR-0032-react-subscribes-to-a-store-it-does-not-own.md) §§1–3;
  [`ADR-0091`](ADR-0091-design-gets-no-agent-a-new-screen-owes-a-card.md) §§2–3.
- **Moves nothing outside the client, and nothing crosses the socket.** No wire type, no
  `PROTOCOL_VERSION`, no server file, no engine change, no schema, no stored key, and no new
  player-facing string. §7 is the load-bearing claim for the plan: **`STORY-1415`'s implementing
  ticket is not `atomic:`.**
- **Registers no decision.** Two fences are named rather than registered, on `ADR-0105` §6's route
  (§9).

## Context

### What is merged, measured rather than recalled, on `develop` at `c54b4a1f`

**The tree above the screens is nine lines.** `App.tsx` renders one `<main>` and one `<Lobby />`,
and holds nothing else. Every provider — signed-in, account, profile, set-name, history, ladder and
the duel store — is mounted in `main.tsx:274-297`, **above** `<App />`, unconditionally and for the
life of the document.

**`Lobby.tsx` is a cascade of early returns.** One block computes `ADR-0114`'s predicate above
everything (`:82-84`), two effects follow (`:99-104` passive, `:120-123` the layout restore), then
six chosen-screen branches return (`:131`, `:142`, `:166`, `:190`, `:208`, `:237`), then the three
store branches (`:255` result, `:297` table, `:373` waiting room), then `ADR-0118`'s empty render
(`:385`), then the front door. **Nothing rendered by that function survives a change of screen**:
every branch returns a different tree, and React discards the state of everything in the branch it
leaves.

**The fact is already everywhere.** `RematchOffered` accumulates into `state.rematchOffers`
(`duel-state.ts:374-381`) with no reference to any screen, and is cleared only by a `Snapshot`
(`:303-305`) and by `DuelFinished` (`:358-359`). `Failure` sets `refusal` and clears no offer
(`:369-373`), which is what lets a refused press leave the panel standing. `rematchStand`
(`result/rematch-stand.ts`) already turns the array plus `mySeat` into `mine`/`theirs` and already
answers `false`/`false` for a client holding no seat.

**The client has never drawn anything above anything.** `createPortal` appears nowhere, no
component is `position: fixed`, and there is no `z-index` in the client at all. The one positioned
element is `PlayingCard.tsx:22-37`, whose two rank glyphs sit `absolute` inside the card's own
`relative` box — local layout, not an overlay. The vocabulary anticipates one: `tokens.css:117`
names `--pd-shadow-pop` *"for what floats (menus, dialogs)"* and nothing uses it. Reduced motion is already global and automatic —
`tokens.css:149-156` stills every `animation` and every `transition` in the document, so a surface
written later is covered without a line of its own (`ADR-0115` §4).

**Announcement has one shipped shape and seven instances.** `role="status"` on a `<p>` rendered
only when there is something to say — `SignInForm`, `SignUpForm` (twice), `ForgotPasswordForm`,
`RecoveryEmailForm`, `RevokeControl`, `NameSurface`. There is no `aria-live` attribute anywhere and
no focus call anywhere.

**`Not now` is on a module that is being deleted.** `account-offer-text.ts:16` holds
`OFFER_DISMISS = "Not now"`, and [`ADR-0125`](ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
retires the post-win account offer whole. `STORY-1408` takes that module with it.

### What is actually in tension

**Three requirements pull the mount point in three directions, and only one point satisfies all
three.**

*The dismissal has a lifetime nothing in the client currently has.* `ADR-0123` §7 wants it to
survive a screen change and die on a reload, and it mints no storage key. The two obvious homes are
both wrong for a reason that is easy to miss: React state **inside** the cascade dies the moment
the player walks back to `/` and out again — which is one press of *Back* — and storage of any kind
is forbidden outright. What is left is state held above the cascade, in something that never
unmounts.

*The rule that there are never two live surfaces is a statement about what rendered.* `ADR-0123` §3
says the panel is on screen "precisely when a chosen screen is — the case `ADR-0114`'s `shown`
already names". But `shown` names the ask's **ruling**, not the render: `ADR-0114` §2 keeps a
deliberate fall-through, where `shown === "duels"` with no history read available falls past its
branch into the room's own screen. On that path `shown` names a chosen screen while
`RematchControl` is on the result screen underneath, and a panel gated on `shown` alone would be
the second surface §3 forbids.

*Most of `ADR-0123` §2 is a list of things the surface must not do.* Nothing beneath it scrimmed,
disabled, or moved; no focus taken; no keypress the player was already making able to answer it; no
timer. A prohibition that lives only in prose is a prohibition the next person breaks — and each of
these can be made structural or left to a reader. Which of them can be made structural is the part
of this decision worth having.

### The deadline

Two, and they point the same way. **This is the product's first surface above another screen**, so
whatever mounts it becomes the shape the next notice copies — `ADR-0123` books that as a cost and
asks that the citation be visibly not an answer. And **the card is minting work with the human**
(`ADR-0123` §8): a mechanism that fixes the panel's markup or its dress is an ADR drawing the card,
which is exactly the order `ADR-0091` §3 forbids. The mechanism has to be decided now and has to
leave the drawing untouched.

## Decision

### 1. `RematchNotice` is mounted by `App`, beside `Lobby`, and takes no props

A new component, `web-client/src/result/RematchNotice.tsx`, rendered by `App.tsx` as the **last
child of `<main>`, after `<Lobby />`**:

```tsx
<main className="min-h-screen bg-bg font-ui text-text">
  <Lobby />
  <RematchNotice />
</main>
```

That is the whole of `App.tsx`'s change: one import and one element. **`Lobby.tsx` is not
edited** — not its cascade, not its branch order, not either of its effects, not the block that
computes `standing`, `ruling` and `shown`.

`RematchNotice` **takes no props**. It reads `useDuelState()`, `useSend()` and `useScreen()` for
itself. Props would have to be handed down by `Lobby`, and being rendered by `Lobby` is the one
thing this component cannot be.

Four things follow from the position, and each is the reason for it rather than a bonus:

- **It is never unmounted by a screen change.** The address moves, `Lobby` re-renders a different
  branch, and this sibling keeps its identity and its state — including on a return to `/`, which
  is where a panel mounted inside the cascade would lose the dismissal (§3).
- **It is inside every provider**, because `main.tsx` wraps `<App />` in all of them. Nothing is
  plumbed, no context is added, and no provider moves.
- **It is outside every `<form>` on every screen.** The sign-in form, the recovery form, the reset
  form and the front door's room-code form are all inside `Lobby`'s subtree; a control that is not
  a descendant of a form cannot be its implicit submit target. `ADR-0123` §2's *no keypress the
  player was already making can answer it* is therefore true by position, not by remembering to
  write `type="button"` — which the panel's controls also do.
- **It registers no effect of any kind.** No `useEffect`, no `useLayoutEffect`, no `setTimeout`, no
  `setInterval`, no subscription of its own beyond the hooks above. This is what makes
  `ADR-0123` §5's *never retires itself on a timer* structural, and it is what makes §2 below safe:
  a component with no effect cannot write the address and cannot race the restore.

### 2. It reads `ADR-0114`'s predicate and writes nothing; the gate is `shown` and the fact

`RematchNotice` computes, from the same two pure functions `Lobby` uses and in the same render
pass:

```tsx
const standing = roomStanding(state, roomAwaited);
const shown = rulingOn(screen, standing) === "honour" ? screen : "first";
const { theirs } = rematchStand(state.rematchOffers, state.mySeat);
```

and renders the panel when **`shown !== "first"` and `theirs`** and it has not been dismissed
(§3) — and `null` otherwise. It never calls `open`, `leave` or `clearToken`, and it never touches
`window.location`.

**This is a second *call* of one predicate, not a second predicate.** `roomStanding` and `rulingOn`
are pure functions of `(state, roomAwaited)` and `(screen, standing)`; both callers read the store
through `useSyncExternalStore` and the address through `use-screen.ts`'s single module-scope
subscription, so a notification re-renders both in the same batch, from the same snapshot, in the
render that commits. There is one implementation, in one file, and `ADR-0114` §1's *one pure module
reads the room and rules on the ask* is unchanged. What would have violated `ADR-0114` is a second
**rule** — a bespoke test of `state.outcome` or of the address — and there is none.

**Everything about the layout-effect restore follows from writing nothing.** The notice cannot fire
on the commit that refuses an ask, because it has no effect to fire; it cannot write the address
over `leave()`'s restore, because it never writes the address; and it does not need to know the
restore exists. On the commit where the agreeing `Snapshot` arrives, `standing` becomes `running`,
`rulingOn` refuses the chosen screen, `shown` becomes `"first"` and this component returns `null` —
in the **same render** in which `Lobby` renders the table and its layout effect restores `/` before
the paint. No frame of the product ever shows a rematch panel over a duel table.

Three cases fall out of the gate rather than needing a clause:

- **A running duel** never carries the panel: `rulingOn` answers `refuse` for every chosen screen
  while `standing` is `running`, so `shown` is `"first"` (`ADR-0112` §2 is untouched).
- **A store that has been told nothing** carries no panel: `theirs` is false on an empty
  `rematchOffers`, so `ADR-0118` §1 holds by construction, and the panel is gated on **the fact the
  server sent** rather than on a standing derived from it. One fact, one gate: a second, derived
  term could only ever withhold something the server had said.
- **A player who left the room** is followed nowhere: *Back to the lobby* is an `<a href="/">`
  (`DuelResult.tsx:66-72`), so the document is replaced and the store dies with it.

**The one place `shown` is not the render, named and bounded.** `ADR-0114` §2's fall-through fires
when a chosen branch's guard is false — `read`, `readLadder` or `account` being `null`. Each of
those is `null` only when its provider is absent, and `main.tsx` mounts all three above `<App />`
for the life of the document, so **in the shipped client `shown !== "first"` is exactly "a chosen
screen is rendering"**. Mounting the notice in `App` rather than in `Lobby` also contains the
divergence in tests: a test that renders `Lobby` without a provider renders no notice at all. §9
states the fence this leaves.

### 3. The dismissal is one boolean in `RematchNotice`, and its lifetime is the mount

```tsx
const [dismissed, setDismissed] = useState(false);
// ADR-0123 §7: a dismissal lasts as long as the offer it was about, and the
// offer ends when the duel that answers it begins (Snapshot) or when the room
// hands down another result (DuelFinished). Cleared here, in the render that
// first sees the offer gone, so the next offer is not swallowed by the last
// dismissal.
if (dismissed && !theirs) setDismissed(false);
```

**Where it lives is the whole answer.** Because §1 mounts the component above the cascade, this
`useState` survives every screen change, including a walk back to `/` and out again, and dies with
the document — which is `ADR-0123` §7's *"a reload brings the panel back"*, obtained with **no
storage key, no context, no provider, no store field and no module-scope variable**. `ADR-0086`
§2's rule that only `main.tsx` reaches for `localStorage` is not tested here, because nothing
reaches.

**The reset is not a nicety.** Offers clear and are made again — a rematch is played, it finishes,
and the rival offers a second one. A dismissal that outlived its offer would mean a player who
pressed *Not now* once never saw another rematch offer for the life of the tab, which is
`ADR-0123` §7's *"hides the surface, never the offer"* inverted into hiding the offer. The clear is
written as a conditional set **during render**, React's documented way to adjust state when the
thing it was derived from changes: it is guaranteed to terminate (after the set the condition is
false), it is `StrictMode`-safe because it is pure, and it costs no effect — which keeps §1's *no
effect of any kind* true.

**The dismiss sends nothing.** `onClick` calls `setDismissed(true)` and returns. There is no
`send`, no fetch, no storage write and no callback out of the component; `ADR-0044` §6's *silence
is a decline* is preserved by there being no code that could break it.

### 4. Two components, one set of words — and the panel's buttons obey `ADR-0127`

**Two components.** `RematchControl` keeps its file, its five states, its markup and its dress
exactly as merged, and continues to be constructed only by `Lobby.tsx:262`. `RematchNotice` is its
own surface with its own shell. One component with a `variant` prop was the alternative, and it is
rejected below: the panel must render three of the five states and must never render the other two,
and a prop that must not take one of its values is a rule written where nothing enforces it.

**One set of words.** A new module `web-client/src/result/rematch-text.ts` holds the four things
both surfaces state, and **both** import them:

| Constant | Value |
| --- | --- |
| `RIVAL_OFFERS` | `Your rival offers a rematch` |
| `REMATCH_LABEL` | `Rematch` |
| `DEALING_LEAD`, `DEALING_TAIL` | `Rematch. The button changes sides —` and `dealing hand 1…` |
| `ROOM_GONE` | `That duel room is gone.` |

`RematchControl`'s edit is **imports only**: the same words, in the same nodes, with the same
`<br />` between the dealing sentence's two lines — which is why that sentence is two constants and
not one string with a newline in it. This is the half of `ADR-0123`'s *"only prose holds them
together"* that a compiler can hold instead; the other half, the two surfaces' **states**, is held
by a test (§8) and is honestly still prose.

**`Not now` is copied, not imported.** The panel declares its own constant with the same value.
Importing `account-offer-text.ts`'s `OFFER_DISMISS` would tie a rematch panel's label to the
account offer's copy, so that changing one silently changed the other — and `ADR-0125` deletes that
module with the offer it belongs to, so the import would also break `STORY-1408`. The rule the two
cases make together: **share a constant when it is one fact stated twice; copy the word when it is
two facts that happen to agree.**

**The panel's own controls, under `ADR-0127` §1.** `Rematch` is drawn only while the rival's
standing offer is a decision this client has been told is open, and it is **never drawn faint,
disabled or as a placeholder**. Its three states are the three the merged control already has, and
the panel states them the same way: the offer standing (the line and the button); the accepted span
(`ADR-0044` §4 sends no frame that says both agreed, so the panel shows the dealing sentence in
place of the button, held in its own `useState` exactly as `RematchControl.tsx:32` does); and
`refusal === "UNKNOWN_ROOM"`, where the button is **retired** and the sentence stands in its place.
The dismiss stands in all three, because it asserts nothing about the game — it is the only way off
a panel that `ADR-0123` §5 says never leaves by itself — and `ADR-0127` §1 governs controls that
state a game fact.

### 5. The panel is out of flow, and what `ADR-0123` §2 forbids is forbidden structurally

The panel's root is **taken out of the document flow** — `position: fixed` — and the reason is not
appearance. An in-flow panel would move the screen beneath it at the instant it arrived: the form a
player is typing in would jump under their hands. That is taking something from the player as
surely as a scrim is, and out-of-flow is what makes `ADR-0123` §2's *"the player may go on doing
what they were doing"* literally true rather than nearly true.

What the panel renders is bounded by its own box. Specifically it renders **no scrim**, sets no
`inert`, sets no `aria-hidden` on anything it does not own, locks no scroll on `<body>` or
`<html>`, and paints over and intercepts pointer events from **nothing outside its own box** — a
transparent positioning wrapper is permitted only if it passes pointer events through. Nothing
beneath it is disabled, and no element of the screen underneath is touched, because the panel is
not a parent of any of them.

**§1's mount is what makes `fixed` mean the viewport.** A `transform`, `filter` or `contain` on any
ancestor turns a fixed element's containing block into that ancestor — the standard way a panel
ends up positioned against a screen instead of the window. The panel's only ancestors are `<main>`
and `<body>`, and neither is transformed; the one `transform` in the client is the chip flight's
(`app.css:129`), which lives inside `Lobby`'s subtree and is an ancestor of nothing here.

**Its shape, its place on the viewport, its width, its colours and any entrance are the card's**
(`ADR-0123` §8), and this ADR fixes none of them. Two constraints the card is written inside, both
consequences of decisions already merged: the panel is bounded to the viewport width, since
`ADR-0121`'s fit axis is `scrollWidth ≤ clientWidth`; and any entrance is **CSS only**, built from
`--pd-motion-*` tokens, because `tokens.css:149-156` stills every animation and transition under
`prefers-reduced-motion` in one place (`ADR-0115` §4) — a JavaScript-driven entrance would escape
it. `ADR-0103`'s height budget is untouched either way: the panel never renders over the table,
which is `shown === "first"`.

### 6. It is announced by `role="status"` on its own root, and it takes no focus

The panel's **root element** carries `role="status"`. That is the shipped precedent in seven
places, and putting it on the root rather than on an inner line is what makes the three states of
§4 announce as they replace one another — `role="status"` implies `aria-live="polite"` and
`aria-atomic="true"`, so the panel is read as a whole when it arrives and re-read when its content
changes to the dealing sentence or to *That duel room is gone.*

**Polite, and nothing else.** Not `role="alert"`, which is assertive and interrupts whatever the
player is being read — the audible form of seizing the screen. Not `role="dialog"` or
`role="alertdialog"`, which tell assistive technology this is a dialog and put several screen
readers into a mode that treats what is beneath as unavailable; `ADR-0123` §2 says it is not a
modal, and the accessibility tree must not say it is.

**Nothing takes focus.** No `autoFocus`, no `.focus()`, no `tabIndex` on the panel or its root, and
no focus trap. The panel's two controls enter the tab order naturally, in DOM order — which, since
the panel is the last child of `<main>`, is **after** everything on the screen beneath. A keyboard
player therefore reaches the panel last and nothing they were already doing is displaced; that is
the trade, and it is named in Consequences. No key handler is added anywhere: no `Escape` to
dismiss, no `Enter` to accept, nothing on `document`. A keypress can reach the panel only after the
player has deliberately focused one of its controls.

### 7. Nothing crosses the socket — the load-bearing claim for the plan

The accept sends `{ type: "OfferRematch" }`, the frame `RematchControl` already sends and the only
frame this ADR causes to be sent. The dismiss sends nothing. No declaration in `protocol.gen.ts`
changes, so `ADR-0047` §2's fingerprint cannot move, `PROTOCOL_VERSION` does not move, no server
file is opened and `ADR-0047`'s one-bumping-branch-at-a-time lock is not claimed. No schema, no
migration, no engine file. **`STORY-1415`'s implementing ticket is not `atomic:`** under `ADR-0068`
§3, and the story serialises against nothing in `EPIC-14`.

The files this decision reaches, whole: `web-client/src/App.tsx` (one import, one element),
`web-client/src/result/RematchNotice.tsx` (new), `web-client/src/result/rematch-text.ts` (new),
`web-client/src/result/RematchControl.tsx` (imports only, same DOM) and tests. `Lobby.tsx`,
`use-screen.ts`, `screen.ts`, `room-standing.ts`, `rematch-stand.ts`, `duel-state.ts`,
`duel-store.ts`, `boot.ts` and `DuelResult.tsx` are **not edited**.

`STORY-1415` still cannot start before its **card** merges: `ADR-0123` §8 makes it minting work
with the human, and §5 above deliberately leaves the drawing to it.

### 8. What a test must prove

`ADR-0100` §5 in full — no `data-testid`, no test-only prop, no exported setter. The cross-screen
proofs render **`App`**, not `Lobby`, because the panel is `App`'s child; `App.test.tsx` already
mounts that tree with a store and the provider stack. The address moves by assigning
`window.location.hash`, which is what a player's own navigation does.

- **It follows onto every screen, not onto one.** The panel stands on each of `duels`,
  `leaderboard`, `account`, `sign-in`, `verify` and `reset` with an incoming offer standing. One
  screen cannot tell *follows every screen* from *is on the account screen*; `ADR-0123` §1 says no
  carve-out, so the test enumerates.
- **One fact, one surface.** On `/` with the same offer, the offer's sentence appears **exactly
  once** in the document — a count, not a presence check, because presence is what two surfaces
  also satisfy.
- **Incoming only, proved with two seats.** `mySeat: 1` with `rematchOffers: [0]` shows the panel;
  `mySeat: 0` with `rematchOffers: [0]` shows nothing. The same array in both, so a seat hard-coded
  anywhere cannot pass both.
- **The dismissal survives a screen change and a trip through `/`.** Dismiss on `#/duels`, go to
  `#/account` — hidden; go to `/`, then back to `#/account` — still hidden. **The trip through `/`
  is the assertion that discriminates the mount point**: a panel mounted inside the cascade passes
  the first half and fails the second.
- **It does not survive a reload, and nothing is stored.** A fresh `render()` of the same tree with
  the same offer shows the panel again, and the set of keys in the injected `Storage` is unchanged
  across a dismissal.
- **The dismissal ends with its offer.** Dismiss, deliver a `Snapshot` and a `DuelFinished`, then
  deliver a fresh `RematchOffered` from the rival's seat: the panel is back.
- **Nothing is sent by a dismissal.** The count of messages sent is unchanged across the press —
  the only assertion that catches a decline frame being added later. The accept sends exactly one
  `OfferRematch`.
- **A keypress the player was already making does not answer it.** With the panel up on
  `#/sign-in`, a real `Enter` in the password field submits that form and sends **zero**
  `OfferRematch`; the same on the front door's room-code field.
- **It takes no focus and disables nothing.** Focus a control on the screen beneath, then deliver
  the offer: `document.activeElement` is the same element, and the screen's own controls are still
  enabled and still accept typing. Pre-focusing is what stops this assertion being vacuous.
- **It never retires itself.** With fake timers and the offer standing, advancing the clock leaves
  the panel exactly where it was.
- **The agreement takes the screen with no panel over it.** After the agreeing `Snapshot` the
  document holds the table, holds no panel, and the address is `/`.

### 9. What follows from the rule as written, named rather than registered

On `ADR-0105` §6's route — stated so a future reader finds them, blocking nothing today:

- **`shown !== "first"` means "a chosen screen is rendering" only while every chosen branch's guard
  is a provider that is always mounted.** A future screen whose branch can fall through at runtime
  — a guard on a value that goes false while the app runs — would put the panel over the room's own
  screen. The repair is small and known (that screen's availability joins the computation of
  `shown` in `Lobby.tsx`, in the one place it is computed), and it becomes owed on the day such a
  screen is written, not before.
- **A `role="status"` region inserted with its content is announced by every screen reader this
  product has been checked against, and the spec-safe alternative is a region mounted empty
  beforehand.** The seven merged instances all insert with content; this one matches them rather
  than inventing a second pattern for one surface. If a UAT round reports the panel unannounced,
  the fix is an always-mounted empty region in `App`, and it is a defect ticket rather than a
  decision.

## Consequences

**What it buys.** `ADR-0123` §§1–7 are implementable without touching `Lobby.tsx` at all: the
branch order `ADR-0114` made mechanical, the two effects whose interaction that ADR spent four
bullet points getting right, and `ADR-0112` §4's restore are all left exactly as merged and cannot
be broken by this work. The dismissal's odd lifetime — survives a screen change, dies on a reload —
turns out to be the natural lifetime of a component mounted beside the thing that changes, so it
needs no key, no store field and no context. Three of `ADR-0123` §2's prohibitions stop being prose
and become properties of where the component sits: it cannot be a form's submit target, it cannot
retire itself on a timer it does not have, and it cannot move the screen underneath because it is
out of flow. And the panel's dress is left entirely to the card the human draws.

**What it costs.**

- **A second call site for `ADR-0114`'s predicate.** One implementation, two callers; if a future
  screen needs a rule that is not in `rulingOn`, there are now two places that would have to learn
  it. §2's fence is the only thing that keeps that honest.
- **`shown` is being used for something it does not exactly mean** (§9's first fence). It is exact
  in the shipped client and inexact in principle, and the gap is a fall-through `ADR-0114` §2 put
  there deliberately.
- **A keyboard player reaches the panel last**, after every control on the screen beneath, because
  DOM order is the tab order and the panel is the last child of `<main>`. Visually it will be one
  of the first things on the screen. That mismatch is the standard price of a non-modal notice and
  it is not free: a player who never tabs to the end of a long screen may never reach *Rematch*
  with the keyboard alone, and must reach for the pointer.
- **`App.tsx` stops being trivial.** It has been nine lines with no logic since the client existed;
  it now decides that one surface is above the screens. The next notice will want the same line,
  and `ADR-0123`'s named cost — the product acquiring an overlay pattern — arrives here in concrete
  form. Deliberately **not** mitigated by a `notice/` module or a generic host: the panel lives in
  `result/` beside the control it shares its words with, so the next notice has to bring its own
  decision instead of inheriting a home.
- **The two surfaces' states can still drift.** The words are now compiler-held; the *set of
  states* each surface draws is held by a test and by §4's prose. `RematchControl` can gain a sixth
  state that the panel never learns about.
- **`RematchControl` is edited for a reason that is not its own.** Four literals become imports, so
  its file changes in a PR about a different surface, and its own tests keep their literals (a test
  that imported the constants would assert that a constant equals itself).
- **A conditional `setState` during render** is correct and documented, and it reads like a
  mistake. It costs a comment forever, and a future refactor that "cleans it up" into an effect
  reintroduces an effect this design says it has none of.
- **Nothing proves the absence of a scrim except a reader.** §5 lists what may not be rendered;
  only the pointer-events and enabled-controls assertions in §8 test any of it, and a scrim added
  later with `pointer-events: none` would pass them.

**What it forecloses.** Nothing structurally, and every rejected shape below stays one client-only
PR away. It deliberately does **not** build: a general notice host or notice queue, a second notice
of any kind, a persisted dismissal, a focus-management scheme, an in-product motion or notification
setting, and any change to what `ADR-0123` §§1–7 say a player sees.

## Alternatives considered

**Mount the panel inside `Lobby`, in each of the six chosen-screen branches.** The strongest case,
and it nearly wins: it is the only shape in which "the panel shows" and "a chosen screen rendered"
are literally the same decision, so `ADR-0114` §2's fall-through cannot produce two surfaces and
§9's first fence would not exist; it needs no second call of the predicate; and it keeps every
surface inside the one file that owns what a screen is. Rejected on `ADR-0123` §7: state held
inside the cascade dies when the player walks back to `/`, so a dismissal would be undone by one
press of *Back* and the panel would return over the next screen the player opened. Splitting the
state upward while leaving the surface down there — a context or a provider for one boolean — buys
back the lifetime at the cost of a provider, an inverted data flow, and six mount sites that a
seventh screen must remember to add, which is `ADR-0123` §1's *no carve-out* held by prose again.

**Collapse the six branches into one, so there is one chosen-screen return to wrap.** Its case is
real: the six branches are the same `<section>`, the same `Back` button and the same shape, so one
`ChosenScreen` wrapper would give one mount point, would make the fall-through visible as a `null`
return, and would delete repetition that is genuinely repetitive. Rejected because it is a refactor
of the most heavily tested and most recently argued code in the client — `ADR-0114` §2's order is
load-bearing and its guards narrow types — undertaken in a PR about a different surface, and
because it still loses the dismissal on a trip through `/`. It remains available on its own merits
as its own ticket.

**Hold the dismissal in the duel store.** Its case: the store is already the one thing that
outlives every screen, `useSyncExternalStore` already distributes it, and the reset could be folded
into the reducer's existing `Snapshot`/`DuelFinished` cases, where the offer is cleared — one
place, one rule, no render-phase set. Rejected because `ADR-0123` §10 says in as many words that
the store gains no state, and because it is the wrong kind of fact: the store holds what the server
said, and "this browser is not showing a panel right now" is not something the server said. A
reducer that carries view state is the first step toward a reducer that carries all of it.

**Key the panel on the offer to reset the dismissal, instead of clearing it during render.**
`<RematchNotice key={String(theirs)} />` remounts the component whenever the rival's offer arrives
or ends, and fresh state is `false` — one line, no conditional set, no comment about React
semantics. Rejected because the key would have to be applied in `App`, which would then be
computing `theirs` — pulling store knowledge into the file whose whole job is composition — and
because a boolean key is a trick: it works only because two offers are always separated by a clear,
which is true today and is written down nowhere.

**One component with a `variant` prop, `"result"` or `"panel"`.** Its case is the strongest
argument against this ADR's own named cost: one file, one set of states, and the drift `ADR-0123`
books as a consequence simply cannot happen. Rejected because the panel must render three of the
five states and must **never** render the other two — `Rematch offered — waiting for your rival`
and the bare `Rematch` — so the prop would carry a rule that nothing enforces, and a future state
added to the shared component would appear on the panel by default rather than by decision. It
would also tie the panel's dress to the result screen's at exactly the moment the human is drawing
the panel's card, which `ADR-0091` §3 puts in the human's hands.

**Nest `RematchControl` inside the panel's shell.** Better than the variant prop: no new states, no
duplicated markup, and the three states the panel needs are the three that control already draws
for an incoming offer. Rejected for the same card reason and one worse — the panel's shell could
only wrap the merged control, never restyle it, so a card that draws a differently-weighted button
inside the panel would force a dress prop into a merged component after the fact. Reversing this
ADR into that shape is easy; reversing that shape back out is a component with two masters.

**Render the panel through a portal into `document.body`.** Its case: it is the conventional way to
put a surface above an application, it escapes any `overflow` or stacking context a screen might
introduce later, and React keeps the component in the tree so the state lifetime is identical.
Rejected because it buys nothing today: the panel's only ancestors are `<main>` and `<body>`, and
neither is positioned, clipped or transformed, so a `fixed` root is already positioned against the
viewport and there is nothing to escape (§5). It costs the panel its place in the accessible tree
under `<main>` and adds an API whose next user will use it for a modal. If a clipping or
transformed ancestor ever appears above `<main>`, a portal is one line away.

**Give the panel no `role` and let the visible change speak.** Its case: `role="status"` on a
container with buttons is a live region wrapping interactive content, which some screen readers
handle awkwardly, and a player who cannot see the panel can still find it by tabbing. Rejected
because a surface whose entire purpose is that it *cannot be missed* is exactly the surface that
must announce, and the seven merged `role="status"` instances make it the pattern rather than an
invention. The awkward case — a live region containing controls — is bounded here: the panel is two
controls and one sentence, and it is announced once when it arrives.
