-- ADR-0031. An address in this table exists for exactly one purpose: recovering a password.
-- It is never used for contact, notification or marketing. One row per player, so this table
-- cannot become a mailing list; one player per address, so a reset is never ambiguous. Only
-- verified addresses live here — an unproven address is a row in email_verification.
CREATE TABLE recovery_email (
    player_id   UUID        PRIMARY KEY REFERENCES player (id),
    address     TEXT        NOT NULL,
    verified_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX recovery_email_address_unique
    ON recovery_email (lower(address COLLATE "und-x-icu"));

CREATE TABLE email_verification (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    address    TEXT        NOT NULL,
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT email_verification_one_per_player UNIQUE (player_id)
);

CREATE TABLE password_reset (
    token_hash BYTEA       PRIMARY KEY,
    player_id  UUID        NOT NULL REFERENCES player (id),
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT password_reset_one_per_player UNIQUE (player_id)
);
