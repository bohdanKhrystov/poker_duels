# Operations

This document is a call site for the operator who holds the database credentials. It shows how to
find a player, take a name away, and curate the blocklist from `psql`. The procedure lives in the
migration (`V5__name_registry.sql`), and this is the only place it is called.

## Find the player and the exact string they hold

```sql
SELECT id, display_name FROM player WHERE display_name IS NOT NULL AND lower(display_name COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu");
```

Run this query with the display name you are looking for. It returns the player's `id` and their
exact stored `display_name` — the string in the case and normalization they set it in. Use both
values in the next step.

## Take the name away

```sql
SELECT retire_display_name('<player id>', '<the name they hold>');
```

The function takes two arguments: the player's `id` and the exact string you found above. It returns
the name it took. A mismatch between the stored name and the expected name raises an exception and
writes nothing.

**A takedown cannot be undone.** Nobody can un-retire a name. The victim of a mistake cannot
reclaim their own name, and the only remedy is that they choose a different one. The interlock —
requiring both player id and expected name to match — is the whole mitigation. Check before you run
the command.

The player is told (see `ADR-0052` §2's notice) the next time they open the name surface. Nobody
else is told anything.

## Add a blocklist entry

```sql
INSERT INTO name_registry (name, reason) VALUES (normalize(btrim($1), NFC), 'BLOCKED');
```

Supply the exact string you want to block. The `normalize` and `btrim` functions apply the same
canonicalisation that the write path uses.

**A blocklist entry cannot be added over a name in use.** The insert raises error code `23505` (a
unique constraint violation) if a player currently holds that exact string. When this happens, your
two options are to leave the name in the list unchanged, or to retire it from its holder first (see
above).

**Nothing is re-screened.** A name already held when its string is blocked stays held until an
operator retires it explicitly. The blocklist is a set-time event, consulted only when a player
attempts to set a name. Screening is never retroactive.

## Remove a blocklist entry

```sql
DELETE FROM name_registry WHERE name = $1 AND reason = 'BLOCKED';
```

Supply the exact string. This removes only `BLOCKED` entries. A name in any other state (taken or
retired) cannot be deleted, and the database enforces this.

---

The operator is whoever holds the database credentials. There is no role, no account, no endpoint,
and no Gradle task. There will not be one until there is a second operator — at that point, the
answer is a database role, not a feature.
