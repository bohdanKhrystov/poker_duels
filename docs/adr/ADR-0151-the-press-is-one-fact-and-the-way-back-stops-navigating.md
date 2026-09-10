# ADR-0151 — The press is one fact above the cascade, and the way back stops navigating

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-163` — **by what mechanism does the result screen's *Back to the lobby* leave
  that screen without ending this browser's hold on the room?** Registered 2026-09-10 by
  [`ADR-0150`](ADR-0150-back-to-the-lobby-keeps-the-room.md) §7, which fixed *what a player sees*
  and refused the *how*. All four parts are answered here: **§1** the fact, **§2** the cascade,
  **§3** the control, **§4** the panel's gate.
- **Derived, not transcribed.** Every force below is either a merged ADR or a line of the shipped
  client, and two competent readers of `Lobby.tsx` land in the same place. Nothing here is a
  product call: `ADR-0150` §§1–5 fix what the player sees, and this ADR changes none of it.
- **Applies, and amends nothing in:** `ADR-0150` §§1–6 (the whole of what a player sees);
  [`ADR-0114`](ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md) §1's two pure
  functions, which are **not edited** — `roomStanding` and `rulingOn` keep their bodies, their
  signatures and their meaning, and every term of `roomStanding` is still a fact the server sent;
  [`ADR-0123`](ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§2–7;
  [`ADR-0044`](ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md);
  [`ADR-0072`](ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md) §§3–4 and §6
  (`forgetRoom` keeps its contract and its two remaining callers);
  [`ADR-0073`](ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) and
  [`ADR-0124`](ADR-0124-one-waiting-room-and-play-again-hands-it-back.md) §2 — the waiting screen's
  control is not touched, and goes on being an `<a href="/">` that forgets.
- **Supersedes, on three named sentences,**
  [`ADR-0138`](ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md):
  §1's *"That is the whole of `App.tsx`'s change: one import and one element"* and its *"Nothing is
  plumbed, no context is added, and no provider moves"*; and §2's gate, *"renders the panel when
  **`shown !== "first"` and `theirs`**"*. Everything else in `ADR-0138` stands byte-unchanged and
  this ADR leans on it: the mount position, the props-less component, *no effect of any kind*,
  §2's *second call of one predicate, not a second predicate*, §3's dismissal, §§4–7 and §9's fence.
  §2's third bullet — *"A player who left the room is followed nowhere: Back to the lobby is an
  `<a href="/">`, so the document is replaced and the store dies with it"* — is a statement of
  what `ADR-0150` §1 already reversed; it goes with the anchor in §3 below.
- **Supersedes the second sentence of** `ADR-0072` §5 — *"The anchor stays an `<a href="/">`: no
  `preventDefault`, no `window.location` call"* — **as a statement about the result screen's
  control only**. It stays true of the waiting screen's control of the same name, which §5 below
  leaves alone, and `ADR-0072` §§1–4 and 6–9 are untouched.
- **Moves nothing outside the client.** No wire type, no `PROTOCOL_VERSION`, no server file, no
  engine change, no schema, no stored key, **no new player-facing string**, and no change to any
  word, colour or shape on any screen. `poker-engine` is not opened.
- **Registers, and does not answer:** `DEC-165` — **the product owner's** — which of two merged
  sentences a player reads when the room behind a standing offer is gone and they are standing on
  the front door (§7). It **blocks nothing** here.

## Context

**`ADR-0150` left one debt and named it.** Its Consequences: *"that assumption is `ADR-0124` §2's
`the forget is what keeps the door open`, and for a finished room this ADR takes that job away from
the forget without saying what takes it up. `DEC-163` is that debt, and it is owed before a single
ticket can be written."* Thirteen merged tickets — `STORY-1415`'s panel — ship a surface no press
can produce until it is paid.

**The four forces, each measured on `develop` at `d5ef7df8`.**

**1. Skipping the result branch does not reach the front door; it reaches the table.** `Lobby.tsx`'s
cascade tests raw store fields — `state.outcome !== null` (`Lobby.tsx:340`), then
`state.view !== null` (`Lobby.tsx:362`), then `state.roomCode !== null` (`Lobby.tsx:438`) — and the
reducer clears nothing a frame established, which `Lobby.tsx:338` says in as many words: *"`view`
and `roomCode` both outlive the duel, so a result branch placed after either is a branch that never
runs."* A finished room therefore satisfies **all three** conditions at once. The obvious edit — a
`&& !left` on the first branch — puts the player at the duel table they just finished. This is the
whole reason `DEC-163` is a decision rather than a one-line change.

**2. The way back would undo itself, and it is quietly doing a second job.** `DuelResult.tsx:60-66`
is an `<a href="/">` whose `onClick` is `forgetRoom`; `ADR-0072` §5 chose the anchor deliberately —
*"the navigation stays the browser's"*. Under `ADR-0150` §5's no-key rule a full-page navigation
re-boots the tab, the resume delivers the finished duel's frames, and the player lands back on the
result screen: the press erases its own effect. The second job is invisible until the anchor goes:
`href="/"` **replaces the whole address**, and an invite-link tab's address is `/?room=ABCDEFGH`,
which `main.tsx:176` reads at **every** boot into `joinRoomCode` — and `boot.ts:113`'s
`options.joinRoomCode ?? remembered` gives the link's code priority over the room this tab holds. A
press that stopped navigating would leave that query in the address for the next reload to act on.

**3. Two sibling surfaces need the same new fact, and neither can hold it.** `App.tsx` renders
`<Lobby />` and `<RematchNotice />` side by side (`ADR-0138` §1). `Lobby` decides whether the result
screen shows; `RematchNotice` must know the same thing to obey `ADR-0123` §3's *one fact never has
two live surfaces at once*. A `useState` in either is unreadable by the other. `Lobby` is the
tempting home — it is never unmounted by a screen change, and `heldPress` (`Lobby.tsx:111`) already
proves a `useState` there survives one — but the panel is not its child, and `ADR-0138` §1 says
being rendered by `Lobby` is the one thing that component cannot be.

**4. The fact must not outlive the result it was pressed on.** A player who leaves the result of
duel A, starts duel B and plays it out arrives at a second `DuelFinished` with the flag still set —
and never sees the verdict of the duel they just played. So the mechanism owes a **clear**, and the
clear has to land before the next `finished` standing without a painted frame of the wrong screen in
between.

**And one trap in the panel's gate.** `ADR-0150` §3 moves the measure from `shown !== "first"` to
*the result screen not showing*. Written as a negation, that admits the **duel table**: standing
`running`, `shown === "first"`, and the result screen is indeed not showing. What holds
`ADR-0138` §2's *"No frame of the product ever shows a rematch panel over a duel table"* today is
the screen gate. Under a negation it would be held instead by `duel-state.ts:307` — `Snapshot`
clearing `rematchOffers` — which is a reducer detail nobody would think to preserve, and a
guarantee that moves from a gate to an accident is a guarantee that will be lost.

**The deadline.** The surface is merged and unreachable now; `STORY-1216` already spent one triage
finding it and `ADR-0150` spent another settling the *whether*. Every further hour is either a QA
round re-finding it or a coder inventing this mechanism inside a ticket, where the branch order of
`Lobby.tsx` would be decided by whoever held the keyboard.

## Decision

### 1. The fact is one boolean in a provider above both surfaces, and it ends with the result it was pressed on

New file `web-client/src/result/result-screen-provider.tsx`, in the shape the client's five other
providers already have:

```tsx
// A consumer with no provider above it reads the merged behaviour: the result
// screen shows, and the press does nothing. That default is what keeps every
// test that renders `Lobby` alone byte-honest about what it is testing.
const LeftTheResultContext = createContext<boolean>(false);
const LeaveTheResultContext = createContext<() => void>(() => {});

