-- ADR-0051 §1: name_registry is the whole display-name namespace, one table where "in use",
-- "blocked" and "retired" are three values of one reason column behind one index. That closes the
-- race a check split across player's unique index plus two more tables would open, and the fold,
-- the three CHECKs and the trimmed rule repeat ADR-0029 §1 exactly so the registry refuses what
-- player.display_name would have refused. No ON DELETE clause, matching V4 and ADR-0049 §1: whether
-- a player row is ever deleted is ADR-0039's, and NO ACTION forces a deletion feature to say what
-- happens to the names that profile has spent.
CREATE TABLE name_registry (
    name         TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    retired_from UUID        REFERENCES player (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT name_registry_pkey PRIMARY KEY (name),
    CONSTRAINT name_registry_reason CHECK (reason IN ('TAKEN', 'BLOCKED', 'RETIRED')),
    CONSTRAINT name_registry_retired_from CHECK (retired_from IS NULL OR reason = 'RETIRED'),
    CONSTRAINT name_registry_length CHECK (char_length(name) BETWEEN 1 AND 32),
    CONSTRAINT name_registry_trimmed CHECK (name = btrim(name)),
    CONSTRAINT name_registry_nfc CHECK (name IS NFC NORMALIZED)
);

-- ADR-0051 §1: ADR-0029 §1's fold, character for character, so a case variant collides here exactly
-- as it would against player.display_name -- comparing like with like is why it is repeated rather
-- than assumed.
CREATE UNIQUE INDEX name_registry_folded
    ON name_registry (lower(name COLLATE "und-x-icu"));

-- ADR-0053 §3: partial on retired_from IS NOT NULL, so the profile read's EXISTS against the rare,
-- ever-growing retired set stays an index probe. It costs the hot write path nothing: a TAKEN row
-- has retired_from IS NULL and writes no entry.
CREATE INDEX name_registry_retired_from_idx
    ON name_registry (retired_from) WHERE retired_from IS NOT NULL;

-- ADR-0051 §8: every name already held is grandfathered as TAKEN, so no existing player loses a
-- name and no existing name becomes claimable. Every test database's player table is empty at this
-- point, so this inserts zero rows in this repository; it is here for a database with rows in it.
INSERT INTO name_registry (name, reason)
SELECT display_name, 'TAKEN' FROM player WHERE display_name IS NOT NULL;

-- ADR-0051 §3: the registry only grows and the only permitted update is TAKEN -> RETIRED, so
-- "retired forever" does not rest on nobody running a DELETE. A BLOCKED row may be deleted because a
-- curated list must stay correctable; nothing else may be deleted and no name may ever be rewritten.
CREATE FUNCTION name_registry_is_monotone() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.reason <> 'BLOCKED' THEN
            RAISE EXCEPTION 'a name that has been held is never released (ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
        RETURN OLD;
    END IF;
    IF NEW.name <> OLD.name OR OLD.reason <> 'TAKEN' OR NEW.reason <> 'RETIRED' THEN
        RAISE EXCEPTION 'the only change a registered name may take is TAKEN to RETIRED (ADR-0051)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER name_registry_monotone
    BEFORE UPDATE OR DELETE ON name_registry
    FOR EACH ROW EXECUTE FUNCTION name_registry_is_monotone();

-- ADR-0051 §3: replaces V3's function body only -- the trigger binds by name and is not re-created
-- here. The permanence trigger gains exactly one exception, scoped to the transition (name -> NULL)
-- and never to who is connected, to a role, or to an elevated-privilege wrapper: a name may be given
-- up only by being spent, so the transition that leaves the player nameless is the same transition
-- that retires the string forever. "name -> a different name" still raises for everybody, operator
-- included.
CREATE OR REPLACE FUNCTION player_display_name_is_permanent() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        IF NEW.display_name IS NOT NULL OR NOT EXISTS (
            SELECT 1 FROM name_registry
             WHERE name = OLD.display_name AND reason = 'RETIRED'
        ) THEN
            RAISE EXCEPTION 'display_name is permanent once set (ADR-0029, ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ADR-0051 §4: the takedown, called from a psql session against the credentials the server already
-- uses -- runs with the caller's own privileges, grants nothing, and no Kotlin ever references it.
-- The registry row is promoted to RETIRED before the player's column is nulled, in that order:
-- reversed, the trigger above raises because its exception looks for a RETIRED row that does not yet
-- exist. expected_name is the interlock: two independent facts about one row must agree before
-- anything is written, which turns a wrong-database or mistyped-id accident into a loud error rather
-- than a silent takedown.
CREATE FUNCTION retire_display_name(target_player UUID, expected_name TEXT)
RETURNS TEXT AS $$
DECLARE
    held TEXT;
BEGIN
    SELECT display_name INTO held FROM player WHERE id = target_player FOR UPDATE;
    IF NOT FOUND OR held IS NULL THEN
        RAISE EXCEPTION 'player % holds no display name', target_player
            USING ERRCODE = 'no_data_found';
    END IF;
    IF lower(held COLLATE "und-x-icu")
       IS DISTINCT FROM lower(btrim(normalize(expected_name, NFC)) COLLATE "und-x-icu") THEN
        RAISE EXCEPTION 'player % does not hold that name', target_player
            USING ERRCODE = 'restrict_violation';
    END IF;

    UPDATE name_registry SET reason = 'RETIRED', retired_from = target_player WHERE name = held;
    UPDATE player SET display_name = NULL WHERE id = target_player;
    RETURN held;
END;
$$ LANGUAGE plpgsql;
