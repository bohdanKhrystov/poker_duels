-- ADR-0029: a display name is unique and permanent once set. The column is nullable; a player
-- profile exists before a name is chosen. The unique fold uses ICU's root locale to avoid
-- host-dependent collation differences between the test container and production deployment.
--
-- Permanence is enforced by a trigger, not application logic alone, so it holds against
-- migrations, direct database access, and any future write path that may be added.

ALTER TABLE player ADD COLUMN display_name TEXT
    CONSTRAINT player_display_name_length CHECK (char_length(display_name) BETWEEN 1 AND 32);

ALTER TABLE player ADD CONSTRAINT player_display_name_trimmed
    CHECK (display_name = btrim(display_name));

ALTER TABLE player ADD CONSTRAINT player_display_name_nfc
    CHECK (display_name IS NFC NORMALIZED);

CREATE UNIQUE INDEX player_display_name_unique
    ON player (lower(display_name COLLATE "und-x-icu"));

CREATE FUNCTION player_display_name_is_permanent() RETURNS trigger AS $$
BEGIN
    IF OLD.display_name IS NOT NULL AND NEW.display_name IS DISTINCT FROM OLD.display_name THEN
        RAISE EXCEPTION 'display_name is permanent once set (ADR-0029)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER player_display_name_permanent
    BEFORE UPDATE OF display_name ON player
    FOR EACH ROW EXECUTE FUNCTION player_display_name_is_permanent();
