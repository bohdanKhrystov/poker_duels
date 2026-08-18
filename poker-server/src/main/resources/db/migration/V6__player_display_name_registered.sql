-- ADR-0051 §1: without this key, "every name ever held is in the registry" is a promise made by
-- one Kotlin file; with it, no write path -- including psql and a test fixture -- can land a
-- display_name that was never registered. No ON DELETE clause, matching V4: NO ACTION forces a
-- future deletion feature (ADR-0039) to say out loud what happens to a profile's spent names. Not
-- DEFERRABLE: PostgresProfileWrites registers the string before it writes the column, in the same
-- transaction, so an immediate check is satisfied and deferring would only hide an ordering
-- mistake until commit.
ALTER TABLE player ADD CONSTRAINT player_display_name_registered
    FOREIGN KEY (display_name) REFERENCES name_registry (name);
