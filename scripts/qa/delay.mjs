// A loopback TCP relay: forwards 127.0.0.1:<listenPort> to 127.0.0.1:<targetPort>, delaying every
// byte in both directions by a fixed <delayMs> — wide enough to make the STORY-1310 round trip
// visible past the 250ms drive.mjs samples at. No dependency (ADR-0089 §2a): node:net is the whole
// toolkit, same discipline as drive.mjs, and this runs before `npm ci` in a QA context.
//
//   node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs> [controlPort]
//   node scripts/qa/delay.mjs cut <controlPort>
//   node scripts/qa/delay.mjs --selftest
//   node scripts/qa/delay.mjs --selftest-cut
//
// Exit codes follow drive.mjs's convention: 2 is usage. The relay never exits on its own — it is a
// background process, killed by whoever started it.
//
// With a controlPort, a second server listens for the cut side (TASK-131002): any connection to it
// destroys every live relayed socket pair with destroy() — never end() — because P3 needs a
// genuine dropped socket, not an application winding a connection down, and reports how many pairs
// it destroyed. The relay keeps listening: the next connection reconnects through it normally,
// exactly like a real dropped-network reconnect.

import { createServer, connect } from "node:net";

function usage() {
  console.error("usage: node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs> [controlPort]");
  console.error("       node scripts/qa/delay.mjs cut <controlPort>");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function listenAsync(server, port) {
  return new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, "127.0.0.1", () => resolve(server.address().port));
  });
}

// Wires source's bytes onto dest, delayMs behind. Each chunk gets its own setTimeout instead of a
// shared queue: equal-delay timers fire in the order they were armed (a Node guarantee, not one
// this file builds), so chunk order survives untouched. end/close/error are armed the same way,
// behind the same delay, so a teardown can never overtake data chunks armed ahead of it on the same
// source — a relay that tore dest down immediately would truncate whatever was still in flight.
function pipe(source, dest, delayMs) {
  source.on("data", (chunk) => {
    // Backpressure is deliberately ignored: write()'s return value exists to pause a source facing
    // a slow sink, and the payloads relayed here — a dev bundle, JSON frames — are tiny on loopback.
    setTimeout(() => { if (!dest.destroyed) dest.write(chunk); }, delayMs);
  });
  source.on("end", () => setTimeout(() => { if (!dest.destroyed) dest.end(); }, delayMs));
  source.on("close", () => setTimeout(() => dest.destroy(), delayMs));
  // 'close' always follows 'error' for a net.Socket, so the handler above already propagates the
  // teardown; this listener exists only so an unhandled 'error' does not crash the process.
  source.on("error", () => {});
}

// Starts the relay, and — when controlPort is given — a second server whose only job is severing
// every pair currently in flight. livePairs is a Set, not a single slot: P3 needs every live
// connection cut, not just the most recent one, and a drive can hold more than one tab through the
// relay at once. Pairs untrack themselves on their own close, so a cut always counts pairs that are
// actually still alive, never ones that already tore down on their own.
async function startRelay(listenPort, targetPort, delayMs, controlPort) {
  const livePairs = new Set();

  const server = createServer((incoming) => {
    console.error(`delay: accepted connection on ${listenPort}, relaying to 127.0.0.1:${targetPort}`);
    const outgoing = connect(targetPort, "127.0.0.1");
    const pair = { incoming, outgoing };
    livePairs.add(pair);
    const untrack = () => livePairs.delete(pair);
    incoming.once("close", untrack);
    outgoing.once("close", untrack);
    pipe(incoming, outgoing, delayMs);
    pipe(outgoing, incoming, delayMs);
  });
  const port = await listenAsync(server, listenPort);

  let controlServer;
  let boundControlPort;
  if (controlPort !== undefined) {
    controlServer = createServer((conn) => {
      const pairs = [...livePairs];
      livePairs.clear();
      for (const pair of pairs) {
        // destroy() on both members, not end(): P3 is a socket dropping out from under a page
        // that is still running, not an application closing it down. destroy() is what actually
        // does that; end() would still be a deliberate, orderly close under the hood, whatever it
        // looks like from outside on a given run.
        pair.incoming.destroy();
        pair.outgoing.destroy();
      }
      console.error(`delay: cut ${pairs.length} live pair(s) on command`);
      conn.end(`cut ${pairs.length}\n`);
    });
    boundControlPort = await listenAsync(controlServer, controlPort);
    console.error(`delay: control port listening on 127.0.0.1:${boundControlPort}`);
  }

  return { server, port, controlServer, controlPort: boundControlPort };
}

