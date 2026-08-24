-- ADR-0049 §1: the device->profile edge moves out of player into a table of its own. The primary
-- key is the natural pair (device_id, player_id), not a surrogate id, so a device returning to a
-- profile it has already revoked is a primary-key violation rather than a rule somebody has to
-- remember. No ON DELETE clause and no CHECK, matching V4 and V5: whether a player row is ever
-- deleted is ADR-0039's, and the default NO ACTION forces a deletion feature to say what happens
-- to bindings instead of cascading silently.
CREATE TABLE device_binding (
    device_id  TEXT        NOT NULL,
    player_id  UUID        NOT NULL REFERENCES player (id),
    bound_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT device_binding_pkey PRIMARY KEY (device_id, player_id)
);

-- ADR-0049 §1: device_binding_live_device is ADR-0012's one-profile-per-device rule, narrowed to
-- live bindings -- it replaces player_device_id_unique exactly, and it is still the database, not
-- application-level locking, that resolves a race between two first contacts. device_binding_live_player
-- fixes one live binding per player: a player holding several devices at once is a feature nobody
-- has asked for, and restricting now is reversible (drop an index), where permitting now and
-- restricting later means deleting rows somebody is using. No index here is not also a constraint:
-- these are the two indexes the two hot reads need -- WHERE device_id = ? AND revoked_at IS NULL
-- on every anonymous connection, WHERE player_id = ? AND revoked_at IS NULL for the account screen.
CREATE UNIQUE INDEX device_binding_live_device
    ON device_binding (device_id) WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX device_binding_live_player
    ON device_binding (player_id) WHERE revoked_at IS NULL;

-- ADR-0049 §8: every existing row is a live binding, because that is what player.device_id
-- NOT NULL UNIQUE meant. bound_at is backfilled from player.created_at -- the binding really was
-- created with the profile -- and revoked_at stays null. This runs before the column it reads is
-- dropped below: Flyway wraps this whole file in one transaction, so there is no committed state in
-- which the copy has not happened yet and the column is already gone. Both partial unique indexes
-- above are satisfiable by construction, since device_id was UNIQUE and each player had exactly one.
INSERT INTO device_binding (device_id, player_id, bound_at)
SELECT device_id, id, created_at FROM player;

-- ADR-0049 §1 and §7: player.device_id and player_device_id_unique are dropped, not kept "for
-- history" -- a second copy of the edge is a fact stored twice that can disagree, and an
-- unmaintained copy is worse than none. Dropped only now, after the backfill above has read it.
ALTER TABLE player DROP CONSTRAINT player_device_id_unique;
ALTER TABLE player DROP COLUMN device_id;

-- ADR-0049 §2: revocation is final. NULL -> a timestamp succeeds once; timestamp -> NULL raises;
-- timestamp -> a different timestamp raises. There is no un-revoking, from the endpoint, from a
-- future admin path, or from a psql session. The error code is restrict_violation (23001), matched
-- on the code and never on the message, as ADR-0029 §4 established for the display-name permanence
-- trigger this one repeats the shape of.
CREATE FUNCTION device_binding_revocation_is_final() RETURNS trigger AS $$
BEGIN
    IF OLD.revoked_at IS NOT NULL AND NEW.revoked_at IS DISTINCT FROM OLD.revoked_at THEN
        RAISE EXCEPTION 'a revoked device binding is final (ADR-0049)'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ADR-0049 §2: OF revoked_at means the trigger fires only on statements naming that column, exactly
-- as ADR-0029 §4's does -- resolve only ever inserts into this table and never updates it, so the
-- identity hot path pays nothing for this trigger's existence.
CREATE TRIGGER device_binding_revocation_final
    BEFORE UPDATE OF revoked_at ON device_binding
    FOR EACH ROW EXECUTE FUNCTION device_binding_revocation_is_final();
