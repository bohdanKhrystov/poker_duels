# ADR-0088 — The two-browser proof is a written hand-check, not a CI job

- **Status:** Accepted
- **Date:** 2026-08-28

## Context

`EPIC-03`'s fourteen stories are all `done` — measured, not remembered: every file from
`STORY-0301` to `STORY-0314` carries `status: done`, and the board row reads *done — 14 of 14*.
One question was left standing behind them. `DEC-024` asks whether the epic ships an automated
two-browser end-to-end test — two real browser contexts against a running server and a real
database, playing a duel to a winner — or whether that proof is a human's in v0.1.

The registers disagree, and the disagreement is what makes this a decision rather than a
formality. The board says the epic is closed; the epic's own `## Open decisions` table says
`DEC-024` is due *before this epic closes* and that it *"decides whether a fourteenth story
exists"* — arithmetic written before `STORY-0314` existed, and `STORY-0312` had already moved the
reservation once. So either the epic closed early and owes a fifteenth story, or the automated
ceiling is where it already stands and the table is stale.

**What is proved today.** Two suites, each end to end in its own layer:

- `poker-server/src/test/kotlin/duels/poker/server/e2e/` — twelve files driving two WebSocket
  sessions through a whole duel against a real, migrated PostgreSQL under Testcontainers, and
  asserting seating, the declared winner, chip flow, secrecy, reconnect, coins, history and the
  ladder. *Two clients, one room, a duel to a winner* is proved here, against real persistence.
- `web-client/src/e2e/` — the real React tree, the real store and the real `bootDuelClient` wiring,
  driven over `scripted-duel.gen.json`, which the server's own `ProtocolCodec` writes and which
  `:poker-server:check` fails the build on any byte of drift (`STORY-0312`). *A whole duel through
  the real client, over frames the server actually emits*, with the secrecy claim made over the
  rendered DOM rather than the store, is proved here.

**What neither proves. This is the entire gap, and it is four things:**

1. **The root render is executed by no test.** `main.tsx`'s module scope does run — `Lobby.test.tsx`
   reaches the real module through `importOriginal` — but its last statement,
   `ReactDOM.createRoot(document.getElementById("root")).render(…)` wrapping seven nested providers,
   runs only in a browser, because no test document has a `#root` to find. `web-client/index.html`'s
   `<div id="root">` is asserted nowhere either. Delete that div, drop a provider out of the tree,
   or nest two of them in the wrong order, and the whole client suite stays green while the page
   renders nothing. `ADR-0032` §5 predicted this — `main.tsx` "stays the untested, logic-free entry
   point" — and the file has since grown to 286 lines.
2. **The real socket is opened by no test.** `new WebSocket(socketUrl(window.location))`
   (`web-client/src/protocol/index.ts:35`) is never executed: `index.test.ts` stubs the constructor
   and asserts the *string* it was handed. `dev-proxy.test.ts` asserts the *values* in
   `vite.config.ts`. Nothing joins the two against a server that is listening — and even the JVM
   suite runs on Ktor's `testApplication`, in process. **No test in this repository has ever
   opened a TCP connection to the duel server.**
3. **The built bundle is never loaded.** CI runs `npm run build` and its exit code is the whole of
   the assertion. Nothing opens `dist/index.html`.
4. **Two browsers have never been two players.** `pd.deviceId` lives in `localStorage`, which is
   shared per origin, so the two-player case needs two storage partitions — something no jsdom test
   and no JVM test has any notion of.

That gap is real and a browser runner would close most of it. It is also buildable **today**, and
not blocked on `EPIC-07`'s hosting: `docker compose up -d`, the server's `main`, `npm run dev`, a
browser driver, two contexts, one duel. Against it stands what it costs: a third CI job that is the
union of the two that exist — JVM plus Docker plus Postgres plus Node plus a browser binary — on
every pull request, in a repository where most pull requests touch tickets and markdown; the
flakiest class of test there is; and a yield confined to one composition file, one constructor call,
a four-line proxy table and a static bundle, none of which change often and all of which fail on the
first page load.

And the thing to be proved is, in `docs/vision.md`'s own words, a human moment: *"Send a link. She
opens it in a browser. We play a full heads-up match. Someone wins. We hit Rematch."*

## Decision

### 1. No browser drives this client, here or in CI

`EPIC-03` ships **no** automated two-browser end-to-end test, no further story, and no browser
runner. No Playwright, Puppeteer, Selenium, WebDriver or Cypress dependency enters
`web-client/package.json`, and `.github/workflows/build.yml` keeps its two jobs. The automated
ceiling is exactly where `STORY-0312` and the server's `e2e` package put it, and `ADR-0032` §4's
sentence — *"still jsdom, still no network"* — stands as written.

`EPIC-03` is closed on its stories: fourteen of fourteen, and the board row is right. The epic's
`## Open decisions` table is struck by the pull request that lands this ADR, and its *"decides
whether a fourteenth story exists"* is answered **no**.

