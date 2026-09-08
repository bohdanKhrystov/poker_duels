-- ADR-0134 §3: name_registry gains a fourth reason, REPLACED, meaning "its holder replaced it" --
-- RETIRED keeps its one meaning, "an operator took it away". Both are terminal, neither may become
-- the other, and the folded index is indifferent to reason, so ADR-0130 §2 holds in full: a spent
-- string is spent for everyone regardless of which of the two ways it was spent.
ALTER TABLE name_registry DROP CONSTRAINT name_registry_reason;
ALTER TABLE name_registry ADD CONSTRAINT name_registry_reason
    CHECK (reason IN ('TAKEN', 'BLOCKED', 'RETIRED', 'REPLACED'));

ALTER TABLE name_registry DROP CONSTRAINT name_registry_retired_from;
ALTER TABLE name_registry ADD CONSTRAINT name_registry_retired_from
    CHECK (retired_from IS NULL OR reason IN ('RETIRED', 'REPLACED'));

-- ADR-0134 §3: the only permitted transition remains TAKEN -> a terminal reason, but there are now
-- two terminal reasons instead of one. name and created_at stay unrewritable, and nothing is
-- reachable from either terminal state -- ADR-0051 §1's monotonicity is not weakened, it is widened
-- by exactly one more terminal value.
CREATE OR REPLACE FUNCTION name_registry_is_monotone() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.reason <> 'BLOCKED' THEN
            RAISE EXCEPTION 'a name that has been held is never released (ADR-0051)'
                USING ERRCODE = 'restrict_violation';
        END IF;
        RETURN OLD;
    END IF;
    IF NEW.name <> OLD.name
       OR OLD.reason <> 'TAKEN'
       OR NEW.reason NOT IN ('RETIRED', 'REPLACED') THEN
        RAISE EXCEPTION 'a registered name may only go TAKEN to RETIRED or REPLACED (ADR-0134)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ADR-0134 §1: the trigger stops saying permanent and says never released. ADR-0130 removed the
-- one-write-per-lifetime rule that made "permanent" true; what actually has to survive is
-- non-release, which ADR-0051 §3 already stated as an exception -- "a name may be given up only by
-- being spent" -- and which is now the whole of the rule rather than a carve-out from it. The body
-- is ADR-0051 §3's with one disjunct deleted, the one that let a null-out through unconditionally:
-- a name -> a different name now succeeds whenever the string being left is already spent, by
-- either terminal reason, in the same transaction. No psql session, migration, admin tool or second
-- write path can move a name off a player without retiring or replacing it first.
DROP TRIGGER player_display_name_permanent ON player;
DROP FUNCTION player_display_name_is_permanent();

CREATE FUNCTION player_display_name_is_never_released() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        IF NOT EXISTS (
            SELECT 1 FROM name_registry
             WHERE name = OLD.display_name AND reason IN ('RETIRED', 'REPLACED')
        ) THEN
            RAISE EXCEPTION 'a display name is spent before it is left (ADR-0051, ADR-0134)'
                USING ERRCODE = 'restrict_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER player_display_name_never_released
    BEFORE UPDATE OF display_name ON player
    FOR EACH ROW EXECUTE FUNCTION player_display_name_is_never_released();