export function ResultScreenProvider(props: { children: ReactNode }): ReactElement {
  const state = useDuelState();
  const roomAwaited = useRoomAwaited();
  const [leftTheResult, setLeftTheResult] = useState(false);
  const leave = useCallback(() => setLeftTheResult(true), []);

  // ADR-0138 §3's clear, applied to the press for §3's own reason: a leave lasts
  // as long as the result it was pressed on. Without it, a player who left duel
  // A's result never sees duel B's verdict.
  if (leftTheResult && roomStanding(state, roomAwaited) !== "finished") {
    setLeftTheResult(false);
  }

  return (
    <LeftTheResultContext.Provider value={leftTheResult}>
      <LeaveTheResultContext.Provider value={leave}>{props.children}</LeaveTheResultContext.Provider>
    </LeftTheResultContext.Provider>
  );
}

export function useLeftTheResult(): boolean { return useContext(LeftTheResultContext); }
export function useLeaveTheResult(): () => void { return useContext(LeaveTheResultContext); }
```

`App.tsx` wraps its two children in it, and that is the whole of `App.tsx`'s change:

```tsx
<main className="min-h-screen bg-bg font-ui text-text">
  <ResultScreenProvider>
    <Lobby />
    <RematchNotice />
  </ResultScreenProvider>