### 2. The proof is a hand-check, and these are its steps

One person, two browsers, one commit, from a clean checkout. Every step carries an observation that
can fail; a step whose failure condition cannot be evaluated has not been performed.

1. `docker compose up -d`. The database is `localhost:5432`, `poker`/`poker` — `ServerConfig`'s own
   defaults, so nothing is configured. **Fails if** the container is not accepting connections.
2. Start the server: run `duels.poker.server.ApplicationKt`'s `main` on the JVM. No Gradle task does
   this — `poker-server` carries no `application` plugin — so it is an IDE run configuration or an
   equivalent `java -cp`. **Fails if** `curl -s localhost:8080/health` does not answer `OK`.
3. `cd web-client && npm ci && npm run dev`. Note the origin Vite prints. **Fails if** the origin
   does not load, or the console shows a proxy error against `/api` or `/ws`.
4. Open that origin in an ordinary window — **A**. Open it again in a **private window or a second
   browser profile** — **B**. Not a second tab: `pd.deviceId` is `localStorage`, shared per origin,
   so two tabs are one player and `ADR-0018` would have B's socket adopt A's seat and close it.
   **Fails if** `localStorage.getItem("pd.deviceId")` reads the same value in both.
5. In A, create the duel. A shows a room code and a link of the form `<origin>/?room=CODE`.
   **Fails if** no link is shown, or it carries no `?room=`.
6. Paste that link into B's address bar. **Fails if** B does not arrive seated at the table without
   anyone typing a code.
7. Play one whole duel to a winner: both seats act on their own turns, at least one hand reaches a
   showdown, and at least one is won without one. **Fails if** either screen offers an action out of
   turn, or the two screens ever disagree about the board, the pot or the stacks.
8. Before the duel ends, reload A once. **Fails if** A does not return to the same seat with the
   same stacks and the same board.
9. Watch the rival's two cards throughout. Until a showdown reveals them they are backs on both
   screens. **Fails if** a rival card is legible at any moment, on screen or in the DOM, before the
   frame that reveals it — and **fails** if the hand won without a showdown in step 7 ever shows one
   at all.
10. Both result screens name the same winner. Both profile strips have moved from where they
    started: the winner `+1`, the loser `−1`, unclamped (`ADR-0014`, `ADR-0015` for a draw), and the
    duel stands in both recent-duel lists naming the other player and the opposite outcome.
    **Fails if** any one of the three disagrees with the other two.
11. Both press Rematch. A new duel starts in the same room (`ADR-0044`). **Fails if** either browser
    is left waiting on a screen the other has already left.

Steps 5, 6, 7, 10 and 11 are `docs/vision.md`'s first success condition, in order.

### 3. A run that is not written down did not happen

The receipt is one line appended to `EPIC-03`'s Definition of done, carrying: the date, the commit
SHA it ran against, the two browsers, the room code, the winner, and both balances afterwards. It is
written by the person who ran it, in the same act that fills the epic's `Metrics` table — which is
already a human's and already says *"filled in when the epic closes"*.

The receipt is **not** a merge gate and **not** a story. No pull request waits behind it, no ticket
is blocked on it, and no agent can write it: it is the one line in that Definition of done a merge
cannot produce.

### 4. The procedure lives in this ADR; receipts live in the register

§2 is here rather than in a new `docs/*.md` because a document nothing points at is a document
nothing runs — twenty ADRs in `docs/adr/` already name `EPIC-07`, which is *not written*. An ADR is
immutable by this repository's own rule, which is the right property for a check whose only value is
that it cannot be quietly weakened: shortening §2 means a superseding ADR in a reviewed pull
request, not an edit. Receipts change, so they live in the epic file.

### 5. Reversing this costs one ADR and one story

Turning it over is a superseding ADR plus one story: a `web-client` dev dependency, a fixture that
starts compose, the server's `main` and `npm run dev`, and a third job in `build.yml`. Nothing
decided here makes that harder — no code moves, no interface changes, no dependency is added or
removed, and the four uncovered things stay exactly as findable as they are today. **That
reversibility is the reason for the choice, not a consolation for it**: the evidence that a browser
runner would catch anything is thin, so the cheapest decision to undo wins. There is no deadline in
either direction — nothing about a two-browser test is cheaper today than in six months, and nothing
about closing `EPIC-03` forecloses one.

## Consequences

**Four things are now knowingly unguarded, and each fails green.** A provider dropped from
`main.tsx`'s render, or a deleted `<div id="root">`, leaves the entire client suite passing and the
page blank. A change to
`socketUrl`, to `vite.config.ts`'s proxy table or to the server's port leaves `socket-url.test.ts`
and `dev-proxy.test.ts` both green while no socket can open — they assert a string and a config
value that nothing forces to agree. A bundler change that breaks `dist/` leaves `npm run build`
exiting 0. And nothing at all speaks to two storage partitions. This ADR does not reduce that risk;
it names it and accepts it.

