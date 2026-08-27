---
id: STORY-0414
title: Claimed here, recovered there, end to end
type: story
status: done
parent: EPIC-04
module: web-client
labels: [client, e2e, auth, identity]
depends_on: [STORY-0407, STORY-0412, STORY-0413]
---

## Goal

One test plays a duel anonymously, wins the coin, names the profile, claims it with credentials, and
signs in from a **second client bearing a different device id** — which reads back the same balance,
the same name and the same duel.

## Why

This test *is* the epic. Everything else is how it is made to pass, and the epic's definition of done
says so in those words.

## Design notes

- **Two clients, two device ids, no shared storage.** The second client must be a genuinely separate
  storage and connection — the assertion is worthless if both halves read the same key. `TASK-030304`
  owns exactly one storage key for the device id, which is what makes two of them constructible.
- **It runs the client's own machinery**, in the shape `STORY-0312` established: a committed script
  of real server frames replayed through the real store and screens, rather than a browser
  automation this repository has not decided on (`DEC-024`).
- **The coin is asserted as a number the server sent**, at both ends, and the balance is compared for
  equality rather than for being non-zero.
- **The duel is asserted by identity**, not by count: the same `duelId` appears in the second
  client's history, with the same opponent and the same outcome.
- **The name is asserted after the claim**, because `ADR-0030` §1 promises a claimed profile keeps
  it: the permanence trigger fires only on statements naming `display_name`, and a claim names no
  column of `player` at all.
- **The hand-checked receipt is part of the epic, not this story**: `ADR-0012` named the cost in
  advance, and the epic's definition of done keeps one manual pass on a real second device.

## Tasks

Split on 2026-08-27 into **ten**, against what `STORY-0407`, `STORY-0412` and `STORY-0413` actually
landed, and with the mechanism probed rather than remembered — a scratch harness of exactly the
shape below was built and run in a worktree before a ticket was written, which is what caught the two
findings under *What the split measured* below.

**Two branches, not one chain.** `TASK-041401` touches `drive-duel.tsx`; `TASK-041402`–`TASK-041405`
touch `account-server.ts`. Their *Files* tables are disjoint, so the two can run at the same time.
Everything from `TASK-041406` on is strictly linear, because four tickets share one test file and
the run is sequential.

```
TASK-041401 ─────────────────────────────┐
TASK-041402 → 041403 → 041404 → 041405 ──┴→ 041406 → 041407 → 041408 → 041409 → 041410
```

| ID | Title | Status |
| --- | --- | --- |
| [TASK-041401](../tasks/TASK-041401-the-duel-driver-writes-into-the-storage-it-is-handed.md) | The duel driver writes into the storage it is handed, and one module owns the double | ready |
| [TASK-041402](../tasks/TASK-041402-two-players-keyed-by-the-device-id-each-one-holds.md) | Two players, keyed by the device id each one holds, and every request written down | backlog |
| [TASK-041403](../tasks/TASK-041403-the-record-each-player-keeps-and-the-name-each-one-sets.md) | The record each player keeps, and the name each one sets | backlog |
| [TASK-041404](../tasks/TASK-041404-the-claim-and-the-credential-it-attaches-to-one-profile.md) | The claim, and the credential it attaches to exactly one profile | backlog |
| [TASK-041405](../tasks/TASK-041405-the-session-outranks-the-device-id-and-sign-out-ends-it.md) | The session outranks the device id, and signing out ends it | backlog |
| [TASK-041406](../tasks/TASK-041406-one-boot-of-the-whole-client-over-the-storage-it-is-handed.md) | One boot of the whole client, over the storage and the server it is handed | backlog |
| [TASK-041407](../tasks/TASK-041407-claimed-here-the-duel-the-coin-the-name-and-the-credential.md) | Claimed here — the duel, the coin the server sent, the name, and the credential | backlog |
| [TASK-041408](../tasks/TASK-041408-recovered-there-a-different-device-reads-back-the-same-three-facts.md) | Recovered there — a different device id reads back the same balance, name and duel | backlog |
| [TASK-041409](../tasks/TASK-041409-the-second-client-sends-no-player-id-and-is-told-who-it-is.md) | The second client sends no player id, and is told who it is | backlog |
| [TASK-041410](../tasks/TASK-041410-signing-out-there-leaves-the-first-browser-untouched.md) | Signing out on the second client leaves it with no profile, and the first untouched | backlog |

### What the split measured, rather than assumed

Four facts came out of running the harness, and two of them changed the shape of the split.

- **The committed script already carries two device ids, and two player ids.** Seat 0's `Welcome` is
  `deviceId: "device-seat-0"`, seat 1's is `"device-seat-1"`, both written by the server's own
  encoder. So the story's *"two clients, two device ids"* needs nothing invented: each browser earns
  its id by playing its own seat of the script. It also hands the split its **discriminator** — the
  second browser's device id names a genuinely different player, so *reading the first browser's
  balance* is a wrong-answer-possible assertion rather than the only answer available.