</main>
```

**Why a provider and not props.** `RematchNotice` goes on taking **no props** — `ADR-0138` §1's rule
survives verbatim — and `Lobby` gains none either, so no render site in any of the nine files that
mount `Lobby` changes. A context is the only shape that gives two siblings one fact without either
owning the other.

**Why it is mounted in `App` and not in `main.tsx`.** `ADR-0138` §8 requires the cross-screen proofs
to render **`App`**. A provider added to `main.tsx`'s stack would leave `App` rendering a tree that
behaves differently from the shipped one, and every proof written against it would be testing a
client nobody runs.

**The lifetime is exactly `ADR-0150` §5, obtained with no key.** A screen change does not unmount
`App`, so the boolean survives it; a reload builds a new tree, so it does not survive that; and
nothing reaches for `Storage`, so `ADR-0086` §2's rule that only `main.tsx` touches `localStorage`
is not tested here, because nothing touches.

**The clear is a conditional set during render**, React's documented way to adjust state when what it
was derived from changes — the identical construction, with the identical justification, that
`ADR-0138` §3 merged three days ago for the dismissal: it terminates (after the set the condition is
false), it is pure and so `StrictMode`-safe, and it costs no effect. **An effect would not do**:
running after paint, it would show one painted frame of the front door between duel B's
`DuelFinished` and the verdict. Because the provider is the parent, its render-phase set is resolved
before either child renders, so no child ever sees a stale flag.

**The clear is keyed on the standing, not on `state.outcome`.** `ADR-0138` §2 names *"a bespoke test
of `state.outcome` or of the address"* as the thing that would have been a second rule; this
provider calls `ADR-0114` §1's own pure function on the same snapshot instead, which makes it a
third **call** and not a third rule.

### 2. `Lobby`'s cascade is keyed on the standing, and one pure function says which screen a finished room shows

`room-standing.ts` — `ADR-0114` §1's *"one pure module reads the room and rules on the ask"*, still
with no `window`, no React and type-only imports — gains one function and one type. `roomStanding`
and `rulingOn` are **not edited**:

```ts
/** Which of a finished room's two screens the client shows. */
export type FinishedRoomScreen = "result" | "front-door";

/**
 * The screen the room this tab holds puts up, once that room is finished: its own
 * result, or the lobby's front door for a holder who has pressed *Back to the
 * lobby* (`ADR-0150` §§1-2). `null` when the room is not finished, which is what
 * keeps the front door's answer off every other screen.
 */
