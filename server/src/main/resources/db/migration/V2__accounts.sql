-- Accounts. A user is found by the provider's stable subject (Apple `sub`, Google `sub`);
-- Apple and Google sign-ins are separate accounts (emails are not used to link them).
CREATE TABLE users (
    id         TEXT   PRIMARY KEY,
    apple_sub  TEXT   UNIQUE,
    google_sub TEXT   UNIQUE,
    email      TEXT,
    created_at BIGINT NOT NULL
);

-- Only a SHA-256 hash of each refresh token is stored. Single-use: refreshing revokes it.
CREATE TABLE refresh_tokens (
    token_hash    TEXT   PRIMARY KEY,
    user_id       TEXT   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at    BIGINT NOT NULL,
    revoked_at    BIGINT,
    -- 'rotated' (replaced by a refresh), 'logout', or 'reuse' (whole account signed out).
    revoke_reason TEXT
);

CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

-- Rows written with dev auth before accounts existed get an owner, then deleting a user
-- deletes their data.
INSERT INTO users (id, created_at) SELECT DISTINCT user_id, 0 FROM records ON CONFLICT DO NOTHING;
ALTER TABLE records ADD CONSTRAINT records_user_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
