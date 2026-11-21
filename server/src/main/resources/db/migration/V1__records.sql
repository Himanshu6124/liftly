-- One generic table for every synced app table (core/protocol SyncProtocol.TABLES).
-- payload is the entity's own columns; the server never interprets it.
CREATE SEQUENCE record_version_seq;

CREATE TABLE records (
    user_id    TEXT   NOT NULL,
    tbl        TEXT   NOT NULL,
    id         TEXT   NOT NULL,
    payload    JSONB  NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT,
    -- From record_version_seq on every accepted write; the pull cursor is the last version seen.
    version    BIGINT NOT NULL,
    PRIMARY KEY (user_id, tbl, id)
);

CREATE INDEX records_pull_idx ON records (user_id, tbl, version);
