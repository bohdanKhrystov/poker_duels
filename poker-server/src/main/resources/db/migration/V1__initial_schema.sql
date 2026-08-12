-- Once this migration has run anywhere, Flyway records its checksum in
-- flyway_schema_history. Editing this file after that makes the recorded checksum a lie
-- and fails validation on the next startup. Every later change is a new V<n>__ file;
-- nothing in this repository ever edits a merged migration.

CREATE TABLE player (
    id           UUID        PRIMARY KEY,
    device_id    TEXT        NOT NULL,
    -- ADR-0014: coin balances are signed. A balance is wins minus losses, so a profile
    -- whose only duel was a loss reads back -1. No CHECK floors this at zero.
    coin_balance INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT player_device_id_unique UNIQUE (device_id)
);

CREATE TABLE duel (
    id          UUID        PRIMARY KEY,
    format      TEXT        NOT NULL,
    started_at  TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE duel_result (
    duel_id    UUID    NOT NULL REFERENCES duel (id),
    player_id  UUID    NOT NULL REFERENCES player (id),
    -- ADR-0014: signed, like coin_balance above — winner +1, loser -1, draw absent (no row).
    coin_delta INTEGER NOT NULL,
    CONSTRAINT duel_result_pkey PRIMARY KEY (duel_id, player_id)
);
