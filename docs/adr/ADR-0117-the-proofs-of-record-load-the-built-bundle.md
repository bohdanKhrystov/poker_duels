# ADR-0117 — The proofs of record load the built bundle, on an origin of its own

- **Status:** Accepted
- **Date:** 2026-09-04
- **Resolves:** `DEC-087` — by what mechanism do the proofs of record load the built bundle a real
  user would receive: what serves `dist/`, on what origin, and what supersedes
  [`ADR-0088`](ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) §2 step 3's
  `npm run dev`? Registered open 2026-08-30 by
  [`ADR-0093`](ADR-0093-ready-for-real-users-is-said-of-the-shipped-artifact.md) §1a for the
  architect; amended 2026-09-04 with `TASK-131006`'s finding
- **Supersedes** `ADR-0088` §2 **step 3, and step 3 alone**, through that ADR's own §5 route. §2's
  other ten steps stand byte-unchanged; §§1, 3, 4 and 5 are untouched; `ADR-0088` §1's refusals —
  no browser dependency in `web-client/package.json`, `build.yml` keeps its two jobs — stand as
  `ADR-0089` §1 restated them
- **Constrains nothing in production.** No `poker-server` route, no `ServerConfig` field, no
  shipped `web-client/src` module and no CI job changes; what moves is `vite.config.ts`, one
  `package.json` script, `src/dev-proxy.test.ts` and `scripts/qa/`. `ADR-0089` §2b holds
  unamended: **no drive becomes a merge condition here**, and nothing in this ADR may be read as
  making one
- **Registers:** `DEC-126` open for the architect — what serves the bundle in the **offered
  deployment**, which this ADR deliberately declines to pre-decide

## Context

`ADR-0093` §1a made *ready for real users* conditional on the proofs of record loading the built
bundle, and registered the mechanism as `DEC-087` rather than inventing one. Its own register row
said the question blocked nothing running today. That was true on 2026-08-30 and is false now.

**What `TASK-131006` found on 2026-09-04.** Driving `STORY-1310`'s `P3` — `ADR-0112` §6's
*genuinely dropped socket*, a reconnect through `reconnecting.ts` under a page that keeps running —
the drive cuts the app's socket at the transport with `scripts/qa/delay.mjs cut`. Under
`npm run dev` the browser's connection to Vite carries **two** sockets: the app's `/ws`, proxied to
Ktor, and Vite's own HMR client. The cut takes both, and Vite's HMR client then reloads the page.
Observed, not inferred, and reproduced 4 of 4 across both seats and both delays:
`performance.getEntriesByType("navigation")[0].type` reads `reload` where it read `navigate` at
first paint; a JS-realm marker planted immediately before the cut was already gone at the first
poll, `t+25ms`, at `0 ms` — inside `retryDelayMillis`'s own first attempt, so the reload **pre-empts**
the reconnect rather than racing it. `reconnecting.ts` was read in full and contains no navigation
call, and `location.reload` cannot be stubbed away (`TypeError: Cannot redefine property`; Chrome
enforces it `[LegacyUnforgeable]`), so no page script can suppress it.

So the dev server does not merely serve a *different artifact*. **It removes a class of reading from
existence**: there is no page instance left to observe recovering in place, and every armed
`MutationObserver` dies with the document.

**And a second distortion, which the same story's records already contain.** `STORY-1310`'s
`delayed 300ms` layout puts `delay.mjs` in front of Vite, delaying every byte 300 ms in each
direction. `P2` recorded a reload whose `open` **did not return for 11.8 s**, whose first paint was
the lobby, and which was filed as `ADR-0112` §6's *"no lobby on the way"* failing on `delayed`.
`web-client/src` holds **102 non-test modules**, and the dev server ships each one as its own
request; at six connections per origin and 600 ms of added round trip per request, the arithmetic is
`102 / 6 × 600 ms ≈ 10 s` — which is the observed number, and is a property of the **instrument**,
not of the client. The built bundle answers the same page in roughly three requests. So the finding
that most nearly contradicts a merged ADR rests on a load latency no real user can ever be in.

**What is in tension.** Against changing anything: the mechanism that is *most* faithful — the Ktor
server serving `dist/` on one origin with no proxy — writes deployment-shaped production code into
the shipped server while `docs/architecture.md` says of deployment *"Not decided yet; not needed
before v0.2."* It would pick single-origin before anyone has decided the topology, and it is the
most expensive of the candidates to undo. Against doing nothing: `ADR-0088` §Alternatives 4 rejected
deferring to `EPIC-07` because *"nothing would make it decidable then that is undecidable now"* —
and unlike 2026-08-28, the deferral now has a named cost, one blocked ticket and two readings resting
on a distorting instrument.

