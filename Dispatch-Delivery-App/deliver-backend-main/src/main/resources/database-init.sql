-- =========================================
-- Deliver schema bootstrap (PostgreSQL)
-- Idempotent / re-runnable
-- =========================================

-- NOTE: Trigger-related cleanup/creation has been removed per request ("先不用触发器").
--       This file contains only tables, constraints, seed data, and safe indexes.

-- ---------- USERS ----------
CREATE TABLE IF NOT EXISTS users (
                                     id            BIGSERIAL PRIMARY KEY,
                                     email         TEXT        NOT NULL,
                                     phone         TEXT,
                                     password      TEXT        NOT NULL,
                                     first_name    TEXT,
                                     last_name     TEXT,
                                     role          TEXT        NOT NULL DEFAULT 'USER',
                                     status        TEXT        NOT NULL DEFAULT 'ACTIVE',
                                     created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     token_version BIGINT      NOT NULL DEFAULT 1
);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS token_version BIGINT      NOT NULL DEFAULT 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_ci ON users (lower(email));
CREATE INDEX IF NOT EXISTS ix_users_status ON users(status);

-- ---------- ROLES ----------
CREATE TABLE IF NOT EXISTS roles (
                                     id   BIGSERIAL PRIMARY KEY,
                                     code VARCHAR(50)  NOT NULL UNIQUE,
                                     name VARCHAR(100) NOT NULL
);

INSERT INTO roles(code, name) VALUES
                                  ('CUSTOMER',   'Customer'),
                                  ('DISPATCHER', 'Dispatcher'),
                                  ('ADMIN',      'Administrator')
ON CONFLICT (code) DO NOTHING;

-- ---------- HUBS ----------
CREATE TABLE IF NOT EXISTS hubs (
                                    id      BIGSERIAL PRIMARY KEY,
                                    name    VARCHAR(100) NOT NULL UNIQUE,
                                    address TEXT,
                                    lat     DOUBLE PRECISION,
                                    lng     DOUBLE PRECISION
);

-- ---------- USER_ROLES ----------
CREATE TABLE IF NOT EXISTS user_roles (
                                          id       BIGSERIAL PRIMARY KEY,
                                          user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role_id  BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                          hub_id   BIGINT NULL  REFERENCES hubs(id)  ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_roles_user_role_hub
    ON user_roles (user_id, role_id, COALESCE(hub_id, 0));
CREATE INDEX IF NOT EXISTS ix_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_role ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS ix_user_roles_hub  ON user_roles(hub_id);

-- ---------- PERMISSIONS ----------
CREATE TABLE IF NOT EXISTS permissions (
                                           id   BIGSERIAL PRIMARY KEY,
                                           code VARCHAR(100)  NOT NULL UNIQUE,
                                           name VARCHAR(200)  NOT NULL
);

-- ---------- ROLE_PERMISSIONS ----------
CREATE TABLE IF NOT EXISTS role_permissions (
                                                id             BIGSERIAL PRIMARY KEY,
                                                role_id        BIGINT NOT NULL REFERENCES roles(id)       ON DELETE CASCADE,
                                                permission_id  BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_role_permissions_role_perm
    ON role_permissions(role_id, permission_id);
CREATE INDEX IF NOT EXISTS ix_role_permissions_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS ix_role_permissions_perm ON role_permissions(permission_id);

-- ---------- REFRESH SESSIONS ----------
CREATE TABLE IF NOT EXISTS refresh_sessions (
                                                id            BIGSERIAL PRIMARY KEY,
                                                user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                sid           VARCHAR(64)  NOT NULL UNIQUE,
                                                refresh_hash  VARCHAR(128) NOT NULL,
                                                revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
                                                expires_at    TIMESTAMPTZ  NOT NULL,
                                                created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                                last_used_at  TIMESTAMPTZ,
                                                device_info   VARCHAR(255)
);

-- Replace the previous invalid partial index (with now()) by composite index:
CREATE INDEX IF NOT EXISTS ix_refresh_sessions_user_rev_expires
    ON refresh_sessions(user_id, revoked, expires_at);
CREATE INDEX IF NOT EXISTS ix_refresh_sessions_user
    ON refresh_sessions(user_id);

-- ---------- Seed permissions & role bindings ----------
INSERT INTO permissions(code, name) VALUES
                                        ('INVITE_CREATE',     '创建邀请/邀请注册'),
                                        ('SESSION_REVOKE_ALL','全部会话踢出')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code='ADMIN' AND p.code='INVITE_CREATE'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code='ADMIN' AND p.code='SESSION_REVOKE_ALL'
ON CONFLICT DO NOTHING;

-- ---------- Seed admin user ----------
INSERT INTO users(email, phone, password, first_name, last_name)
VALUES (
           'admin@deliver.local',
           NULL,
           '$2a$10$z4JzkOoOQL3vr/CFOC4.Cujtf7yG16YcYnHo8.WsOJga4qlx7n2pS', -- bcrypt: admin
           'Admin',
           'User'
       )
ON CONFLICT (lower(email)) DO NOTHING;

INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.code='ADMIN'
WHERE lower(u.email)='admin@deliver.local'
ON CONFLICT DO NOTHING;
