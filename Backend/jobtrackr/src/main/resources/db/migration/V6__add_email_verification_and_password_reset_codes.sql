ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE auth_one_time_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_auth_one_time_codes PRIMARY KEY (id),
    CONSTRAINT fk_auth_one_time_codes_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_auth_one_time_codes_attempts CHECK (failed_attempts >= 0)
);

CREATE INDEX idx_auth_codes_user_purpose_created
    ON auth_one_time_codes(user_id, purpose, created_at);
CREATE INDEX idx_auth_codes_expiry
    ON auth_one_time_codes(expires_at);
