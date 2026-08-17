-- ADR-0027: the credential table stores authentication secrets and the auth_session table
-- stores active authentication sessions. No ON DELETE clause on foreign keys: whether a player
-- row is ever deleted is DEC-029, and the default NO ACTION forces a deletion feature to state
-- out loud what happens to credentials and sessions instead of silently cascading.
--
-- secret_hash is nullable because whether a credential carries a server-held secret depends
-- on its kind. This shape holds a password, a passphrase against a handle, or a third-party
-- subject, without preferring any of them.

CREATE TABLE credential (
    id          UUID        PRIMARY KEY,
    player_id   UUID        NOT NULL REFERENCES player (id),
    kind        TEXT        NOT NULL,
    identifier  TEXT        NOT NULL,
    secret_hash TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT credential_kind_identifier_unique UNIQUE (kind, identifier)
);

CREATE TABLE auth_session (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX auth_session_player_id_idx ON auth_session (player_id);