export function finishedRoomScreen(
  standing: RoomStanding,
  leftTheResult: boolean,
): FinishedRoomScreen | null {
  if (standing !== "finished") return null;
  return leftTheResult ? "front-door" : "result";
}
```

**`Lobby.tsx`'s three store branches test the standing.** The result branch becomes
`finishedRoomScreen(standing, leftTheResult) === "result"`; the table branch becomes
`standing === "running"`; the waiting branch becomes `standing === "waiting"`. The `heldPress`
branch, the `standing === "unknown"` branch, the six chosen-screen branches above them and the
front-door fall-through are untouched, and so is the order.

**This renders identically for every state that exists today**, because `roomStanding` *is* that
cascade: it returns `finished` before `running` and `running` before `waiting`, in the same order
and on the same three fields (`ADR-0114` §1 says so and gives the reason). What it buys is the
skip. When the answer is `"front-door"` the standing is `finished`, so `running`, `waiting` and
`unknown` are all false and the cascade falls through to the front door — **the raw fields could
never do this**, because a finished room's `view` and `roomCode` are both still set (Context, force
1). The store branches stop being three independent tests of three fields and become three arms of
one answer, which is the property that makes the front door reachable at all.

**The fall-through `ADR-0114` §2 named is preserved exactly.** `finishedRoomScreen` does not take
`shown`. A chosen screen whose provider is absent — `shown === "duels"` with `read === null` — still
falls past its branch and lands on the result screen of the room the player holds, which is
`ADR-0114` §2's *"a player whose read is unavailable lands on the room they hold rather than on a
lobby that pretends they hold nothing."* Had the `shown === "first"` conjunct been folded into this
function, that player would land on the front door instead, and a merged rule would have been
reversed by a mechanism ADR.

`Lobby` reads `const leftTheResult = useLeftTheResult()` and `const leaveTheResult =
useLeaveTheResult()` beside its other hooks, above every branch — the same position `standing`,
`ruling` and `shown` are computed in, and for the same rules-of-hooks reason.

### 3. The way back stops navigating: a button, and the press writes the address the anchor used to write

**`DuelResult`'s way back becomes a `<button type="button">`** carrying the same four words and the
same class list, so nothing about it changes on screen:

```tsx
<button type="button" className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text" onClick={props.onBack}>
  Back to the lobby
