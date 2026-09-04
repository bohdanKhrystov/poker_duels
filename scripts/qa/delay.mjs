// A loopback TCP relay: forwards 127.0.0.1:<listenPort> to 127.0.0.1:<targetPort>, delaying every
// byte in both directions by a fixed <delayMs> — wide enough to make the STORY-1310 round trip
// visible past the 250ms drive.mjs samples at. No dependency (ADR-0089 §2a): node:net is the whole
// toolkit, same discipline as drive.mjs, and this runs before `npm ci` in a QA context.
//
//   node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs>
//   node scripts/qa/delay.mjs --selftest
//
// Exit codes follow drive.mjs's convention: 2 is usage. The relay never exits on its own — it is a
// background process, killed by whoever started it.

import { createServer, connect } from "node:net";

function usage() {
  console.error("usage: node scripts/qa/delay.mjs <listenPort> <targetPort> <delayMs>");
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

async function startRelay(listenPort, targetPort, delayMs) {
  const server = createServer((incoming) => {
    console.error(`delay: accepted connection on ${listenPort}, relaying to 127.0.0.1:${targetPort}`);
    const outgoing = connect(targetPort, "127.0.0.1");
    pipe(incoming, outgoing, delayMs);
    pipe(outgoing, incoming, delayMs);
  });
  return { server, port: await listenAsync(server, listenPort) };
}

// A throwaway echo server for the self-test below: whatever it receives, it writes straight back,
// with no delay of its own — the only delay in the measured round trip comes from the relay.
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

async function main() {
  if (process.argv[2] === "--selftest") {
    await selftest();
    process.exit(0);
  }

  const [listenPort, targetPort, delayMs] = process.argv.slice(2, 5).map(Number);
  if (![listenPort, targetPort, delayMs].every(Number.isFinite)) {
    usage();
    process.exit(2);
  }

  await startRelay(listenPort, targetPort, delayMs);
  // No process.exit here: the relay is a background process, meant to keep running until killed.
}

try {
  await main();
} catch (err) {
  console.error(`delay: ${err.message}`);
  process.exit(1);
}