Pulling a third way: **an origin change is not free.** `pd.deviceId` and `pd.roomCode` are
`localStorage`, partitioned per origin, and moving off `5173` silently gives every profile a fresh
partition. Whether that is a cost at all depends on facts this decision had to check rather than
assume.

**The deadline.** There is none that points at a particular answer, but there is one that points at
*now*: `STORY-1310`'s remaining rows are being driven this week, and every day the question stays
open adds `dev` records that may have to be read again. Moving the origin is also cheaper while the
pile of records naming `localhost:5173` is small than it will be after `EPIC-07` and a UAT pile.

## Decision

### 1. The proofs of record load `dist/`, served by `vite preview`, on `http://localhost:4173`

The built bundle is served by **`vite preview`** — the `preview` command of the Vite already pinned
by `ADR-0026` and already in `web-client/package.json`. No dependency is added, to any module, for
any purpose. `web-client/package.json` gains one script, `"preview": "vite preview"`.

`web-client/vite.config.ts` gains exactly this, and nothing else:

```ts
preview: { port: 4173, strictPort: true },
```

- **`4173`** is Vite's own preview default, so the number is documented upstream rather than
  invented here. The origin of a proof of record is `http://localhost:4173`.
- **`strictPort: true`** is load-bearing, not tidiness. Without it a leftover listener makes Vite
  pick the next free port and the drive proceeds against an origin nobody named. With it, a
  collision is a refusal to start.
- **No `preview.proxy` is written.** Vite resolves `preview.proxy ?? server.proxy`, so both modes
  read the **one** table `ADR-0026` §*Development runs against the real server* established and
  `dev-proxy.test.ts` guards. Two tables would be two things that can disagree.

`dev-proxy.test.ts` gains three assertions: `preview.port` is `4173`, `preview.strictPort` is
`true`, and **`preview.proxy` is `undefined`** — the last is the one with teeth, because a copied
proxy table is exactly how the two modes would drift apart in silence.

### 2. The mode is proved off the artifact, never off the port

A drive establishes which artifact it is on by reading what is served, not by trusting a port:

```
curl -s http://localhost:4173/ | grep -q '/@vite/client'
```

The dev server injects `<script type="module" src="/@vite/client">` into the HTML it serves; the
built `index.html` references a hashed asset and never that path. So the marker is not a proxy for
the mode — **it is the presence of the exact mechanism that broke `P3`**. A match on the built
origin means the dev server is answering and the drive must stop.

### 3. `scripts/qa/stack.sh` owns the mode, and `wait-web` is where it is enforced

- **`build-web`** — new. Runs `npm run build` in `web-client` in the **foreground** and dies unless
  `web-client/dist/index.html` exists afterwards. Foreground, so a failed build is an exit code at
  the point of failure rather than a background task that quietly never listens.
- **`wait-web [built|dev]`** — **defaults to `built`.** Polls that mode's origin for `200` as it
  does today, and then applies §2's check: `built` fails if the marker matches, `dev` fails if it
  does not. Every existing caller already invokes `wait-web`, which is why the gate goes here and
  nowhere else.
- **`web-origin [built|dev]`** — new. Prints the origin, so no caller carries the literal.
- **`status`** — reports `web: built | dev | down`.

`scripts/qa/drive.mjs`'s `APP` and its two `url.includes("localhost:5173")` target checks read the
origin from the environment (`PD_APP_ORIGIN`), defaulting to the built origin. `stack.sh`'s `WEB`
constant is replaced by the two origins.

The `qa-cycle` skill's bring-up runs `stack.sh build-web` **before** starting `vite preview` as its
background task, in place of `npm run dev`, then `stack.sh wait-web`. Nothing else in the skill's
teardown or port discipline changes; `strictPort` makes its *"a round that leaves a listener makes
the next round fail"* warning a hard failure rather than a silent one.

### 4. `ADR-0088` §2 step 3 is superseded, and reads

> 3. `cd web-client && npm ci && npm run build && npm run preview`. Note the origin Vite prints —
>    `http://localhost:4173/`. **Fails if** the origin does not load; if the console shows a proxy
>    error against `/api` or `/ws`; or if `curl -s http://localhost:4173/` answers a body containing
>    `/@vite/client`, which means the dev server is answering and the artifact under check is not
>    the one a user receives.

