ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(60) NULL;

CREATE TABLE user_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_user_identities PRIMARY KEY (id),
    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uk_user_identities_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_user_identities_user ON user_identities(user_id);
