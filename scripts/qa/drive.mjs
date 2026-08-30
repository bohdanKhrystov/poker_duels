// The QA agent's hands: one Chrome profile, driven over the DevTools protocol.
//
// No npm package and no dependency in web-client/package.json — ADR-0088 §1 forbids that by name
// and DEC-082 does not reopen it. Node's built-in WebSocket and fetch are the whole toolkit.
//
// Each invocation reattaches to the profile's existing page, so state survives between commands
// and every verb is a separate process. That is what lets an agent drive a browser from a shell.
//
//   node scripts/qa/drive.mjs <port> <verb> [args...]
//
// Verbs are listed under `usage` at the bottom. Exit code 0 means the verb succeeded; 1 means it
// did not (a wait that timed out, a control that was not there). A QA case reads the exit code,
// never the prose.

import { writeFileSync } from "node:fs";

const [, , portArg, verb, ...args] = process.argv;
const PORT = portArg;
const APP = "http://localhost:5173/";

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const fail = (msg) => {
  console.error(`drive: ${msg}`);
  process.exit(1);
};

async function targets() {
  const r = await fetch(`http://localhost:${PORT}/json/list`).catch(() => null);
  if (!r) fail(`no browser on port ${PORT}`);
  return r.json();
}

async function attach({ fresh = false } = {}) {
  let t;
  if (fresh) {
    t = await (await fetch(`http://localhost:${PORT}/json/new?url=about:blank`, { method: "PUT" })).json();
  } else {
    const list = await targets();
    t = list.find((x) => x.type === "page" && x.url.includes("localhost:5173"));
    if (!t) t = list.find((x) => x.type === "page");
    if (!t) t = await (await fetch(`http://localhost:${PORT}/json/new?url=about:blank`, { method: "PUT" })).json();
  }
  const ws = new WebSocket(t.webSocketDebuggerUrl);
  let id = 0;
  const pending = new Map();
  await new Promise((res, rej) => {
    ws.addEventListener("open", res);
    ws.addEventListener("error", () => rej(new Error("cdp socket failed")));
  });
  ws.addEventListener("message", (e) => {
    const m = JSON.parse(e.data);
    if (m.id && pending.has(m.id)) {
      pending.get(m.id)(m);
      pending.delete(m.id);
    }
  });
  const send = (method, params = {}) =>
    new Promise((res) => {
      const i = ++id;
      pending.set(i, res);
      ws.send(JSON.stringify({ id: i, method, params }));
    });
  await send("Page.enable");
  await send("Runtime.enable");
  const evaluate = async (expression) => {
    const o = await send("Runtime.evaluate", { expression, returnByValue: true, awaitPromise: true });
    if (o.result?.exceptionDetails) {
      return { error: o.result.exceptionDetails.text || "exception" };
    }
    return { value: o.result?.result?.value };
  };
  return { ws, send, evaluate, url: t.url };
}

const ROOT_TEXT = "(document.getElementById('root')?.innerText ?? '')";

// A control is found by the visible text it starts with, among enabled buttons and anchors.
// startsWith rather than includes: "Call 100" and "All in 10,000" both contain digits that change
// every hand, and a substring match on "Call" would also hit a button labelled "Recall".
const clickExpr = (label) => `(() => {
  const allEls = [...document.querySelectorAll('button, a, [role=button]')];
  const els = allEls.filter(e => !e.disabled && e.offsetParent !== null);
  const want = ${JSON.stringify(label)}.toLowerCase();
  const el = els.find(e => (e.innerText || '').trim().toLowerCase().startsWith(want));
  if (!el) {
    const hiddenMatches = allEls.filter(e => (e.innerText || '').trim().toLowerCase().startsWith(want) && (e.disabled || e.offsetParent === null));
    const saw = els.map(e => (e.innerText || '').trim().split('\\n')[0]);
    return { ok: false, saw, hiddenMatches: hiddenMatches.length };
  }
  el.click();
  return { ok: true, clicked: (el.innerText || '').trim().split('\\n')[0] };
})()`;

async function waitFor(page, predicateExpr, timeoutMs, what) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const { value } = await page.evaluate(predicateExpr);
    if (value) return true;
    await sleep(250);
  }
  const { value: seen } = await page.evaluate(`${ROOT_TEXT}.slice(0, 400)`);
  console.error(`drive: timed out after ${timeoutMs}ms waiting for ${what}`);
  console.error(`--- screen was ---\n${seen}`);
  return false;
}