</button>
```

**Not an anchor with `preventDefault`.** An `<a href="/">` whose click never navigates still offers
a middle-click, a *Open link in new tab* and a status-bar target, all three of which would open a
document that re-boots into the result screen — the exact effect the press exists to avoid. A
control that does not navigate is a button, and `ADR-0072` §4's own rule applies to markup as much
as to names: *"the name is the mechanism, deliberately."*

**The prop is renamed `onBack` and becomes required.** With an anchor, an absent `onLeave` still left
the screen, because the browser did the work; with a button, an absent handler is a result screen
with no way off. Required is how the compiler states that, and every call site is a compile error
until it is fixed. The name changes because `onLeave` now says the one thing `ADR-0150` §1 is at
pains to deny: the press leaves no room. `WaitingTable`'s `onLeave` keeps its name, because there it
is true.

**`Lobby` builds the handler, and `DuelResult` goes on knowing nothing about navigation**
(`ADR-0060` §4, `ADR-0072` §5's *"a function of its props"*, both kept):

```tsx
onBack={() => {
  // The one thing `href="/"` did that mattered besides navigating: it replaced
  // the whole address, dropping an invite link's `?room=ABCDEFGH` — which
  // `main.tsx:176` reads into `joinRoomCode` at every boot and `boot.ts:113`
  // prefers over the room this tab holds. `leave()` writes `hashForScreen("first")`,
  // which is `"/"`, so the address after the press is what the anchor produced.
  leave();
  leaveTheResult();
}}
```

`leave()` is `use-screen.ts`'s own `replaceState` + `notify`, called from an event handler exactly as
the six merged `Back` buttons in this file already call it (`ADR-0032` §3's rule, extended to
`forgetRoom` by `ADR-0072` §4: from a handler only, never from render, never from an effect).
`use-screen.ts` and `screen.ts` are **not modified**. In the shipped client the address is already
`/` when the result screen shows, so the write is a no-op the `useSyncExternalStore` snapshot
compares away; what it earns is the query, and the address's normalisation in `ADR-0138` §9's
fence.

**`forgetRoom` is not called, and that is the whole of `ADR-0150` §1 in code.** `Lobby` keeps
`useForgetRoom()` for its one remaining caller, `WaitingTable` (`Lobby.tsx:439`).

### 4. The panel's gate names the front door rather than negating the result screen

`RematchNotice` gains one hook call — `const leftTheResult = useLeftTheResult()` — and its early
return becomes two, with `standing`, `shown` and `theirs` computed exactly as they are today and
**both of `ADR-0138` §3's render-phase clears still above them**, for the reason §3 gives: the offer
can end on a render this component returns `null` for, and a clear that sat below the gate would miss
it.

```tsx
// ADR-0150 §3: the panel stands where the result screen is not. Written as the
// screen it may stand on rather than as a negation — "not the result screen" is
// also true of the duel table, and ADR-0138 §2's "no frame of the product ever
// shows a rematch panel over a duel table" must stay a property of this gate
// rather than of the reducer clearing `rematchOffers` on a Snapshot.
if (shown === "first" && finishedRoomScreen(standing, leftTheResult) !== "front-door") return null;
if (!theirs || dismissed) return null;
```

**This is a second call of one function, not a second rule** — `ADR-0138` §2's argument carried
forward without amendment. `finishedRoomScreen` is pure, lives once in `room-standing.ts`, and is
called by `Lobby` and by the panel on the same render's snapshot of the same store and the same
address; the two cannot disagree about which screen is showing, because there is one implementation
of the question. `RematchNotice` still tests no `state.outcome`, still never reads
`window.location`, still calls no `open`, `leave` or `clearToken`, and still registers no effect of
any kind.

**Four cases fall out of the gate rather than needing a clause**, which is why it is written
positively:

- **The duel table** never carries the panel: standing `running` ⇒ `null` ⇒ withheld. Structural,
  and independent of the reducer.
- **The waiting room** and **the silence before the first frame** likewise: `waiting` and `unknown`
  both answer `null`.
- **The result screen** never carries it: `"result"` ⇒ withheld, which is `ADR-0123` §3's one live
  surface.
- **The front door of a holder who has pressed the control** carries it: `"front-door"`, which is
  `ADR-0150` §3's new case and the only `first`-screen state that does.

`ADR-0138` §2's other three bullets stand; its third — *a player who left the room is followed
nowhere* — described a document that no longer dies, and `ADR-0150` §1 is what replaced it.

### 5. What is not touched

The waiting screen's `Back to the lobby` keeps its `<a href="/">`, its `onLeave={forgetRoom}`, its
promise sentence and its full-page navigation (`ADR-0073` §§2–3, `ADR-0124` §2). Its forget goes on
doing both of its jobs, because for a `WAITING` room both are still wanted.

`roomStanding`, `rulingOn`, `spendsOnArrival`, `use-screen.ts`, `screen.ts`, `duel-state.ts`,
`duel-store.ts`, `boot.ts`, `rematch-stand.ts`, `rematch-text.ts`, `RematchControl.tsx`,
`WaitingTable.tsx` and `sign-out.ts`: not edited. The store gains no field, and no client-only value
enters `DuelState` — `ADR-0114` §1's *"every term of `roomStanding` is a fact the server sent"*
stays literally true, because the press is a **parameter** to a new function and never a term of the
old one.

The files this decision reaches, whole: `web-client/src/App.tsx`,
`web-client/src/result/result-screen-provider.tsx` (new), `web-client/src/routing/room-standing.ts`,
`web-client/src/lobby/Lobby.tsx`, `web-client/src/result/DuelResult.tsx`,
`web-client/src/result/RematchNotice.tsx`, and tests. Nothing crosses the socket, so `ADR-0047` §2's
fingerprint cannot move and the implementing tickets are not `atomic:` under `ADR-0068` §3.

### 6. What a test must prove

`ADR-0100` §5 in full — no `data-testid`, no test-only prop, no exported setter, and nothing that
resets the provider from outside. The cross-screen proofs render **`App`**.

- **The press keeps the room, proved on both halves.** After it, `pd.roomCode` is still in the
  injected `Storage` **and** a second `Welcome` still sends exactly one `JoinRoom` at that code.
  `ADR-0072` §9's own argument: the storage assertion alone cannot tell *remembered* from
  *rejoined*, and the frame assertion alone cannot tell *forgotten* from *never written*.
- **The press shows the front door.** `Play duel` and the room-code field are in the document
  afterwards, and the verdict headline is not.
- **The press sends nothing.** The count of messages sent is unchanged across it.
- **The press is not a navigation, and it takes the invite link's query with it.** Rendered at
  `/?room=ABCDEFGH`, after the press `window.location.search` is empty and `pathname` is `/`. This
  is the assertion that fails if the button is wired to the fact alone; a control that merely
  stopped navigating passes every other test in this list.
- **The panel stands on the front door after the press, exactly once.** A count of the offer's
  sentence in the document, not a presence check — `ADR-0138` §8's reason: presence is what two
  surfaces also satisfy.
- **It does not stand before the press.** The same store, the same offer, no press: no panel. This
  is the control that stops the assertion above being satisfied by a gate that is simply always
  open.
- **It never stands over the duel table.** With `theirs` accumulated and a `Snapshot` then applied
  in an order that leaves the offer standing, the document holds the table and no panel. Delivered
  in the order that isolates the gate: applied the other way round the reducer clears
  `rematchOffers` and the test would pass against a gate that had been deleted.
- **A second duel's verdict reaches the result screen.** Press the way back; `Play duel`;
  `RoomJoined` at a different code; `Snapshot`; `DuelFinished`. The verdict is on screen. This is
  the only test that fails if §1's clear is missing, and nothing else in the suite covers it.
- **The press does not survive a reload, and stores nothing.** A fresh `render()` of the same tree
  from the same frames shows the result screen, and the set of keys in the injected `Storage` is
  unchanged across the press.
- **The waiting screen's way back still forgets.** `forgetRoom` is called once and it is still a
  link. Without this, a mechanism that made *both* controls keep the room would be green.

**Which merged assertions go, and what replaces them** — `ADR-0072` §9's rule, that a replacement is
at least as strong as what it retires:

| Goes | Replaced by |
| --- | --- |
| `Lobby.test.tsx`'s *"forgets the room when the player takes the way back"* (`forgetRoom` called once on the result screen's control) | the first three bullets above: `forgetRoom` is **not** called, the room is still remembered, and the front door is showing |
| The **six** `getByRole("link", { name: "Back to the lobby" })` queries that reach the **result** screen: all four in `DuelResult.test.tsx`, and `Lobby.test.tsx:1464` and `:1532` | the same queries by `role: "button"`, plus one assertion that the result screen's way back carries **no `href`** — the only form that catches an anchor smuggled back in. `Lobby.test.tsx:1464` already asserts `href === "/"` and is the one that must invert |

`WaitingTable.test.tsx`'s one link query stays a link query, and is the discriminator that keeps the
two controls apart.

### 7. Registers, and does not answer

**`DEC-165` — the product owner's — two sentences about one dead room.** `Lobby.tsx:494` renders
`No duel room has that code.` on the front door whenever `state.refusal !== null`, and
`RematchNotice` renders `That duel room is gone.` when `state.refusal === "UNKNOWN_ROOM"`
(`ADR-0138` §4). Until this ADR the two could not be on screen together, because the front door was
unreachable while a room was held. They can now: a player whose rematch press is refused, or who
pressed the way back after being refused on the result screen, reads a sentence about a room code
they never typed. `Failure` sets one field and the client cannot see which frame it answered —
`ADR-0072` §7 and `ADR-0044` §3 both refuse to track that — so this is a choice between two merged
sentences, and **which one a player reads is what they are told**. Registered rather than decided,
and it **blocks nothing**: the mechanism above ships either way, and the repair is one conditional
wherever the answer puts it.

## Consequences

**What it buys.** `STORY-1415`'s thirteen merged tickets become reachable, which is the whole
purpose. `ADR-0150` §§1–5 become implementable with no wire, no schema, no storage, no server
change and no new word; `ADR-0138`'s mount, its dismissal and its *no effect of any kind* survive
intact; and `ADR-0114`'s *one predicate* survives as a stronger claim than before — the question
*which screen is the room's own* now has exactly one implementation instead of being spelled out
twice in two components' branch conditions.

**What it costs.**

- **`App.tsx` stops being one import and one element, and the client gains a sixth provider.**
  `ADR-0138` §1's *"Nothing is plumbed, no context is added, and no provider moves"* was a real
  claim about a real simplicity, and it is spent here. The provider also subscribes to the store,
  so every frame now re-renders `App`'s subtree from the provider down rather than from each child
  independently — the same commits, in a different order, but one more component reading the store
  on every frame for the life of the tab.
- **The result screen's way back stops being a link, and every merged assertion that finds it by
  role changes with it — but only some of them.** Thirteen sites query that role by that exact
  name; **six** are the result screen's and move to `role: "button"`, and **seven** are the waiting
  screen's and must not. The two screens say the same four words, so nothing but reading each test
  tells them apart, and a sweep would silently break the control this ADR does not touch. `onLeave`
  → required `onBack` is at least a compile error at every one of its roughly twenty call sites; the
  role query is the part no compiler catches — it fails at runtime as *unable to find role*, and a
  coder who "fixes" it by re-adding an `href` restores the bug, which is why §6 pins the absent
  `href` explicitly.
- **The press leaves no history entry, so the browser's own Back no longer returns to the result
  screen.** Today the anchor pushes one, and Back re-boots into the result. `replaceState` does not,
  and `ADR-0150` §2's *"No control returns to the result screen"* becomes true of the browser's
  control too. Deliberate — a history entry that returned would return by reload, which is a second
  way to undo the press — but it is a change to something a player can do today.
- **`Lobby`'s cascade now depends on `roomStanding`'s ordering, where it used to depend on its
  own.** The three store branches read `standing === …` and are correct only because
  `roomStanding` tests `outcome` before `view` before `roomCode`. Reordering that function — which
  looks like a local tidy inside a pure module — silently changes which screen a finished room
  renders. The reason lives in `ADR-0114` §1 and in a comment; it is not enforced by anything.
- **`room-standing.ts` now holds a function whose second argument is a press.** Every term of
  `roomStanding` is still a fact the server sent, and `finishedRoomScreen` takes the standing rather
  than the state, so the module's rule is bent rather than broken — but a reader can no longer say
  *nothing in this file knows about the client's own state*, and the next client-only term will find
  the door open.
- **A new player-visible collision**, `DEC-165` above: two sentences about one gone room, one of
  them addressed to a player who typed no code. Created by putting the front door under a held
  room, named rather than papered over, and blocking nothing.

**`ADR-0150`'s four named costs: which are resolved and which are inherited.**

- **"The client gains a state it has never had: holds a room, shows the front door"** — the debt
  `DEC-163` was registered to pay. **Resolved.** What takes up the forget's second job is §1's
  boolean and §2's standing-keyed cascade; the front door is reachable because
  `finishedRoomScreen` answers `"front-door"`, not because the tab forgot anything. Every branch
  that assumed *a room the tab holds is the screen the tab shows* has been re-read: the six chosen
  screens (unchanged, `ADR-0112` §3 already honours them), the three store branches (§2), the
  `heldPress` ask (unchanged and correct — a player holding a finished room who presses `Play duel`
  into a name ask sees the ask), the `unknown` silence (unchanged), the front door's refusal line
  (`DEC-165`), and boot's `joinRoomCode ?? remembered` (§3's `leave()`).
- **"One label, two behaviours"** — **inherited, and made sharper.** The two controls now differ in
  element as well as in effect: an anchor that forgets on the waiting screen, a button that
  remembers on the result screen. A reader of the tree can see the asymmetry that a reader of the
  screen cannot. Reconciling them is still a decision about the waiting room and is still not made.
- **"A reload undoes the press"** — **inherited by construction.** §1 mints no key, so `ADR-0150`
  §5 is what ships. It stays one clause to reverse: the provider's `useState` becomes a
  storage-backed read and a write, in one file, plus a key to clear.
- **"`Play duel` abandons a standing offer in silence"** — **inherited, untouched.** §2's cascade
  puts the player on the front door with the control live; what happens when they press it is
  `ADR-0150` §4 and `ADR-0044` §6, and nothing here changes it.

**What it forecloses.** Very little. It does not build a *leave* on the wire, a decline, a control
that ends a room, a notice on the front door, a way back to the result screen, or an address for the
held-room front door. The other repair `DEC-162` named — a route from the result screen to the
chosen screens — remains compatible: it would add controls to `DuelResult` and would not touch any
of the four parts above. Reversing this ADR whole is deleting one file, one function and three
branch conditions, and restoring an `href`.

## Alternatives considered

**Lift the boolean into `App` and pass it down as props.** The strongest case, and the smallest
diff: no new file, no context, no provider, and React's most ordinary shape — state lives in the
common ancestor of the two components that need it. It also keeps the client's provider count where
it is, and `App` is already inside every provider, so nothing is plumbed. Rejected on the clear:
`App` would have to own the render-phase reset, which means `App` reads the store and calls
`roomStanding` anyway — so the *"App stays a layout element"* saving is not real — and the props
would break `ADR-0138` §1's *takes no props*, whose stated reason (`RematchNotice` must not depend
on being rendered by something that decides what it shows) applies to `App` as much as to `Lobby`.
Doing the clear from a child's effect instead was measured and rejected separately: an effect runs
after paint, so duel B's verdict would be preceded by one painted frame of the front door.

**A module-scope boolean with a subscriber set, in `use-screen.ts`'s own shape.** Its case is
strong: that file is the merged precedent for exactly this — a fact outside React, one listener set,
`useSyncExternalStore`, read independently by any number of components with no props and no context
— and it would make the fact readable from anywhere without touching `App.tsx` at all. Rejected
because module scope outlives a `render()`: the press would leak from one test to the next in the
same file, `ADR-0150` §5's *a reload does not keep it* would have no honest proof (a fresh `render()`
is the same module), and the only way to reset it between tests is an exported setter that exists for
tests — which `ADR-0100` §5 forbids by name. `use-screen.ts` escapes this because its state is
`window.location`, which the harness already resets.

**Put the fact in the store, as a client action on the reducer.** Its case is the best of any
rejected option: the reducer already clears everything on `Snapshot`, `DuelFinished` and a
`RoomJoined` naming another room, so §1's clear would come for free and could not be forgotten; both
components already read the store, so no context, no props and no provider; and one place would hold
every fact about the room. Rejected because it puts a fact the server never sent into the state
`roomStanding` reads. `ADR-0114` §1's *"Every term of `roomStanding` is a fact the server sent…the
client asserts no game fact, it reads back the last thing it was told"* is the sentence the whole
routing rule rests on, and `ADR-0123` §10 already refused the store a field for this surface. A
client-only term there would be indistinguishable, three files away, from something the server said.

**Give the held-room front door its own address — `#/lobby`, or a `Screen` member.** Its case: the
client already has one mechanism for *which screen is showing*, it is `screen.ts`, and a new screen
would need no new fact, no provider and no gate — `shown !== "first"` would go on working untouched,
`RematchNotice` would not be edited at all, and the answer would be one entry in two switch
statements. Rejected on `ADR-0150` §5: an address survives a reload, so the press would keep the
player off the result screen after a refresh — which is precisely what §5 says does not happen, and
`DEC-163` may not change what §§1–5 say a player sees. It also drags in `ADR-0076`'s address rules
and `ADR-0081`'s fragment reading for a screen that is the front door under another name.