- **A first boot makes zero HTTP requests.** `ProfileProvider`'s effect runs at mount; the device id
  arrives later, from the `Welcome`; and `readFromApi` returns `no-profile` **without a request** when
  the device id is null (`api.ts:42`). Measured: an empty request log and `No profile yet.` on screen.
  Every browser in this story is therefore booted **twice** — once to mint, once to read — and that
  is a fact about the real client, not an artefact: a returning browser has its id in storage before
  the tree mounts. `TASK-041406` owns it and states it as a test.
- **A screen change needs `findBy*`, not a microtask flush.** jsdom queues `hashchange` as a task, so
  after a click the hash reads `#/account` while the old screen is still rendered — measured. The
  merged answer is `App.test.tsx`'s `await screen.findBy…`, and `setTimeout` is forbidden outright:
  `virtual-time.test.ts` is a **text scan** that fails any test file containing that token without
  `vi.useFakeTimers(`.
- **The address is module-global.** `use-screen.ts` keeps one subscriber set and reads
  `window.location.hash`, so two mounted clients share one address. The two browsers are therefore
  never mounted at once; a fact one browser establishes travels to the next assertion in a binding.

### The mock this story must use, and the one it must not

`Lobby.tsx:6` is `import { useHistory, useLadder, useSignedIn } from "../main";`. Those are module
constants bound to `window.fetch` and the real `localStorage`, with no prop path, so a test that
needs a different history read must mock the module. There are two merged shapes and only one works:

- **`Lobby.test.tsx:40` — partial, via `importOriginal`.** Keeps every other export. Verified working
  from `src/e2e/` in the probe. This is the one.
- **`App.test.tsx:41` — wholesale.** Replaces the module and exports none of its bindings. Two
  `STORY-0412` tickets were blocked and rewritten over it, the second because it was written against
  the first's *pre-amendment description* rather than its merged shape. Named here so the third does
  not happen.

Because `vi.mock` is hoisted and file-scoped, the mock lives in each **test file**, never in
`drive-arc.tsx`. The harness is handed a `vi.hoisted` object and writes into it.

### Where the fixtures come from, and why none of it is a decision

- **The socket frames are the committed script**, generated from the server's own `ProtocolCodec` —
  `STORY-0312`'s mechanism, reused rather than re-invented.
- **The HTTP bodies are `meBody` and `duelRowBody`** from the merged `profile-fixture.ts`, which ten
  test files already use. `STORY-0312`'s rule that a hand-typed fixture is worse than none was about
  the wire, where a generator exists; for HTTP the merged answer is the fixture builders, and one
  place changes when `GET /api/me` changes.
- **The duel row is derived from the script's own `DuelFinished`** — `{"winner":0,"handsPlayed":7,"finalStacks":[3000,0]}`
  — so the duel the fake server reports is the duel that was played, not a free parameter.
- **A session outranks a device id** is not a judgement this split made:
  [`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md) says it in its title,
  and `connection.ts:40–42` records that the client keeps sending its device id under a token
  precisely because the server ignores it.
- **`duelId` reaches no DOM text.** It is a React `key` in both `ProfileStrip` and `HistoryScreen`, so
  the story's *"asserted by identity"* is an assertion over the read, with the rendered line as
  corroboration. `TASK-041408` says so rather than leaving a coder to discover it.

### The guard this story is asked to extend

`no-secret-in-a-url.md` — the note `TASK-041224` promoted to a file *because `STORY-0414` cites it* —
records that the merged sweep covers four functions called directly and says: *"a future caller that
builds its own URL… is invisible to this test… Extending this file's coverage to a new caller is that
caller's ticket."* A browser driven through its **screens** is that caller, and `TASK-041409` is the
extension — in this story's own file, over its own request log, widening nothing that is merged.

## Acceptance criteria

- [ ] One test carries the whole arc: anonymous duel → win → name → sign-up → second client → sign-in
      → same balance, same name, same duel.
- [ ] The two clients hold different device ids, and neither reads the other's storage — asserted, not
      arranged and assumed.
- [ ] The second client never sends a player id, and is *told* who it is.
- [ ] The balance read at the end equals the balance read before the claim, exactly.
- [ ] The duel in the second client's history has the same `duelId` and the same outcome as the one
      played in the first.
- [ ] Signing out on the second client leaves it with no profile, and the first client is unaffected.

## Open decisions

**None.** The split raised none, and the four judgements it made are each derivable from something
already merged — recorded under *Where the fixtures come from* above so nobody has to rediscover the
reasoning. `DEC-024` stays open, is the architect's, and is untouched: this story's *Out of scope*
below refuses the browser harness rather than answering the question.

## Out of scope

- A browser-driven end-to-end harness — `DEC-024` is open and this story does not answer it.
- Anything the epic's other stories own; this story adds no production behaviour, only proof.
- Any change to `main.tsx`. Making the three module-scope providers injectable would be production
  behaviour, and the merged partial mock already gives a test everything it needs.
- Re-proving the scripted duel. `STORY-0312`'s eighteen merged tests own it; `TASK-041407` asserts
  only that it finished and what it left in storage.
