# Protocol version ledger

One row per `PROTOCOL_VERSION`, naming the 16-hex fingerprint of the wire shape that number means.
A row is written by hand — see [`ADR-0047`](adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md)
for why there is deliberately no command that writes one. The ledger starts at **2**: it was
introduced after version 1's shape had already been replaced, and that shape is not recoverable.

| Version | Wire fingerprint | Claimed by | Landed |
| --- | --- | --- | --- |
| 2 | `d3728722cc4a0efa` | STORY-0202 | 2026-08-12 |
| 3 | `5e6bcd90d8a2d391` | STORY-0213 | 2026-08-23 |