let page;
try {
  switch (verb) {
    case "open": {
      page = await attach({ fresh: false });
      const url = args[0] || APP;
      await page.send("Page.navigate", { url });
      // The socket opens after the document does; a click dispatched before the store is wired
      // lands on a control that silently does nothing. Wait for the tree, not for a guessed sleep.
      const ok = await waitFor(page, `document.getElementById('root')?.innerHTML.length > 50`, 20000, "#root to render");
      if (!ok) fail("page never rendered");
      const { value } = await page.evaluate(`${ROOT_TEXT}.slice(0, 600)`);
      console.log(value);
      break;
    }

    case "text": {
      page = await attach();
      const n = Number(args[0] || 900);
      const { value } = await page.evaluate(`${ROOT_TEXT}.slice(0, ${n})`);
      console.log(value);
      break;
    }

    case "click": {
      page = await attach();
      const label = args[0] ?? fail("click needs a label");
      const { value } = await page.evaluate(clickExpr(label));
      if (!value?.ok) {
        if (value?.hiddenMatches > 0) {
          console.error(`drive: found ${value.hiddenMatches} match(es) for ${JSON.stringify(label)}, all invisible`);
        } else {
          console.error(`drive: no visible control starting with ${JSON.stringify(label)}`);
          console.error(`drive: visible controls: ${JSON.stringify(value?.saw ?? [])}`);
        }
        process.exit(1);
      }
      console.log(`clicked: ${value.clicked}`);
      break;
    }

    case "wait": {
      page = await attach();
      const needle = args[0] ?? fail("wait needs text");
      const timeout = Number(args[1] || 20000);
      const expr = `${ROOT_TEXT}.toLowerCase().includes(${JSON.stringify(needle.toLowerCase())})`;
      if (!(await waitFor(page, expr, timeout, JSON.stringify(needle)))) process.exit(1);
      console.log(`saw: ${needle}`);
      break;
    }

    case "absent": {
      // Proves a negative for a bounded window — the shape a secrecy case needs.
      page = await attach();
      const needle = args[0] ?? fail("absent needs text");
      const forMs = Number(args[1] || 3000);
      const deadline = Date.now() + forMs;
      const expr = `${ROOT_TEXT}.toLowerCase().includes(${JSON.stringify(needle.toLowerCase())})`;
      while (Date.now() < deadline) {
        const { value } = await page.evaluate(expr);
        if (value) {
          const { value: seen } = await page.evaluate(`${ROOT_TEXT}.slice(0, 400)`);
          console.error(`drive: ${JSON.stringify(needle)} APPEARED within ${forMs}ms`);
          console.error(`--- screen ---\n${seen}`);
          process.exit(1);
        }
        await sleep(250);
      }
      console.log(`absent for ${forMs}ms: ${needle}`);
      break;
    }

    case "type": {
      page = await attach();
      const nth = Number(args[0] ?? 0);
      const value = args[1] ?? "";
      // React tracks input value on the DOM node; setting .value directly is not seen by it, so
      // the native setter plus a bubbling input event is the only reliable way in.
      const { value: out } = await page.evaluate(`(() => {
        const els = [...document.querySelectorAll('input, textarea')];
        const el = els[${nth}];
        if (!el) return { ok: false, count: els.length };
        const proto = el instanceof HTMLTextAreaElement ? HTMLTextAreaElement : HTMLInputElement;
        const setter = Object.getOwnPropertyDescriptor(proto.prototype, 'value').set;
        setter.call(el, ${JSON.stringify(value)});
        el.dispatchEvent(new Event('input', { bubbles: true }));
        return { ok: true };
      })()`);
      if (!out?.ok) fail(`no input at index ${nth} (found ${out?.count ?? 0})`);
      console.log(`typed into input ${nth}`);
      break;
    }

    case "link": {
      page = await attach();
      const { value } = await page.evaluate(`(() => {
        const fromInput = [...document.querySelectorAll('input, textarea')]
          .map(e => e.value).find(v => v && v.includes('room='));
        if (fromInput) return fromInput;
        const m = ${ROOT_TEXT}.match(/https?:\\/\\/[^\\s]*room=[A-Za-z0-9]+/i);
        return m ? m[0] : null;
      })()`);
      if (!value) fail("no invite link on screen");
      console.log(value);
      break;
    }

    case "device": {
      page = await attach();
      const { value } = await page.evaluate("localStorage.getItem('pd.deviceId')");
      console.log(value ?? "");
      break;
    }

    case "forget-room": {
      // ADR-0072 keeps a tab in its room. A profile that played before reloads back into the old
      // room instead of the lobby, and every later case then fails for the wrong reason.
      page = await attach();
      await page.evaluate("localStorage.removeItem('pd.roomCode')");
      console.log("forgot pd.roomCode");
      break;
    }

    case "eval": {
      page = await attach();
      const { value, error } = await page.evaluate(args[0] ?? "null");
      if (error) fail(error);
      console.log(typeof value === "string" ? value : JSON.stringify(value));
      break;
    }

    case "close": {
      // Close the app tab, leaving the browser alive by creating a new about:blank tab first.
      // This is how a player closing their tab appears to the server — the WebSocket closes.
      const blank = await (await fetch(`http://localhost:${PORT}/json/new?url=about:blank`, { method: "PUT" })).json();
      const list = await targets();
      const appTab = list.find((x) => x.type === "page" && x.url.includes("localhost:5173"));
      if (!appTab) {
        console.error(`drive: no app tab to close`);
        process.exit(1);
      }
      const closeResp = await fetch(`http://localhost:${PORT}/json/close/${appTab.id}`);
      if (!closeResp.ok) {
        console.error(`drive: failed to close tab`);
        process.exit(1);
      }
      console.log("closed app tab");
      break;
    }

    case "shot": {
      page = await attach();
      const path = args[0] ?? fail("shot needs a path");
      const result = await page.send("Page.captureScreenshot", {});
      const data = result.result.data;
      const buffer = Buffer.from(data, "base64");
      writeFileSync(path, buffer);
      console.log(path);
      break;
    }

    default:
      console.error(`usage: node scripts/qa/drive.mjs <port> <verb> [args]

  open [url]              navigate and wait for #root to render
  text [chars]            the rendered text of #root
  click <label>           click the enabled control whose text starts with <label>
  wait <text> [ms]        block until text appears           (exit 1 on timeout)
  absent <text> [ms]      assert text does NOT appear for ms (exit 1 if it does)
  type <index> <value>    fill the nth input, React-safely
  link                    the invite link on screen
  device                  this profile's pd.deviceId
  forget-room             clear pd.roomCode (ADR-0072 room memory)
  eval <expression>       escape hatch
  shot <path>             write the rendered screen to <path> as a PNG
  close                   end this profile's app session (closes WebSocket, keeps browser alive)`);
      process.exit(2);
  }
} finally {
  try {
    page?.ws.close();
  } catch {
    /* nothing to close */
  }
}
