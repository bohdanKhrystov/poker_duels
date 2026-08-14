/**
 * The duel socket's URL on this page's own origin.
 *
 * Relative by construction: `ADR-0026` proxies `/ws` to Ktor in development
 * and production is same-origin, so there is no base-URL knob to configure
 * and nothing for an environment to get wrong.
 */
export function socketUrl(location: {
  protocol: string;
  host: string;
}): string {
  const wsProtocol = location.protocol === "https:" ? "wss:" : "ws:";
  return `${wsProtocol}//${location.host}/ws`;
}
