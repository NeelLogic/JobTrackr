CREATE TABLE application_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_application_status_history PRIMARY KEY (id),
    CONSTRAINT fk_status_history_application
        FOREIGN KEY (application_id) REFERENCES job_applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_status_history_user_changed
    ON application_status_history(user_id, changed_at);
CREATE INDEX idx_status_history_application_changed
    ON application_status_history(application_id, changed_at);

INSERT INTO application_status_history (
    application_id,
    user_id,
    from_status,
    to_status,
    changed_at
)
SELECT
    id,
    user_id,
    NULL,
    status,
    created_at
FROM job_applications;
