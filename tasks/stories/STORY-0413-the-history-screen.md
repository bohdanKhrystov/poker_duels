---
id: STORY-0413
title: The history screen — pages, filters, search
type: story
status: backlog
parent: EPIC-04
module: web-client
labels: [client, ui, history]
depends_on: [STORY-0409, STORY-0411]
---

## Goal

A player can read their whole duel record on a screen: page by page, narrowed by outcome, and
searched by opponent — each row naming the opponent, the outcome, the coin it moved, the hands it
lasted and when it finished.

## Why

`STORY-0311`'s strip shows a handful of recent results and was never meant to be the record.
`STORY-0408` and `STORY-0409` make the whole record readable; this is where a person reads it.

## Design notes

- **The client derives nothing and sorts nothing.** Rows render in the order the server sent them —
  a client-side `sort` or `reverse` is exactly the defect `TASK-031112` pinned for the strip, and the
  same fixture discipline applies: a fixture monotone in no field, asserted with `toEqual` over a
  mapped array.
- **The cursor is opaque.** The client stores what the server sent and hands it back; it never
  constructs, parses or increments one, and never derives a page number from a row count.
- **A filter change discards the cursor**, because a cursor belongs to the filter that produced it
  (`STORY-0409`). Requesting page two of a filter with page one's cursor from another filter is a
  bug the screen must make unreachable.
- **Every state is a state**: loading, empty-because-no-duels, empty-because-the-filter-matched-
  nothing, and failed. The last two say different things — *you have played no duels* and *no duel
  matches this* are different facts about the world.
- **Nothing derives the outcome from the coin delta.** The server sends `outcome`; the row prints it.
  `STORY-0311`'s `profile-text` words are reused rather than re-authored.
- No test sleeps on a real clock; paging and search are asserted against a fake transport, not a
  timer.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0413` once `STORY-0409` and `STORY-0411` have merged.* | — |

## Acceptance criteria

- [ ] Rows render in the order received, proven with a fixture monotone in no field.
- [ ] Asking for the next page appends the server's next page and asks with the server's cursor,
      byte-identical to what was received.
- [ ] Reaching the last page stops offering another, and no request is made after it.
- [ ] Each of the three outcome filters requests the filter and renders what came back; changing a
      filter drops the cursor and asks for the first page.
- [ ] Searching sends the term the player typed, unmodified, and renders the result.
- [ ] The four states each render their own words, and the two empty states differ.
- [ ] An opponent with no name renders the same treatment `STORY-0411` chose, not a player id.
- [ ] The suite's own count is asserted, and no test sleeps on a real clock.

## Out of scope

- Replaying a duel — the log is not persisted (`DEC-008`), and the viewer is v0.4.
- Another player's history — `EPIC-05`.
- The visual language — `EPIC-06`.
