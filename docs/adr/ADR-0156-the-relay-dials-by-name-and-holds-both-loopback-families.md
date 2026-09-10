# ADR-0156 — The relay dials its target by name, and holds both loopback families

- **Status:** Accepted
- **Date:** 2026-09-10
- **Resolves:** `DEC-169` — **by what change does the QA relay reach the preview origin
  [`ADR-0117`](ADR-0117-the-proofs-of-record-load-the-built-bundle.md) §1 established?**
  **By changing the instrument, in `scripts/qa/delay.mjs`, and not the origin.** Registered **and**
  answered in this pull request: the question was found on 2026-09-10 by `TASK-131006`'s drive, not
  by a planner, and it has never stood in an open table. It gates `TASK-131006`, `EPIC-13`'s last
  ticket
- **Amends nothing and supersedes nothing.** `ADR-0117` §1 stands byte-unchanged:
  `web-client/vite.config.ts` is **not** touched, `preview: { port: 4173, strictPort: true }` gains
  no `host` key, and the origin of a proof of record stays `http://localhost:4173`. §2's marker
  check, §3's `stack.sh` work and §7's re-drive list are all untouched — §3 remains owed, and this
  ADR spends only the two lines of it that `TASK-131006` cannot run without
- **Constrains nothing in production.** Two files under `scripts/qa/`. No `poker-server` route, no
  `web-client/src` module, no CI job, and no `verify:` block anywhere runs a browser
  ([`ADR-0089`](ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md) §2b
  stands unamended)
- **Registers no decision.** The one adjacent question — a proof origin reachable from a second
  device on the LAN, which
  [`ADR-0140`](ADR-0140-the-clipboard-api-is-the-only-copy-this-client-attempts.md) §6 names as a
  precondition for evidence it does not have — is argued in §Alternatives 2 and left to the ticket
  that actually needs it, because a `DEC` nobody is working is noise in the open table

## Context

`TASK-131006` — `STORY-1310`'s `P3`, a socket dropped under a page that keeps running — is the last
unfinished ticket in `EPIC-13`. Its drive on 2026-09-10 reported `cut 0`: the relay had no live pair
to sever. A `cut 0` looks like a reading and is not one; it is the instrument saying it was never in
the path.

**What was measured, on `develop`, today.**

| Fact | How it was taken |
| --- | --- |
| `scripts/qa/delay.mjs:66` dials `connect(targetPort, "127.0.0.1")`; four further `connect` sites at 105, 127, 208 and 241 do the same | read |
| `web-client/vite.config.ts:7` sets `preview: { port: 4173, strictPort: true }` and **no `host`** | read |
| `dns.lookup("localhost")` answers `::1` first — Node 26.7.0, default result order `verbatim` | `dns.lookup("localhost", { all: true })` → `[::1, 127.0.0.1]` |
| A Node server told to listen on `localhost` binds **`::1` only** | `net.createServer().listen(0, "localhost")` → `{"address":"::1","family":"IPv6"}` |
| Vite 5.4.21 binds `localhost` when no `host` is set, in **both** modes — `resolveHostname(optionsHost)` returns `"localhost"` for `undefined`, and dev and preview hand it to the same `httpServerStart` | read at `vite/dist/node/chunks/dep-BK3b2jBa.js:17312`, `:63435` (dev) and `:66326` (preview); then run against this repository's own config — dev bound `{"address":"::1","family":"IPv6"}`, preview bound `{"address":"::1","family":"IPv6"}` |
| `connect(port, "127.0.0.1")` against a `::1`-only listener is **`ECONNREFUSED`** | run |
| `connect({ port, host: "localhost", autoSelectFamily: true })` reaches a listener on **either** family; with `autoSelectFamily: false` it reaches `::1` only, and is `ECONNREFUSED` against a `127.0.0.1`-only listener | run |
| A listener holding `[::1]:P` does **not** collide with a bind on `127.0.0.1:P` — both succeed. A second bind on `[::1]:P` is `EADDRINUSE` | run |

So the relay accepts the browser and cannot forward it, and `P3` is unmeasurable. That much was
already known when this question was raised.

