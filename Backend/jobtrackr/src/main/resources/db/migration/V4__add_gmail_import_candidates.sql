CREATE TABLE gmail_import_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message_id_hash VARCHAR(64) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    confidence VARCHAR(10) NOT NULL,
    company VARCHAR(120) NOT NULL,
    job_title VARCHAR(160) NOT NULL,
    location VARCHAR(160),
    job_url VARCHAR(1000),
    application_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    employment_type VARCHAR(20) NOT NULL,
    source_subject VARCHAR(500) NOT NULL,
    source_sender VARCHAR(500) NOT NULL,
    received_at TIMESTAMP(6) NOT NULL,
    candidate_state VARCHAR(20) NOT NULL,
    imported_application_id BIGINT,
    detected_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_gmail_import_candidates PRIMARY KEY (id),
    CONSTRAINT fk_gmail_import_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_gmail_import_application
        FOREIGN KEY (imported_application_id) REFERENCES job_applications(id) ON DELETE SET NULL,
    CONSTRAINT uk_gmail_import_user_message UNIQUE (user_id, message_id_hash)
);

CREATE INDEX idx_gmail_import_user_state_received
    ON gmail_import_candidates(user_id, candidate_state, received_at);