Steps 1, 2 and 4–11 stand byte-unchanged, reading *"that origin"* in step 4 as the origin step 3
now prints. Step 4's failure condition — the two `pd.deviceId`s reading the same value — is
unaffected by the move: it compares two partitions with each other, never with a remembered one.

A receipt written under `ADR-0088` §3 names the artifact it ran against. A receipt that does not, or
that names `dev`, does not satisfy `ADR-0093` §1a.

### 5. `npm run dev` stays. What it loses is the proofs of record

The dev server is not removed, deprecated or discouraged for development. `npm run dev` is how this
client is developed and that does not change.

What changes is which records may be offered as **proofs of record** under `ADR-0093` §1a:

| Drive | Mode |
| --- | --- |
| `ADR-0088` §2's hand-check | **`built`**, always, from this ADR's merge |
| Any QA, UAT or audit round started after this merges | **`built`** by default |
| A round that names `dev` in its own record | permitted — and its record may not be offered toward `ADR-0093` §1a |
| Development, and a drive investigating an unmerged change | `dev`, freely |

**Every round record and every driven row names the artifact it ran against**, in the same place it
already names its network layout. The two are independent axes: `built, delayed 300ms` is a
complete answer; `bare` alone is not. `STORY-1310`'s own `bare` — *"the product as it ships"* —
becomes true under `built` and was not true before.

### 6. A `dev` finding that turns on load latency is re-read before it is filed

`ADR-0089` §4 already refuses to file a failure that does not reproduce by hand, and excludes it
from `B(N)` as a harness defect. This extends that discipline by one clause, in the same shape and
for the same reason:

**A finding made on `dev` whose conclusion depends on how long the page took to load or paint does
not stand against a merged source. It is re-read on `built` before it is filed.** The dev server
answers a page in ~102 requests where the bundle answers it in ~3, so under any added latency the
two are not the same measurement.

This is not a gate and creates none: `ADR-0089` §2b stands unamended, no pull request, `verify:`
block or ticket waits on any drive, and `build.yml` keeps its two jobs. It governs what a **finding**
may rest on, exactly as §4 does.

### 7. What re-drives, and what does not

This ADR does not retroactively invalidate a record. A `dev` record is a true record of a `dev` run
at a named commit; `ADR-0089` §2c already forbids citing any of them as coverage.

- **`TASK-131006` / `STORY-1310` `P3` re-drives on `built`.** It is `BLOCKED`, not read — this is
  the ticket the answer unblocks.
- **`P4`, mid-flight, finishes on the mode it started on** and names it. Its subject is a rejoin
  round trip, not a severed socket, and re-driving in-flight work costs more than it buys.
- **`P1`, `P2` and `P5` do not re-drive** to satisfy this ADR. But `P2`'s and `P5`'s `delayed`
  halves each carry a finding that turns on an 11.8 s first paint, which §6 says does not stand
  against `ADR-0112` §6 until it is re-read on `built`. That is a separate ticket for the planner,
  filed against the finding, not against this ADR.

### 8. This does not decide what serves the bundle in production

`vite preview` is a proof instrument on a workstation. It is **not** a statement that the offered
deployment serves the client from a Vite process, from a proxy, or from any particular origin. That
question is registered as **`DEC-126`, the architect's**, due before anything is offered to real
users — beside `ADR-0093` §1b's bound transport, at the same event, and named rather than folded
into a prose pointer at an epic that is not written. When it is answered, the hand-check's origin is
the deployment's and this ADR is superseded for §4; §§1–3 survive as the local instrument or go with
it.

## Consequences

**A build now stands between a change and a drive, and nothing catches a stale one.** `npm run dev`
reflected an edit instantly. `built` does not: a drive that skips `build-web` runs the *previous*
commit's bundle while its record names the current SHA — a record that lies, in the one register
this project treats as evidence. §2's marker proves the **mode**, not the **freshness**, and no
cheap check proves freshness (a hash in the HTML would have to be compared against something, and
the something is the build). The mitigation is ordering — `build-web` before `wait-web` — which is
discipline, not a mechanism. **This is the sharpest cost here and it is not closed.**