**Keep the `<a href="/">` and call `preventDefault()` in the handler.** Its case: the smallest
possible change to `DuelResult`, no prop rename, no required prop, and not one of the twelve merged
role queries moves. Rejected because the affordances survive the handler: a middle-click, a
*Open link in new tab* and the status bar all still promise a document at `/`, and each of them
delivers a tab that re-boots straight back into the result screen. An element that lies to the mouse
is worse than a churn of test queries, and the queries are the cost of telling the truth.

**Gate the panel on the negation `ADR-0150` §3 uses in prose — *the result screen is not showing*.**
Its case: it is the ADR's own words, transcribed, which is the safest thing a mechanism ADR can do,
and it needs no new type and no positive enumeration. Rejected on a measurement: the negation is
also true while the duel table is showing, and the only thing that would then keep the panel off the
table is `duel-state.ts:307` clearing `rematchOffers` on a `Snapshot`. `ADR-0138` §2 states
*"No frame of the product ever shows a rematch panel over a duel table"* as a property of the gate;
moving it into the reducer would make it true today and unowned tomorrow. §4 is §3's rule computed
over the screens §3 itself enumerates.

**Let `finishedRoomScreen` take `shown` as well, so one call answers the whole question in both
components.** Its case is tidiness: the function would state the complete condition, and neither
caller would compose anything. Rejected because `Lobby`'s result branch is also the fall-through
`ADR-0114` §2 relies on — a chosen screen whose provider is absent must land on the room the player
holds — and a `shown === "first"` conjunct inside the function would send that player to the front
door instead, reversing a merged rule from inside a mechanism decision.
