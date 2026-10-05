CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    private_notes VARCHAR(4096),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE posts (
    id UUID PRIMARY KEY,
    content VARCHAR(2000) NOT NULL,
    media_url VARCHAR(2048),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SCHEDULED', 'PUBLISHED')),
    scheduled_at TIMESTAMPTZ,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT scheduled_post_has_date CHECK (status <> 'SCHEDULED' OR scheduled_at IS NOT NULL)
);

CREATE INDEX idx_posts_author_created ON posts(author_id, created_at DESC);
CREATE INDEX idx_posts_status_created ON posts(status, created_at DESC);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