**One premise of the question is false, and it changes what has to be fixed.** The question was
raised with the note that `P1`, `P2`, `P4` and `P6` drove successfully against the dev server
*because dev binds both families*. It does not. Dev and preview resolve their host through the same
function and both bound `::1` in the measurement above. Nothing in this repository asks Vite for a
family in either mode, and `scripts/qa/stack.sh` starts neither — they are the `qa-cycle` skill's
background tasks, and the skill's line is a bare `npm run dev`. How those delayed drives reached
Vite on `5273` is therefore **recorded nowhere**: a `--host` at bring-up would do it, and so would a
machine that resolved `localhost` to `127.0.0.1`, and both are properties of a run rather than of
the repository. That is the finding. Those drives passed on a condition nothing states, nothing
requires and nothing checks — which is why the first drive that did not get it handed back a number
instead of a failure.

**This repository has lost a reading to the same class before.** `STORY-1310`'s own `P1` row:
*"`localhost:5173` still resolved to `::1:5173` (the stale Vite) even with the relay correctly bound
on `127.0.0.1:5173`"* — the browser reached a leftover Vite and never the relay. `ADR-0117`
§Consequences named it and declined it: *"The `localhost` dual-stack trap that cost `P1` a reading …
is untouched."* It is today's fault at the other hop.

**What is in tension.**

*Against touching the origin.* `ADR-0117` §1 writes the preview block out verbatim and says
`vite.config.ts` *"gains exactly this, and nothing else"*, so a `host` key contradicts a merged
ADR's literal text and needs this ADR to authorise it — a real cost, but not the decisive one. The
decisive one is that pinning the origin's family fixes **one** origin. The dev server has the same
bind and the same trap; a static server, or the Ktor server if `DEC-126` lands there, would each
arrive with a family of its own, and the relay would rediscover this every time.

*Against touching the relay.* `delay.mjs` is the one instrument every refresh-path drive shares, and
it has five `connect` sites, so *"just change line 66"* is narrower than it looks. Its self-tests
are the only thing standing between a change here and a silently wrong reading in every later drive.

*The third force, which decides the shape.* Whatever is changed, **nothing today asserts that the
relay can reach the origin a drive is pointed at.** The relay prints `accepted connection … relaying
to …` and then swallows the outgoing socket's error in `source.on("error", () => {})`, so an
unreachable target produces a dead page, no message, and — at cut time — a confident `cut 0`. That
silence, not the address literal, is what turned an instrument fault into a reading.

*The deadline.* None points at a particular answer. One points at now: `TASK-131006` is `EPIC-13`'s
last ticket, and the epic reads `done` above an open box that says `P3` remains undriven.

## Decision

### 1. The relay dials its target by name, and resolves every family

`scripts/qa/delay.mjs`'s target dial — today `connect(targetPort, "127.0.0.1")` at line 66 —
becomes, in substance:

```js
const outgoing = connect({ port: targetPort, host: TARGET_HOST, autoSelectFamily: true });
```

with `TARGET_HOST = "localhost"` as a named constant carrying the reason. The relay resolves the
target the way the browser resolves the origin: by name, over every address that name has. A relay
that resolves differently from the browser it sits in front of is not transparent, and transparency
is the whole of its contract.

`autoSelectFamily: true` is written **explicitly**, not left to the default. It is the load-bearing
half — measured, the name alone with autoselection off reaches `::1` and is `ECONNREFUSED` against a
`127.0.0.1`-only target — and the default is a Node-version fact (true from Node 20;
`web-client/.nvmrc` pins 24) that a flag can flip. An instrument does not rest on an ambient default
it can state.

### 2. The four other `connect` sites keep `127.0.0.1`, and the rule that says why

Lines 105, 127, 208 and 241 dial the relay's own listen port and its own control port — ports **this
same process bound**, through `listenAsync`, on `127.0.0.1`. They are unchanged.

> **Dial by literal address what you bound. Dial by name what somebody else bound.**

This is not tidiness. `cut` is the one command whose entire output is a count, and a `localhost`
dial could resolve to a *different* process holding `[::1]:<controlPort>` — cutting nothing and
reporting `cut 0`, the very reading this ADR exists to make impossible. For the same reason the
**control server stays bound on `127.0.0.1` alone**: it is dialled only by `delay.mjs cut`, which
knows the address because it is the address the relay bound.