**The origin moves, and every literal `5173` outside production code is now wrong until repointed.**
`scripts/qa/stack.sh`'s `WEB`, `scripts/qa/drive.mjs`'s `APP` and two target filters, the `qa-cycle`
skill's port discipline, and `STORY-1310`'s `bare`/`delayed` layout definitions all name it. And one
that is *not* repointed by this ADR: `ServerConfig.DEFAULT_BASE_URL` is `"http://localhost:5173"`
(`ADR-0077`), so the server's recovery links now name an origin no proof of record uses. It bites
nothing today — an unconfigured build binds `NoRecoveryMailer` and no link arrives — but it is a
trap armed for the first drive that can follow one, and `ADR-0093` §1b's transport work must set
`BASE_URL`. Named here so it is found by grep rather than by a broken link.

**The storage-partition change is accepted, explicitly, and it costs the drives nothing.** Checked
rather than assumed: `ADR-0089` §3 and the `qa-cycle` skill make a **fresh `mktemp -d` Chrome profile
per round mandatory**, because `ADR-0018` re-seats a returning device by `pd.deviceId`. A fresh
profile has an empty partition on any origin, so no round has ever carried storage across an origin
and none will. The hand-check's step 4 compares two partitions with each other and is unaffected.
The one real loss is a human's own browser: a device id and a room code held on `5173` do not follow
to `4173`, and a half-played state on the old origin cannot be carried over. That is acceptable, and
for a proof of record it is slightly better than neutral — the human arrives as a device the server
has never seen, which is what a real user is. **What a drive measures does not change; what a human
keeps between drives does.**

**The drives will behave differently, and not all of it is the product changing.** The built bundle
is React's production build: `React.StrictMode` (`main.tsx:275`) stops double-invoking mounts and
effects, every development warning disappears, and code is minified. A defect masked by a
double-mount in `dev` surfaces only on `built`, and one caused by it disappears. A round may
therefore disagree with its own history for a reason that is neither a defect nor a repair, and
`ADR-0089` §4's harness/product classifier has no term for *"the mode changed"*. This ADR does not
add one; it names the gap.

**A stale `dist/` is a new failure mode the dev server made impossible.** `vite preview` serves from
disk per request, so a rebuild under a running preview is picked up — but a preview process left
running from another checkout serves that checkout's bundle, and nothing in §2 can tell.

**It proves the bytes, not the serving path.** A proxy is still between the browser and Ktor, so
`/ws`'s upgrade, its close semantics and its headers are Vite's `http-proxy`, not production's.
`ADR-0093` §4 names a proxy out of the readiness bar, which is why this is acceptable — but a reader
who takes a `built` record as evidence that the *deployment* works is wrong, and `DEC-126` is where
that is answered. `ADR-0088` gap 3 closes for the hand-check and the rounds and stays open for CI,
where `npm run build`'s exit code remains the whole of the assertion, because `ADR-0089` §2b forbids
the alternative.

**Three merged texts become false on this merge, and the answering change corrects them.**
`ADR-0088`'s Consequences — *"The built bundle stays unproven, including by this hand-check"* — is
falsified rather than superseded, since it is a statement of fact and not a clause. `ADR-0096`'s
*"The audit walks `npm run dev` like everything else until `DEC-087` is answered"* is spent, and the
audit walks `built`. `docs/test-plan.md`'s *"Every case runs against `npm run dev`. `dist/` is loaded
by nothing here"* becomes false the moment §3's `stack.sh` changes land, so its correction is
**named work inside the implementing ticket**, not a note for whoever notices — a document that
describes a mechanism must move in the change that moves the mechanism. The register strikes are a
different case and are made here: `DEC-087` leaves every open table in the pull request that lands
this ADR.

**What it does not fix.** A cut through one relay still severs both seats, so `P3`'s *"the rival's
screen keeps running"* half needs one relay per browser profile — an implementation the ticket
settles, not a decision. The `localhost` dual-stack trap that cost `P1` a reading (`::1` shadowing a
relay bound on `127.0.0.1`) is untouched: `preview` resolves its host exactly as `server` does today,
so the new mode introduces no new trap and removes none.

**What it forecloses.** Nothing permanently, and reversal is smaller than `ADR-0089` §6's: revert
five lines of `vite.config.ts` and `package.json`, three `stack.sh` subcommands and one `drive.mjs`
constant, plus a superseding ADR restoring `ADR-0088` §2 step 3. **That reversibility is the reason
for the choice, not a consolation for it** — the evidence that `vite preview`'s serving differs from
production's in any way that matters is nil, and where the evidence is thin the cheapest thing to
undo wins. What it does spend is the cheap answer: if `DEC-126` lands on the Ktor server serving
`dist/`, this instrument is maintained beside it or superseded, and the work here is not reused.