// The client side of the control port: connects, reports exactly what the relay wrote back, and
// resolves. A control port nothing is listening on rejects with the connection error, which main's
// top-level catch turns into exit 1 naming the reason.
async function cutCommand(controlPort) {
  const conn = connect(controlPort, "127.0.0.1");
  let output = "";
  await new Promise((resolve, reject) => {
    conn.once("error", reject);
    conn.on("data", (chunk) => { output += chunk.toString(); });
    conn.once("close", resolve);
  });
  process.stdout.write(output);
}

// A throwaway echo server for the self-tests below: whatever it receives, it writes straight back,
// with no delay of its own — the only delay in a measured round trip comes from the relay.
async function startEcho() {
  const server = createServer((socket) => socket.on("data", (chunk) => socket.write(chunk)));
  return { server, port: await listenAsync(server, 0) };
}

// Sends three distinctly-labelled chunks through a relay proxying to echoPort and returns the
// round-trip time once every byte is back. A dropped, doubled or reordered chunk throws instead of
// returning a time, so a bad relay fails loudly rather than reporting an unchecked number.
async function measureRoundTrip(echoPort, delayMs) {
  const { server: relayServer, port: relayPort } = await startRelay(0, echoPort, delayMs);
  const client = connect(relayPort, "127.0.0.1");
  await new Promise((resolve, reject) => {
    client.once("connect", resolve);
    client.once("error", reject);
  });

  const chunks = ["alpha-chunk-", "bravo-chunk-", "charlie-chunk"];
  const want = chunks.join("");
  let got = "";
  const echoed = new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`echo timed out at ${delayMs}ms`)), 5000);
    client.on("data", (chunk) => {
      got += chunk.toString();
      if (got.length >= want.length) {
        clearTimeout(timer);
        resolve();
      }
    });
  });

  const startedAt = Date.now();
  for (const chunk of chunks) client.write(chunk);
  await echoed;
  const elapsedMs = Date.now() - startedAt;

  client.destroy();
  relayServer.close();
  if (got !== want) {
    throw new Error(`echo mismatch at ${delayMs}ms: sent ${JSON.stringify(want)}, got ${JSON.stringify(got)}`);
  }
  return elapsedMs;
}

async function selftest() {
  const { server: echoServer, port: echoPort } = await startEcho();

  // Two different delays, not one: a relay that sleeps a hard-coded 200ms passes a single-delay
  // check, and only a second, different input tells the argument from the constant.
  const fastMs = await measureRoundTrip(echoPort, 50);
  const slowMs = await measureRoundTrip(echoPort, 200);
  echoServer.close();

  console.log(`round trip through a 50ms relay: ${fastMs}ms`);
  console.log(`round trip through a 200ms relay: ${slowMs}ms`);

  if (fastMs < 100) {
    throw new Error(`50ms relay round trip was ${fastMs}ms, expected >= 100ms`);
  }
  if (slowMs - fastMs < 250) {
    throw new Error(`200ms relay exceeded the 50ms one by ${slowMs - fastMs}ms, expected >= 250ms`);
  }
  console.log("delay: selftest passed");
}

async function waitUntil(predicate, timeoutMs, description) {
  const deadline = Date.now() + timeoutMs;
  while (!predicate()) {
    if (Date.now() > deadline) throw new Error(`timed out waiting for ${description}`);
    await sleep(10);
  }
}