### 3. The relay holds both loopback families on its listen port, or it does not start

`startRelay` binds the listen port on `127.0.0.1` **and** on `::1`, with one connection handler and
one `livePairs` behind both, so a `cut` destroys pairs from either. A family it cannot take is a
refusal to start, not a warning.

The order is load-bearing, and a coder will hit it: bind `127.0.0.1` at the requested port first,
read the resolved port back from `server.address()`, then bind `::1` at **that** port — both
self-tests ask for port `0`, and two independent ephemeral binds would land on two different ports.

This closes the other hop, and §5's layout makes it the likely one. Measured: a listener on
`[::1]:4173` does not collide with a bind on `127.0.0.1:4173`. So a `vite preview` started without
§5's `--port` — the single most available operator error here — takes `[::1]:4173`, the relay takes
`127.0.0.1:4173`, `strictPort` sees nothing, and the browser, resolving `localhost` to `::1` first,
reaches **preview directly and bypasses the relay entirely**. Every reading would then be of an
unrelayed stack, and the cut would answer `cut 0`. With both families held, that operator error
becomes `EADDRINUSE` at bring-up — which is what `ADR-0117` §1 already asks `strictPort` to be:
*"a collision is a refusal to start."*

### 4. A relay that cannot reach its target says so

Three changes, in ascending cost, all in `delay.mjs`:

- **At start-up**, once both listens are up, the relay dials the target once and **exits 1** naming
  `host:port` and the error code if it cannot connect. The successful dial prints the resolved
  `remoteAddress` — so every drive's record contains the family the relay actually reached, rather
  than an assumption about it.
- **Per connection**, an outgoing socket that fails before it connects is printed —
  `delay: could not reach <target> for a connection accepted on <listenPort>: <code>` — instead of
  being swallowed by the `error` handler that exists only to keep the process alive.
- **`--selftest-unreachable`**, a third self-test flag beside `--selftest` and `--selftest-cut`,
  proving both: a relay pointed at a port nothing listens on exits non-zero and names the target,
  and a relay whose target dies **after** start-up prints the per-connection failure rather than
  answering with silence. The two self-tests that exist keep passing unchanged.

This is the answer to *what stops this recurring*. It is an assertion the instrument makes about
itself, at bring-up, in the one place that knows the answer.

### 5. The origin does not move; a relayed proof runs preview on `4273` behind the relay on `4173`

`web-client/vite.config.ts` is not edited. `ADR-0117` §1's origin — `http://localhost:4173` — is
what the browser is pointed at, and it is the relay that answers there:

```
cd web-client && npm run build
npm run preview -- --port 4273          # strictPort still applies, at 4273
node scripts/qa/delay.mjs 4173 4273 <delayMs> 6173
```

This is `STORY-1310`'s own layout applied to the built mode, and for its reason: the served port
moves so that **the origin a record names does not**. Serving the bundle on `5173` instead would be
`ADR-0117` §Alternatives 3, rejected there by name because it *"makes the mode unobservable by port
at the exact moment the mode is the thing that matters"* — a `4xxx` origin still tells a reader
which artifact a record is about.

A drive's readiness check is `ADR-0117` §2's marker, fetched **through the relay's origin** rather
than against preview's own port:

```
curl -s http://localhost:4173/ | grep -q '/@vite/client'   # must NOT match
```

Run there, one command proves the artifact *and* the whole path at once, which is the cheapest
guard available and needs no new mechanism.

### 6. What the follow-up ticket must do

One ticket, two files, no browser in its `verify:` block:

| File | Change |
| --- | --- |
| `scripts/qa/delay.mjs` | §§1–4: the target dial by name with `autoSelectFamily: true`; both loopback families on the listen port, the second bind at the first's resolved port; the start-up dial (fatal, naming the target, printing the resolved `remoteAddress`); the per-connection failure message; `--selftest-unreachable` |
| `scripts/qa/drive.mjs` | `ADR-0117` §3, **as already merged**: `APP` (line 19) and the two `url.includes("localhost:5173")` tab filters (lines 39 and 313) read `PD_APP_ORIGIN`, defaulting to the built origin `http://localhost:4173/`. Nothing new is decided here — this is the part of §3 `TASK-131006` cannot run without |