## Alternatives considered

**1. The Ktor server serves `dist/` — `staticFiles` plus an SPA fallback on `8080`, one origin, no
proxy.** This is the strongest alternative and the most faithful thing available. It removes the
proxy from the proof entirely; the page, `/api` and `/ws` become genuinely same-origin, which is what
`socket-url.ts`'s KDoc already claims of production (*"production is same-origin, so there is no
base-URL knob"*); `P3`'s cut becomes a true single-hop drop with no intermediary deciding what Ktor
sees; and it is code `EPIC-07` will need anyway **if** the deployment is single-origin. Rejected on
two grounds, the second decisive. First, cost and reversibility: it is production code in the
shipped server — a route, a config field for where `dist/` lives, a decision about what `/` answers
when `dist/` is absent, caching headers, and tests for all of it — where the alternative is five
lines of config; once shipped, people build on it. Second, **it pre-decides the deployment topology
that `docs/architecture.md` explicitly leaves open** — *"Not decided yet; not needed before v0.2"* —
by writing single-origin into the server before anyone has chosen it. The thing blocked today is one
QA drive, not a deployment. If `DEC-126` lands here, that ADR supersedes this one and the code is
written then, against a real deployment, with evidence this decision does not have.

**2. A static server under `scripts/qa/` — node builtins, an SPA fallback, a raw WebSocket-upgrade
relay to `8080`.** Total control, zero dependency, `ADR-0089` §2a satisfied by construction, no
production code, and `git rm`-reversible like everything else under `scripts/qa/`; `delay.mjs`
already proves this repository can relay TCP with `node:net` and nothing else. Rejected because it is
roughly 150 lines of hand-rolled proxy doing what an already-installed, already-configured,
upstream-maintained tool does in five — and `ADR-0088` §Alternatives 2 already priced this exact
mistake: *"an un-run test is worse than no test — it rots against the client it tests, goes red for
reasons nobody owns, and the first person to find it broken deletes it."* A hand-written WebSocket
upgrade proxy is the most rot-prone thing in that category, and it would be load-bearing for every
proof of record this product has.

**3. Run `vite preview` on `5173`, keeping the origin exactly where it is.** The cheapest change on
the board, and its case is entirely practical: `stack.sh`'s `WEB`, `drive.mjs`'s two hard-coded
target filters, the skill's port discipline and `STORY-1310`'s layout definitions all stay correct
byte-for-byte; no storage partition moves; `DEFAULT_BASE_URL` stays true; nothing in this ADR's
second Consequences paragraph is incurred at all. Rejected because it makes the mode **unobservable
by port at the exact moment the mode is the thing that matters**. A leftover dev server on `5173`
would be driven as though it were the bundle, producing precisely the `P3` symptom that raised this
question — and `P1` has already lost a reading to a leftover Vite on `[::1]:5173`, so this is a
failure this repository has actually had, not a hypothetical. §2's marker check would still catch
it, but only when run; with two ports, `strictPort` turns the same collision into a refusal to start
**before** a drive begins. A loud failure at bring-up is worth more than a correct reading at
teardown, and the price is a list of literals that a grep finds.

**4. `npm run build`, then open `dist/index.html` over `file://`.** Nothing to serve, nothing to
start, no port, and it loads the built bytes literally. Rejected outright and quickly: `file://` is
an opaque origin with different `localStorage` semantics, ES modules are blocked by CORS there, and
with no proxy `/api` and `/ws` resolve to nothing. It does not load a working page, so it proves
less than `npm run build`'s exit code does.

**5. Answer nothing; wait for `EPIC-07` and prove against the real deployment.** The case is real:
the most valuable proof runs against the artifact on the origin a user actually reaches, and
anything built now is a stand-in that must be maintained or thrown away. Rejected on `ADR-0088`
§Alternatives 4's own reasoning, restated with one thing added. Its reasoning: *"nothing would make
it decidable then that is undecidable now"* — the same candidates, the same reversibility, no new
evidence arriving in the interval — and *"a deferral whose unlock is nothing is how twenty ADRs came
to name an epic that is not written."* What is added since 2026-08-28 is that the deferral now has a
name and a bill: `TASK-131006` is blocked, `STORY-1310` cannot finish, and two of its recorded
findings rest on an instrument this deferral would preserve. `DEC-126` is what honestly remains for
`EPIC-07`, and it is registered in an open table with a due event rather than left as prose.