// Hermetic against a throwaway echo server, on ephemeral ports throughout. Echoes before it cuts: a
// cut test that only watches the connection die would pass against a relay that never connected in
// the first place, proving nothing about severing a live pair.
async function selftestCut() {
  // A dedicated echo, not the shared startEcho: this one remembers what it received so assertion 4
  // can tell "the payload never arrived" from "it arrived but nothing was watching."
  let echoReceived = "";
  const echoServer = createServer((socket) => {
    socket.on("data", (chunk) => {
      echoReceived += chunk.toString();
      socket.write(chunk);
    });
  });
  const echoPort = await listenAsync(echoServer, 0);

  const delayMs = 150;
  const { server: relayServer, controlServer, port: relayPort, controlPort } =
    await startRelay(0, echoPort, delayMs, 0);

  const client = connect(relayPort, "127.0.0.1");
  // A destroy()-based cut can surface here as ECONNRESET once the far end goes away; expected, not
  // a self-test failure.
  client.on("error", () => {});
  let clientReceived = "";
  client.on("data", (chunk) => { clientReceived += chunk.toString(); });
  // Tracked from the moment the socket exists, not from the moment assertion 3 starts waiting: the
  // close can land within a millisecond of the destroy, well before this function gets back around
  // to awaiting it, and a once("close", ...) attached after the fact would miss an event that has
  // already fired.
  let clientClosed = false;
  client.once("close", () => { clientClosed = true; });
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error("client failed to connect to the relay")), 5000);
    client.once("connect", () => { clearTimeout(timer); resolve(); });
  });

  // Assertion 1, the load-bearing one: a payload sent now must complete a full round trip through
  // the relay before anything is cut, proving the fixture is a live connection rather than one that
  // never worked in the first place — the other three assertions mean nothing without this.
  client.write("before-cut");
  await waitUntil(() => clientReceived.includes("before-cut"), 5000, "the pre-cut echo");
  console.log("delay: pre-cut echo observed on a live connection");

  // Written while the pair is still live, so its delayed forward is already queued inside the
  // relay's pipe by the time the cut lands — an in-flight write, not one that never entered the
  // relay, is what assertion 4 needs in order to catch a destroyed-guard that only looks correct.
  client.write("after-cut");
  await sleep(20);

  // Assertion 2: the control port counts the pair it destroyed.
  const cutTriggeredAt = Date.now();
  const cutOutput = await new Promise((resolve, reject) => {
    const control = connect(controlPort, "127.0.0.1");
    let output = "";
    control.once("error", reject);
    control.on("data", (chunk) => { output += chunk.toString(); });
    control.once("close", () => resolve(output));
  });
  if (cutOutput !== "cut 1\n") {
    throw new Error(`control port reported ${JSON.stringify(cutOutput)}, expected "cut 1\\n"`);
  }
  console.log(`delay: control port reported ${cutOutput.trim()}`);

  // Assertion 3: the far end closes within 2s of the cut — the sever reaches the client, not just
  // the relay's own bookkeeping. It does not distinguish destroy() from end(): on loopback, with
  // nothing left unread or unwritten at cut time, a graceful end() produces the same observation
  // here, within a couple of milliseconds, under Node's default allowHalfOpen: false. So this
  // assertion pins that the cut severs the connection, not how — destroy() is still the right call
  // (P3 needs a genuine dropped socket, not a FIN), this assertion just isn't what proves it.
  // clientClosed may already be true here (a same-host destroy can outrun this function getting
  // back around to checking) — waitUntil treats that as an instant pass, which is correct: however
  // early it happened, it happened within budget. The deadline is anchored at cutTriggeredAt, not at
  // this line, so the round trip to read the control port's response does not pad the 2s window.
  const closeBudgetMs = Math.max(0, 2000 - (Date.now() - cutTriggeredAt));
  await waitUntil(() => clientClosed, closeBudgetMs, "the client socket to close within 2s of the cut");
  console.log(`delay: client socket closed ${Date.now() - cutTriggeredAt}ms after the cut`);

  // Assertion 4: give the in-flight write's delay timer time to fire if the destroyed guard failed
  // to catch it, then confirm it reached neither the echo server nor, echoed, the client back — the
  // pair is gone rather than merely quiet.
  await sleep(delayMs + 500);
  relayServer.close();
  if (controlServer) controlServer.close();
  echoServer.close();

  if (echoReceived.includes("after-cut")) {
    throw new Error("the echo server received a write sent after the cut — the pair was not destroyed");
  }
  if (clientReceived.includes("after-cut")) {
    throw new Error("the client observed an echo of a write sent after the cut — the pair was not destroyed");
  }
  console.log("delay: a write after the cut never echoed");

  console.log("delay: selftest-cut passed");
}

async function main() {
  if (process.argv[2] === "--selftest") {
    await selftest();
    process.exit(0);
  }
  if (process.argv[2] === "--selftest-cut") {
    await selftestCut();
    process.exit(0);
  }
  if (process.argv[2] === "cut") {
    const controlPort = Number(process.argv[3]);
    if (!Number.isFinite(controlPort)) {
      usage();
      process.exit(2);
    }
    await cutCommand(controlPort);
    process.exit(0);
  }

  const [listenPort, targetPort, delayMs, controlPortArg] = process.argv.slice(2, 6).map(Number);
  if (![listenPort, targetPort, delayMs].every(Number.isFinite)) {
    usage();
    process.exit(2);
  }
  const controlPort = Number.isFinite(controlPortArg) ? controlPortArg : undefined;

  await startRelay(listenPort, targetPort, delayMs, controlPort);
  // No process.exit here: the relay is a background process, meant to keep running until killed.
}

try {
  await main();
} catch (err) {
  console.error(`delay: ${err.message}`);
  process.exit(1);
}
