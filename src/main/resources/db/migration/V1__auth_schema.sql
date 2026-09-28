-- Auth/session schema for the User -> UserDevice -> RefreshToken model.
--
-- This is the first Flyway migration in the project. It assumes the dev
-- database's baseline tables (users, roles, user_roles, addresses, ...) were
-- already created by Hibernate's previous ddl-auto=update and are otherwise
-- left untouched here. From this point on, ddl-auto is "validate" and Flyway
-- owns schema changes.
--
-- user_device and refresh_token are dropped and recreated because their
-- shape changes (token_hash instead of a raw token, revoked_at, replaced_by_id,
-- explicit indexes). This is a one-time, destructive reset of session state
-- only - acceptable pre-production since sessions are re-issued on next login.

ALTER TABLE users DROP COLUMN IF EXISTS confirm_password;

DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS user_device;

CREATE TABLE user_device (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    device_id       VARCHAR(255) NOT NULL,
    device_name     VARCHAR(255),
    device_type     VARCHAR(30),
    last_seen_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_device UNIQUE (user_id, device_id)
);

CREATE TABLE refresh_token (
    id              BIGSERIAL PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES user_device (id) ON DELETE CASCADE,
    token_hash      VARCHAR(64) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,
    replaced_by_id  BIGINT REFERENCES refresh_token (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_device_id ON refresh_token (device_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);