**The regression window is a release, not a pull request.** Any of the four breaks the moment it is
merged and is discovered at the next hand-check. With over a thousand merged pull requests behind
`develop` and no cadence forcing the check, that window is however long a human leaves it — the
opposite of every other gate in this repository, where `verify:` blocks the ticket that broke it.

**A failure has no bisect.** When step 6 fails, there is no red test naming a commit. Someone
re-runs eleven manual steps against older checkouts by hand, restarting a server and two browsers
each time.

**The built bundle stays unproven, including by this hand-check.** Step 3 runs `npm run dev`, not
`dist/`, because nothing in this repository serves built assets — `EPIC-03`'s *Out of scope* puts
that in `EPIC-07`. So `npm run build`'s exit code remains the only claim anyone makes about the
artifact a player would eventually load, and gap 3 above survives this decision entirely. It is
stated rather than papered over.

**The check decays with repetition.** Eleven steps with named failure conditions beat one sentence,
but the eighth person to run it skips step 9 in a way a test suite never does. An automated test is
worth more on its hundredth run than its first; this is worth less.

**It sets a precedent beyond this epic.** `EPIC-04`'s account screens and `EPIC-05`'s ladder are in
the same position — proved in jsdom, proved in the JVM, never in a browser — and this is now the
merged answer saying that is acceptable. A later epic wanting a browser runner argues against a
precedent rather than into a vacuum.

**What it buys.** CI stays two jobs, both deterministic and neither needing a browser binary, a
bound port or a container-plus-Node union; no flake lands on pull requests that touch only tickets.
The gap stops being an unstated assumption: *"no test in this repository has ever opened a TCP
connection to the duel server"* was true and unrecorded before this ADR. And `EPIC-03` closes on a
decision instead of on an oversight.

**What it forecloses.** Nothing permanently. But `EPIC-03` closes without a machine ever having
asserted the sentence `docs/vision.md` opens with, and the epic's `Metrics` will describe a suite
that has never touched a browser. Anyone reading those numbers as coverage of the vision's first
success condition is wrong, and this paragraph is where they find that out.

## Alternatives considered

**1. Ship it: a fifteenth story building the automated two-browser test.** Its case is the strongest
here. It would close the only untested seam in a repository that tests everything else twice, and it
is the one mechanical proof of the sentence the product exists for. It is buildable today, not
blocked on hosting: compose, the server's `main`, `npm run dev`, two browser contexts, one duel. It
would catch all four failures above at merge time rather than at release time, and keep catching
them for free forever — which is precisely the argument that ordinarily beats *"a human will
notice"*, because a human noticing is not a schedule. Rejected on the ratio, not on the principle:
what it *uniquely* covers is one composition file, one constructor call, a four-line proxy table and
a bundle — the two-player duel itself is already proved against real Postgres in the server's `e2e`
package, and the whole-client duel is already proved against the server's own frames — while what it
costs is a permanent third CI job that is the union of the other two, running on every pull request
in a repository whose pull requests are mostly markdown. With the yield that thin, the reversible
option wins, and §5 keeps it reversible.

**2. Write the browser test, keep it out of CI — an `npm run e2e` someone runs locally.** The
coverage without the CI bill, and it has a real advantage over §2 that a numbered list cannot match:
it is *executable*, so it cannot be misread, half-performed or skipped at step 9. Rejected because
an un-run test is worse than no test — it rots against the client it tests, goes red for reasons
nobody owns, and the first person to find it broken deletes it. This repository already made this
exact argument one layer down, in `CONTRIBUTING.md` on `-PrequireDocker=true`: *"a test suite that
skips silently in CI is a test suite that has stopped testing."* A check that runs only when someone
remembers is a manual check with a build dependency attached.

**3. Leave the hand-check as the one sentence `EPIC-03`'s Definition of done already carries.** Zero
cost, and it is demonstrably what the epic was written expecting — the last checkbox already says
the vision's success condition *"is not an automated assertion, and pretending otherwise would be
the dishonest kind of green."* Rejected because *"checked by hand, once, and recorded"* names no
step, no observation and no failure. Two people would run two different things and neither could
fail it: without step 4 it is one player in two tabs, and without step 9 nobody looks at the rival's
cards at all. A manual check that cannot fail is not a check, and the sentence would have let
`EPIC-03` close on a green that meant nothing.

**4. Defer it to `EPIC-07`, when a deployed host exists to point a browser at.** The case is not
empty: the most valuable browser test runs against the real artifact on a real origin, and neither
exists yet — `npm run dev` is not what a player will load, which is exactly why gap 3 survives this
ADR. Rejected on two grounds. First, it is false that the question needs a host: the untested seam
is the boot wiring and the real socket, and `npm run dev` exercises both today. Second, and
decisively, **nothing would make it decidable then that is undecidable now** — the same four
uncovered things, the same CI cost, the same reversibility, and no new evidence arrives in the
interval. A deferral whose unlock is *"nothing"* is how twenty ADRs came to name an epic that is not
written and collects none of them, and this decision refuses to be the twenty-first.