Gates: `node scripts/qa/delay.mjs --selftest`, `--selftest-cut` and `--selftest-unreachable` each
exit 0, plus `python3 .github/scripts/lint_tickets.py`. The `--selftest-unreachable` assertions are
the ones with teeth and should be probed by mutation: put the target dial back to
`connect(port, "127.0.0.1")` and it must go red against a `::1`-bound echo.

`TASK-131006` is not re-scoped by this ADR beyond its stack paragraph, which becomes §5's three
commands. It stays `ready` — no decision blocks it any more — and it may not be driven until this
ticket lands; its `depends_on` gains the new id when the planner files it.

**Still owed and not spent here:** `ADR-0117` §3's `stack.sh` half — `build-web`, `web-origin`,
`wait-web [built|dev]`, and `status`'s third mode. Until that lands, `stack.sh`'s `WEB` and its
`served`/`status` probes still name `5173`, and a drive on the built origin must not use them. That
is a ticket for the planner, and nothing in this ADR waits on it.

## Consequences

**A machine without IPv6 loopback cannot run this relay at all.** §3 makes a failed `::1` bind
fatal, which on such a machine is `EADDRNOTAVAIL` at bring-up and no drive until somebody supersedes
this. That is deliberate: a best-effort second bind, silently skipped, is precisely the silence §4
exists to delete, and the escape has to be a stated flag rather than an inference. Measured on this
machine only, which is the machine every drive has ever run on. **This is the sharpest cost here and
it is not closed.**

**The relay's port becomes genuinely exclusive on the machine, and some bring-ups that "worked" will
now refuse.** A leftover listener on either family — another worktree's preview, a stale Vite, a
second checkout's relay — used to be invisible on the family the relay did not hold; it is now
`EADDRINUSE`. Every start it newly refuses is a start whose readings would have been of the wrong
process, so this is the change paying out rather than costing — but the first person to meet it gets
a refusal where they used to get a page, and `kill` is denied in this repository, so freeing the
port is somebody's `TaskStop` and not a command.

**Dialling by name buys reachability and spends certainty about which server answered.** Where two
processes hold the same port on different families, the relay now takes whichever answers first
instead of failing. §4's `remoteAddress` line puts the resolved address into the drive's own record,
so the ambiguity is written down rather than assumed — but only for the start-up dial.
Per-connection dials could in principle land on the other family, and nothing checks that they did
not. Named, not closed.

**The path is proved at bring-up, not continuously.** §4's start-up dial cannot see a target that
moves or dies at minute forty of a drive; the per-connection message can, and nobody is *required*
to read stderr. A drive that ends in an unexplained blank screen still owes a look at the relay's
output, and that is discipline rather than a mechanism — the same residual `ADR-0117` accepted for a
stale `dist/`.

**Two files under `scripts/qa/` will briefly hold two truths about the origin.** `drive.mjs` starts
defaulting to `http://localhost:4173/` while `stack.sh`'s `WEB`, `status` and `served` still probe
`5173`, because §3's other half is owed. For that window `stack.sh status` can report `web: down`
against a perfectly good built origin, and `served` will not name the checkout behind it. Named here
so the next reader finds it by grep rather than by confusion.

**What it forecloses.** Nothing permanently, and reversal is a `git revert` of one commit over two
QA scripts: no production code, no config, no schema, no wire. That reversibility is the reason for
the choice and not a consolation for it — the evidence that the origin's own config is the wrong
place for this is strong, but the evidence about any *future* origin's family is nil, and where the
evidence is thin the cheapest thing to undo wins. What it does spend is the argument for
`preview.host`: an origin whose family is a repository fact is genuinely nicer than an instrument
that copes, and if `DEC-126` lands on a server this project owns, §Alternatives 1 becomes available
again and nothing here stands in its way.

**What it does not fix.** One relay per browser profile is still what `P3`'s *"the rival's screen
keeps running"* half needs, exactly as `ADR-0117` left it. Nothing here makes the proxy in front of
Ktor production-shaped, and no record produced through this instrument says anything about the
deployment — that is still `DEC-126`.

## Alternatives considered

