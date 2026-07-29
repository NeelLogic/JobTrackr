CREATE TABLE oauth_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    state_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_oauth_states PRIMARY KEY (id),
    CONSTRAINT fk_oauth_states_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_oauth_states_provider_hash UNIQUE (provider, state_hash)
);

CREATE INDEX idx_oauth_states_expiry ON oauth_states(expires_at);

CREATE TABLE gmail_connections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    google_email VARCHAR(254) NOT NULL,
    encrypted_access_token VARCHAR(4096) NOT NULL,
    encrypted_refresh_token VARCHAR(4096) NOT NULL,
    access_token_expires_at TIMESTAMP(6) NOT NULL,
    granted_scopes VARCHAR(1000) NOT NULL,
    connected_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    last_sync_at TIMESTAMP(6),
    CONSTRAINT pk_gmail_connections PRIMARY KEY (id),
    CONSTRAINT fk_gmail_connections_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_gmail_connections_user UNIQUE (user_id),
    CONSTRAINT uk_gmail_connections_email UNIQUE (google_email)
);