**1. `preview` gains `host: "127.0.0.1"`, amending `ADR-0117` §1.** The strongest alternative, and
the one the question was raised expecting. Its case: the origin's address family becomes a fact of
the repository rather than of the machine, which is where such a fact belongs; it is one line
against §§1–4's several dozen; it removes the dependence on Node's family autoselection entirely;
`strictPort`, the relay and the browser would all be talking about one address; and it costs the
shared instrument nothing, which matters because every refresh drive rides on it. It needs no
`dev-proxy.test.ts` change either, since `TASK-131011` never added §1's three preview assertions.
Rejected on three grounds, the second decisive. First, it fixes exactly one origin: dev has the
identical bind and the identical trap, and any future origin arrives with a family of its own.
Second — measured — it **narrows `strictPort` to one family**: a stale preview on `[::1]:4173`, from
a worktree whose config predates the key, does not collide with a `127.0.0.1:4173` bind, so the
guard `ADR-0117` §1 calls load-bearing stops firing at exactly the moment it is needed, and the
browser reaches the stale bundle. Third, it makes a merged ADR's verbatim block wrong for a change
that does not have to live in that file at all — an amendment is cheap to write and permanent to
read.

**2. `preview: { host: true }`, binding every interface.** Better than alternative 1 on its own
terms: dual-stack, so both families are reachable *and* both collide, and it would put the built
bundle on the host's LAN address — which is exactly the first of the two changes `ADR-0140` §6 names
as a precondition for ever having evidence about the no-clipboard path on the human's phone, a
question `ADR-0128` and `ADR-0140` both had to decide without any. Rejected here, and only here:
serving a bundle on every interface is a change of posture that belongs to the ticket which wants a
LAN proof origin, with its own reviewer, not smuggled in as the fix for a relay that cannot dial. It
also still amends §1, and it still leaves the relay unable to reach anything it did not bind itself.
Named so that the LAN ticket finds it already argued.

**3. `delay.mjs` dials `"localhost"` and nothing else — one token on line 66.** The cheapest change
available, and it does fix today's break. Rejected on measurement: with autoselection off the name
resolves to `::1` and is `ECONNREFUSED` against a `127.0.0.1`-only target, so it trades a relay that
cannot reach IPv6 for one that cannot reach IPv4 — and the moment anyone takes alternative 1's
advice, or a Ktor server binds `127.0.0.1`, it breaks in the other direction. Node's default makes
it work today, and a `--no-network-family-autoselection` or a Node downgrade makes it stop, silently,
in a script whose failure mode is a number that looks like a reading.

**4. Change line 66 and stop — the narrow reading of the question.** Defensible: it unblocks
`TASK-131006`, it is the smallest possible diff, and §3 is arguably a different fault. Rejected
because §3 is the *same* fault at the other hop — the relay and the thing it talks to disagreeing
about address family — it has already cost this repository a reading in `P1`, and §5's layout makes
the collision that triggers it the most likely operator error in the very drive being unblocked.
Fixing one hop and leaving the other hands the next drive a `cut 0` for the second time.

**5. Drop the relay for `P3` and take the socket down from the browser instead — CDP's
`Network.emulateNetworkConditions` with `offline: true`.** A real alternative and cheaper than all
of the above: no relay, no ports, no families, and `drive.mjs` already speaks CDP, so it is one
verb. Rejected because it measures a different event. `ADR-0112` §6 asks for a socket dropped under
a page that keeps running on a network that still works; `offline: true` takes the browser's whole
network stack down, so every reconnect attempt `reconnecting.ts` makes is refused by the browser
rather than by the world, and the recovery can only be observed after a second operator action puts
it back. The reading would be *"the client recovers when the operator restores the network"*, which
is not `P3`. It is not free either — it needs `Network.enable` per target, which `drive.mjs` does
not do. Worth remembering for a future path that genuinely is about being offline.

**6. Answer nothing; re-drive `P3` on `dev` and accept the reload.** The case: `STORY-1310` already
records `P3` honestly as an instrument fault, `EPIC-13` closed with that box open and said so, and
this is one row of one QA story. Rejected because the row is the last thing standing between
`EPIC-13` and an honest close, `ADR-0117` §7 names `TASK-131006` as the ticket its answer unblocks,
and that same ADR's §5 forbids offering a `dev` record toward `ADR-0093` §1a — so the deferral does
not produce a cheaper reading, it produces no reading at all, twice.
